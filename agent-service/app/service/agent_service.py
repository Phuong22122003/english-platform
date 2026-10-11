from app.schemas.topic import *
from app.schemas.test import *
from langchain.chat_models import init_chat_model
import json
import os
from app.core import settings
from fastmcp import Client
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
        print(settings.CONTENT_SERVICE_URL + f"/vocabulary/topics/full")
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
            q['explanation'] = q['explanation']
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

            q['explanation'] = q['explanation']

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
            q['explanation'] = q['explanation']
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
