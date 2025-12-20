from langgraph.graph import START, StateGraph, END
from app.schemas.plan import *
from app.schemas.topic import *
from app.schemas.test import *
from langchain.chat_models import init_chat_model
from .topic_service import topic_service
import json
import os
from app.core import settings
import httpx
from fastmcp import Client
from datetime import datetime, timedelta
import json
from openpyxl import Workbook
from gtts import gTTS
import shutil
from icrawler.builtin import BingImageCrawler
from PIL import Image
import requests
from fastapi.responses import JSONResponse
if not os.environ.get("GOOGLE_API_KEY"):
  os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MCP_PATH = os.path.join(BASE_DIR, "mcp", "plan_mcp.py")
class MCPClientHolder:
    _client = None

    @classmethod
    async def get_client(cls):
        if cls._client is None:
            cls._client = await Client(MCP_PATH).__aenter__()
        return cls._client

    @classmethod
    async def close(cls):
        if cls._client:
            await cls._client.__aexit__(None, None, None)
            cls._client = None

class AgentService:
    def __init__(self): 
        self.llm = init_chat_model("gemini-2.5-flash-lite", model_provider="google_genai")
        self.AUDIO_ROOT = os.path.join(BASE_DIR, "files", "audios")
        self.IMAGE_ROOT = os.path.join(BASE_DIR, "files", "images")
        self.TOPIC_ROOT = os.path.join(BASE_DIR,"files","topic")
        self.EXCEL_FILE = os.path.join(BASE_DIR, "files", "topic.xlsx")
        os.makedirs(self.AUDIO_ROOT, exist_ok=True)
        os.makedirs(self.IMAGE_ROOT, exist_ok=True)
        os.makedirs(self.TOPIC_ROOT, exist_ok=True)
        
        self.VOCAB_HEADERS = ["word","phonetic (IPA)","meaning","example","exampleMeaning","imageName","audioName"]
        # Compile application and test
        graph_builder = StateGraph(Plan)
        graph_builder.add_node(self.plan,'plan')
        graph_builder.add_node(self.plan_group,'plan_group')
        graph_builder.add_node(self.plan_detail,'plan_detail')
        
        graph_builder.add_edge(START, "plan")
        graph_builder.add_edge("plan", "plan_group")
        graph_builder.add_edge("plan_group", "plan_detail")
        graph_builder.add_edge("plan_detail", END)
        
        self.graph = graph_builder.compile()
        
    async def clear_directory(self,path: str):
        if os.path.exists(path):
            for filename in os.listdir(path):
                file_path = os.path.join(path, filename)
                try:
                    if os.path.isfile(file_path) or os.path.islink(file_path):
                        os.unlink(file_path)      # xóa file
                    elif os.path.isdir(file_path):
                        shutil.rmtree(file_path)  # xóa folder con
                except Exception as e:
                    print(f" Không xóa được {file_path}: {e}")
                    
    async def get_image(self, name, description=None, path = None, max_size=(300, 300)):
        if path == None: 
            path = self.IMAGE_ROOT
            
        save_dir = os.path.join(path)
        os.makedirs(save_dir, exist_ok=True)
        
        if description == None:
            description = name
            
        crawler = BingImageCrawler(
            storage={"root_dir": save_dir},
            downloader_threads=1
        )
        crawler.crawl(keyword=description, max_num=1)

        files = os.listdir(save_dir)
        if files:
            old = files[0]
            ext = os.path.splitext(old)[1]
            new_name = f"{name}.jpg"
            old_path = os.path.join(save_dir, old)
            new_path = os.path.join(save_dir, new_name)

            # Resize ảnh
            with Image.open(old_path) as img:
                img.thumbnail(max_size)
                if img.mode != "RGB":
                    img = img.convert("RGB")
                img.save(new_path, format="JPEG")

            # Xóa file gốc nếu cần
            if old_path != new_path:
                os.remove(old_path)

            return new_name
    
    async def create_topic(self, topic_type: TopicType, description: str):   
        await self.clear_directory(self.AUDIO_ROOT)
        await self.clear_directory(self.IMAGE_ROOT)
        await self.clear_directory(self.TOPIC_ROOT)        
        if topic_type == 'VOCABULARY':
            return await self.create_vocabulary_topic(description)
        elif topic_type == 'LISTENING':
            return await self.create_listening_topic(description)
        
    async def create_listening_topic(self,description):
        client = await MCPClientHolder.get_client()
        prompt = await client.get_prompt("get_listening_topic_prompt", arguments={"description": description})
        prompt = prompt.messages[0].content.text
        response = self.llm.invoke(prompt)
        topic = response.content
        if topic.startswith("```"):
            topic = topic.strip("`")
            topic = topic.replace("json", "", 1).strip()
            
        print(F'Topic: ', topic)
        topic = json.loads(topic)
        
        # Topic image
        await self.get_image(name = 'topic', description= topic['description'], path=self.TOPIC_ROOT)
        
        for listening in topic['listening']:
            audio_path = os.path.join(self.AUDIO_ROOT, listening['audioName'])
             # ---- Tạo audio ----
            try:
                tts = gTTS(text=listening['transcript'], lang="en")
                tts.save(audio_path)
            except Exception as e:
                pass
            image_name = listening['imageName']
            if '.' in image_name:
                image_name = image_name.split('.')[-2]
            await self.get_image(name = image_name, description = listening['question'], path=self.IMAGE_ROOT)
        files = []
        files.append(
            ('topic', ('topic.json', json.dumps(topic), "application/json"))
        )
        files.append(
            ('listening', ('listening.json', json.dumps(topic['listening']), "application/json"))
        )
        for filename in os.listdir(self.TOPIC_ROOT):
            path = os.path.join(self.TOPIC_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("topic_image", (filename, open(path, "rb"), "image/jpeg")) )
        for filename in os.listdir(self.IMAGE_ROOT): 
            path = os.path.join(self.IMAGE_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("listening_images", (filename, open(path, "rb"), "image/jpeg")) )
        for filename in os.listdir(self.AUDIO_ROOT):
            path = os.path.join(self.AUDIO_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("listening_audios", (filename, open(path, "rb"), "audio/mpeg")) )
        headers = {
            "Authorization": f"Bearer {settings.JWT}"
        }
        response = requests.post(
            settings.CONTENT_SERVICE_URL + f"/listening/topics/full",
            files=files,
            headers=headers
        )
        for _, f in files:
            content = f[1]
            if isinstance(content, tuple) and hasattr(content[1], "close"):
                content[1].close()
                
        return JSONResponse(
            status_code=response.status_code,
            content=response.json()
        )
        
    async def create_vocabulary_topic(self, description):
        client = await MCPClientHolder.get_client()
        wb = Workbook()
        ws = wb.active
        prompt = await client.get_prompt("get_vocab_topic_prompt", arguments={"description": description})
        prompt = prompt.messages[0].content.text
        response = self.llm.invoke(prompt)
        topic = response.content
        if topic.startswith("```"):
            topic = topic.strip("`")
            topic = topic.replace("json", "", 1).strip()
            
        print(F'Topic: ', topic)
        topic = json.loads(topic)
        
        # Topic image
        await self.get_image(name = 'topic', description= topic['description'], path=self.TOPIC_ROOT)

        ws.title = topic['name']
        ws.append(self.VOCAB_HEADERS)

        # ---- Thư mục audio theo topic ----
        audio_dir = os.path.join(self.AUDIO_ROOT)

        for row in topic['vocab']:
            word = row[0]
            audio_file = row[6]
            audio_path = os.path.join(audio_dir, audio_file)

            # ---- Tạo audio ----
            try:
                tts = gTTS(text=word, lang="en")
                tts.save(audio_path)
                print(f"Audio: {word}")
            except Exception as e:
                print(f"Audio error: {word} – {e}")
                row[6] = "ERROR.mp3"

            # ---- Tải ảnh ----
            await self.get_image(name = word,description=word,path=self.IMAGE_ROOT)

            # ---- Ghi Excel ----
            ws.append(row)
            
        if os.path.exists(self.EXCEL_FILE):
            os.remove(self.EXCEL_FILE)
        wb.save(self.EXCEL_FILE)
        
        files = []
        files.append(
            ('topic', ('topic.json', json.dumps(topic), "application/json"))
        )
        files.append( 
            ("vocabulary_file", 
                ("data.xlsx", 
                    open(self.EXCEL_FILE, "rb"),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            ) 
        )
        for filename in os.listdir(self.TOPIC_ROOT): 
            path = os.path.join(self.TOPIC_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("topic_image", (filename, open(path, "rb"), "image/jpeg")) )
        for filename in os.listdir(self.IMAGE_ROOT): 
            path = os.path.join(self.IMAGE_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("vocab_images", (filename, open(path, "rb"), "image/jpeg")) )
        for filename in os.listdir(self.AUDIO_ROOT): 
            path = os.path.join(self.AUDIO_ROOT, filename) 
            if os.path.isfile(path):
                files.append( ("vocab_audios", (filename, open(path, "rb"), "audio/mpeg")) )
        
        headers = {
            "Authorization": f"Bearer {settings.JWT}"
        }
        
        response = requests.post(
            settings.CONTENT_SERVICE_URL + f"/vocabulary/topics/full",
            files=files,
            headers=headers
        )
        
        for _, f in files:
            content = f[1]
            if isinstance(content, tuple) and hasattr(content[1], "close"):
                content[1].close()
                
        return JSONResponse(
            status_code=response.status_code,
            content=response.json()
        )
        
    async def create_test(self,request: TestRequest):
        await self.clear_directory(self.IMAGE_ROOT)        
        await self.clear_directory(self.AUDIO_ROOT)
        if request.test_type== 'VOCABULARY':
            return await self.create_vocabulary_test(request)
        elif request.test_type == 'LISTENING':
            return await self.create_listening_test(request)
        elif request.test_type == 'GRAMMAR':
            return await self.create_grammar_test(request)
    async def create_grammar_test(self, request: TestRequest):
        client = await MCPClientHolder.get_client()
        prompt = await client.get_prompt("get_grammar_test_creation_prompt", arguments={"description": f'Topic name: {request.name}\n{request.description}\nTopic content: {request.content[:500] if request.content else ""}'})
        prompt = prompt.messages[0].content.text
        response = self.llm.invoke(prompt)
        test = response.content
        if test.startswith("```"):
                test = test.strip("`")       # xóa dấu `
                test = test.replace("json", "", 1).strip()  # xóa chữ 'json' ở đầu nếu có
        print(test)
        test = json.loads(test)
        test_payload = {}
        test_payload['name'] = test['name']
        test_payload['duration'] = test['duration']
        test_payload['questions'] = []
        for q in test['questions']:
            q['explaination'] = q['explanation']
            test_payload["questions"].append(q)
        headers = {
            "Authorization": f"Bearer {settings.JWT}",
            "Content-Type": "application/json"
        }
        response = requests.post(
            settings.CONTENT_SERVICE_URL + f"/grammar/grammars/{request.id}/tests",
            json=test_payload,
            headers=headers,
            
        )
        print(settings.CONTENT_SERVICE_URL + f"/grammar/grammars/{request.id}/tests")
        print(response.text)
        
        return JSONResponse(
            status_code=response.status_code,
            content=response.json()
        )
    async def create_listening_test(self, request: TestRequest):
        client = await MCPClientHolder.get_client()
        prompt = await client.get_prompt("get_listening_test_creation_prompt", arguments={"description": f'Topic name: {request.name}\n {request.description}'})
        prompt = prompt.messages[0].content.text
        response = self.llm.invoke(prompt)
        test = response.content
        if test.startswith("```"):
                test = test.strip("`")       # xóa dấu `
                test = test.replace("json", "", 1).strip()  # xóa chữ 'json' ở đầu nếu có
        print(test)
        test = json.loads(test)
        test_payload = {}
        test_payload['name'] = test['name']
        test_payload['duration'] = test['duration']
        test_payload['questions'] = []
        for q in test['questions']:
            audio_path = os.path.join(self.AUDIO_ROOT,  q['audioName'])
            # ---- Tạo audio ----
            try:
                tts = gTTS(text=q['transcript'], lang="en")
                tts.save(audio_path)
            except Exception as e:
                pass
            
            image_name = q['imageName']
            
            if '.' in image_name:
                image_name = image_name.split('.')[-2]
            await self.get_image(name = image_name, description = q['question'], path=self.IMAGE_ROOT)
            
            q['explaination'] = q['explanation']
            
            test_payload["questions"].append(q)
            
        payload = []
        payload.append(
                ('request', ('test.json', json.dumps(test_payload), "application/json"))
        )
        for filename in os.listdir(self.IMAGE_ROOT): 
            path = os.path.join(self.IMAGE_ROOT, filename) 
            if os.path.isfile(path):
                payload.append( ("images", (filename, open(path, "rb"), "image/jpeg")) )
        for filename in os.listdir(self.AUDIO_ROOT):
            path = os.path.join(self.AUDIO_ROOT, filename) 
            if os.path.isfile(path):
                payload.append( ("audios", (filename, open(path, "rb"), "audio/mpeg")) )
        headers = {
            "Authorization": f"Bearer {settings.JWT}"
        }
        response = requests.post(
            settings.CONTENT_SERVICE_URL + f"/listening/topics/{request.id}/tests",
            files=payload,
            headers=headers
        )
        print(settings.CONTENT_SERVICE_URL + f"/listening/topics/{request.id}/tests")
        print(response.text)
        for _, f in payload:
            content = f[1]
            if isinstance(content, tuple) and hasattr(content[1], "close"):
                content[1].close()
        return JSONResponse(
            status_code=response.status_code,
            content=response.json()
        )
        
    async def create_vocabulary_test(self,request: TestRequest):
        client = await MCPClientHolder.get_client()
        prompt = await client.get_prompt("get_vocab_test_creation_prompt", arguments={"description": f'Topic name: {request.name}\n {request.description}'})
        prompt = prompt.messages[0].content.text
        response = self.llm.invoke(prompt)
        test = response.content
        if test.startswith("```"):
                test = test.strip("`")       # xóa dấu `
                test = test.replace("json", "", 1).strip()  # xóa chữ 'json' ở đầu nếu có
        print(test)
        test = json.loads(test)
        test_payload = {}
        test_payload['name'] = test['name']
        test_payload['duration'] = test['duration']
        test_payload['questions'] = []
        for q in test['questions']:
            await self.get_image(name = q['word'], description= q['word'],path=self.IMAGE_ROOT)
            q['imageName'] = f"{q['word']}.jpg"
            q['explaination'] = q['explanation']
            test_payload["questions"].append(q)
        payload = []
        payload.append(
                ('test', ('test.json', json.dumps(test_payload), "application/json"))
        )
        for filename in os.listdir(self.IMAGE_ROOT): 
            path = os.path.join(self.IMAGE_ROOT, filename) 
            if os.path.isfile(path):
                payload.append( ("images", (filename, open(path, "rb"), "image/jpeg")) )
                
        headers = {
            "Authorization": f"Bearer {settings.JWT}"
        }
        response = requests.post(
            settings.CONTENT_SERVICE_URL + f"/vocabulary/topics/{request.id}/tests",
            files=payload,
            headers=headers
        )
        print(settings.CONTENT_SERVICE_URL + f"/vocabulary/topics/{request.id}/tests")
        print(response.text)
        for _, f in payload:
            content = f[1]
            if isinstance(content, tuple) and hasattr(content[1], "close"):
                content[1].close()
        return JSONResponse(
            status_code=response.status_code,
            content=response.json()
        )
        
    async def invoke(self, input_data:dict):
        print("Invoking agent service with input data:", input_data)
        result = await self.graph.ainvoke({'user_info': input_data, 'userId': input_data.get("user_info", "").get("userId", ""), 'level': input_data.get("user_info", "").get("level", "beginner"), 'study_time': input_data.get("user_info", "").get("studyTime", "morning")})
        return result

    async def plan(self, plan: Plan):
        print("Plan:", plan)
        print("Creating plan...")
        client = await MCPClientHolder.get_client()
        current_time = await client.call_tool("get_current_time", {"timezone": "Asia/Ho_Chi_Minh"})
        current_time_str = current_time.content[0].text
        prompt = await client.get_prompt("get_plan_prompt", arguments={"user_info": plan["user_info"], "current_time": current_time_str})
        prompt = prompt.messages[0].content.text

        response = self.llm.invoke(prompt)
        plan_json = response.content
        if plan_json.startswith("```"):
            plan_json = plan_json.strip("`")       # xóa dấu `
            plan_json = plan_json.replace("json", "", 1).strip()  # xóa chữ 'json' ở đầu nếu có

        plan_response = json.loads(plan_json)
        plan.update(plan_response)
        return plan

    async def plan_group(self, plan: Plan, replace_index: int = -1):
        print("Creating plan groups...")
        client = await MCPClientHolder.get_client()

        existing_groups = plan.get("planGroups", [])
        args = {
            "plan": json.dumps(plan, ensure_ascii=False),
            "existingGroups": json.dumps(existing_groups, ensure_ascii=False),
            "replaceIndex": replace_index
        }

        prompt = await client.get_prompt("get_plan_group_prompt", arguments=args)
        prompt_text = prompt.messages[0].content.text

        response = self.llm.invoke(prompt_text)
        plan_group_json = response.content.strip()

        if plan_group_json.startswith("```"):
            plan_group_json = plan_group_json.strip("`").replace("json", "").strip()

        try:
            plan_groups = json.loads(plan_group_json)
            print(" Generated plan groups:", plan_groups)
            plan["planGroups"] = plan_groups
        except Exception as e:
            print(" JSON parse error in plan_group:", e)
            plan_groups = existing_groups  # fallback giữ nhóm cũ

        return plan

    async def plan_detail(self, plan: dict):
        existTopic = set()
        print("Creating plan details...")
        client = await MCPClientHolder.get_client()

        # ======= HELPER FUNCTIONS =======
        def parse_datetime(dt_str):
            try:
                return datetime.fromisoformat(dt_str)
            except Exception:
                return None

        def reschedule_after_removal(plan, removed_index):
            """
            Khi 1 group bị xóa, tự động kéo các group sau nó lên để nối liền thời gian.
            """
            plan_groups = plan.get("planGroups", [])
            if not plan_groups or removed_index >= len(plan_groups):
                return plan_groups

            # Lấy endDate của group trước (nếu có)
            prev_end = None
            if removed_index > 0:
                prev_end = parse_datetime(plan_groups[removed_index - 1]["endDate"])

            for i in range(removed_index, len(plan_groups)):
                g = plan_groups[i]
                start = parse_datetime(g["startDate"])
                end = parse_datetime(g["endDate"])
                if not start or not end:
                    continue

                duration = (end - start).days
                if duration <= 0:
                    duration = 1

                # Nếu có prev_end thì dời startDate lên ngay sau prev_end
                if prev_end:
                    new_start = prev_end + timedelta(days=1)
                    new_end = new_start + timedelta(days=duration)
                    g["startDate"] = new_start.replace(hour=12, minute=0, second=0).isoformat()
                    g["endDate"] = new_end.replace(hour=12, minute=0, second=0).isoformat()
                    prev_end = new_end
                else:
                    # Nếu xóa group đầu tiên thì bắt đầu từ startDate của plan
                    plan_start = parse_datetime(plan.get("startDate"))
                    if plan_start:
                        new_start = plan_start
                        new_end = new_start + timedelta(days=duration)
                        g["startDate"] = new_start.replace(hour=12, minute=0, second=0).isoformat()
                        g["endDate"] = new_end.replace(hour=12, minute=0, second=0).isoformat()
                        prev_end = new_end
            return plan_groups

        # ======= MAIN LOOP =======
        idx = 0
        while idx < len(plan.get("planGroups", [])):
            group = plan["planGroups"][idx]
            retries = 0
            success = False

            while retries < 3 and not success:
                print(f"\n🔹 [Group {idx+1}] {group.get('name', 'Unnamed')} (Attempt {retries+1}/3)")

                #   Tìm topics liên quan trong VectorDB
                data = topic_service.search(
                    group.get('name', '') + ' ' +
                    group.get('description', '') + ' ' +
                    plan.get('level', '')
                )

                if not data:
                    print(f" No topics found for '{group.get('name', 'Unnamed')}' → regenerating group {idx}...")
                    try:
                        regenerated_plan = await self.plan_group(plan, replace_index=idx)
                    except Exception as e:
                        print(f" MCP error while regenerating group {idx}: {e}")
                        retries += 1
                        continue

                    # cập nhật lại group
                    new_groups = regenerated_plan.get("planGroups", [])
                    if new_groups and idx < len(new_groups):
                        plan["planGroups"] = new_groups
                        group = plan["planGroups"][idx]
                        print(f" Group {idx+1} regenerated → '{group.get('name', 'Unnamed')}'")
                        data = topic_service.search(
                            group.get('name', '') + ' ' +
                            group.get('description', '') + ' ' +
                            plan.get('level', '')
                        )
                    else:
                        print(" Regeneration failed or wrong index.")
                        break

                # Không có data sau regenerate
                if not data:
                    retries += 1
                    continue

                #   Gọi LLM sinh chi tiết nhóm
                try:
                    prompt = await client.get_prompt(
                        "get_plan_detail_prompt",
                        arguments={
                            "group": group,
                            "plan": plan,
                            "topics": data,
                            "existTopic": list(existTopic),
                        },
                    )
                    prompt_text = prompt.messages[0].content.text
                    response = self.llm.invoke(prompt_text)
                    result_json = response.content.strip()
                    if result_json.startswith("```"):
                        result_json = result_json.strip("`").replace("json", "").strip()
                    evaluation = json.loads(result_json)
                except Exception as e:
                    print(f" Error invoking LLM or parsing JSON for group {idx+1}: {e}")
                    retries += 1
                    continue

                #   Phân tích kết quả đánh giá
                plan_detail = []
                if isinstance(evaluation, dict) and "details" in evaluation:
                    evaluation = evaluation["details"]

                for item in evaluation:
                    topic_id = item.get("topicId")
                    approved = item.get("approved", True)
                    if not approved or not topic_id:
                        continue

                    topic = next((t for t in data if t["id"] == topic_id), None)
                    if topic:
                        plan_detail.append({
                            "topicType": topic["topic_type"],
                            "topicId": topic["id"]
                        })
                        existTopic.add(topic_id)

                if plan_detail:
                    group.setdefault("details", []).extend(plan_detail)
                    print(f" Generated {len(plan_detail)} detail(s) for '{group.get('name', 'Unnamed')}'")
                    success = True
                else:
                    print(f" Group '{group.get('name', 'Unnamed')}' has no details → regenerating this group...")
                    retries += 1
                    try:
                        regenerated_plan = await self.plan_group(plan, replace_index=idx)
                        new_groups = regenerated_plan.get("planGroups", [])
                        if new_groups and idx < len(new_groups):
                            plan["planGroups"] = new_groups
                            group = plan["planGroups"][idx]
                            print(f"♻️ Group '{group.get('name', 'Unnamed')}' replaced successfully.")
                        else:
                            print(" Failed to regenerate group (no response or wrong index).")
                            break
                    except Exception as e:
                        print(f" MCP regenerate error: {e}")
                        retries += 1
                        continue

            #  Sau 3 lần retry thất bại → xóa group và kéo các group sau lên
            if not success:
                print(f" Removing group '{group.get('name', 'Unnamed')}' after {retries} failed attempts.")
                plan["planGroups"].pop(idx)
                plan["planGroups"] = reschedule_after_removal(plan, idx)
                continue  # Không tăng idx vì danh sách đã thu ngắn

            idx += 1  # Tăng chỉ khi group thành công

        #  Chuẩn hóa dữ liệu và gửi callback
        normalized = normalize_datetime_fields(plan)
        print("\n Final plan with details:", json.dumps(normalized, indent=2, ensure_ascii=False))
        await send_callback(normalized, plan.get("userId", ""))
        return plan



async def send_callback(plan, userId):
    async with httpx.AsyncClient(timeout=5) as client:
        try:
            response = await client.post(
                settings.PLAN_SERVICE_CALLBACK_URL,
                json= {**plan, "userId": userId}
            )
            print(" Callback response:", response.status_code, response.text)
        except Exception as e:
            print(" Callback failed:", e)

def normalize_datetime_fields(plan):
    def ensure_full_datetime(value):
        if isinstance(value, str) and len(value) == 10:  # dạng '2023-10-27'
            return value + "T00:00:00"
        return value

    plan["startDate"] = ensure_full_datetime(plan.get("startDate"))
    plan["endDate"] = ensure_full_datetime(plan.get("endDate"))

    for group in plan.get("planGroups", []):
        group["startDate"] = ensure_full_datetime(group.get("startDate"))
        group["endDate"] = ensure_full_datetime(group.get("endDate"))

    return plan

# from typing_extensions import List, TypedDict

# class PlanDetail(TypedDict):
#     topicType:str
#     topicId: str

# class PlanGroup(TypedDict):
#     name: str
#     description: str
#     startDate: str
#     endDate: str
#     details: List[PlanDetail] 
    
# class Plan(TypedDict):
#     user_info: str
#     title: str
#     description: str
#     startDate: str
#     endDate:str
#     planGroups: List[PlanGroup]
    
