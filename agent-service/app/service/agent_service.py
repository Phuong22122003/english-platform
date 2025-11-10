from langgraph.graph import START, StateGraph, END
from app.schemas.plan import *
from langchain.chat_models import init_chat_model
from .topic_service import topic_service
import json
import os
from app.core import settings
import httpx
from fastmcp import Client
from datetime import datetime, timedelta
import json
if not os.environ.get("GOOGLE_API_KEY"):
  os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY


class MCPClientHolder:
    _client = None

    @classmethod
    async def get_client(cls):
        if cls._client is None:
            cls._client = await Client(r"D:\Documents\DATN\backend\agent-service\app\mcp\plan_mcp.py").__aenter__()
        return cls._client

    @classmethod
    async def close(cls):
        if cls._client:
            await cls._client.__aexit__(None, None, None)
            cls._client = None

class AgentService:
    def __init__(self): 
        self.llm = init_chat_model("gemini-2.5-flash-lite", model_provider="google_genai")
        
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
            print("✅ Generated plan groups:", plan_groups)
            plan["planGroups"] = plan_groups
        except Exception as e:
            print("❌ JSON parse error in plan_group:", e)
            plan_groups = existing_groups  # fallback giữ nhóm cũ

        return plan

    async def plan_detail(self, plan: dict):
        existTopic = set()
        print("🧠 Creating plan details...")
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

                # 🔎 1️⃣ Tìm topics liên quan trong VectorDB
                data = topic_service.search(
                    group.get('name', '') + ' ' +
                    group.get('description', '') + ' ' +
                    plan.get('level', '')
                )

                if not data:
                    print(f"⚠️ No topics found for '{group.get('name', 'Unnamed')}' → regenerating group {idx}...")
                    try:
                        regenerated_plan = await self.plan_group(plan, replace_index=idx)
                    except Exception as e:
                        print(f"❌ MCP error while regenerating group {idx}: {e}")
                        retries += 1
                        continue

                    # cập nhật lại group
                    new_groups = regenerated_plan.get("planGroups", [])
                    if new_groups and idx < len(new_groups):
                        plan["planGroups"] = new_groups
                        group = plan["planGroups"][idx]
                        print(f"✅ Group {idx+1} regenerated → '{group.get('name', 'Unnamed')}'")
                        data = topic_service.search(
                            group.get('name', '') + ' ' +
                            group.get('description', '') + ' ' +
                            plan.get('level', '')
                        )
                    else:
                        print("❌ Regeneration failed or wrong index.")
                        break

                # Không có data sau regenerate
                if not data:
                    retries += 1
                    continue

                # 🔮 2️⃣ Gọi LLM sinh chi tiết nhóm
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
                    print(f"❌ Error invoking LLM or parsing JSON for group {idx+1}: {e}")
                    retries += 1
                    continue

                # 🔍 3️⃣ Phân tích kết quả đánh giá
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
                    print(f"✅ Generated {len(plan_detail)} detail(s) for '{group.get('name', 'Unnamed')}'")
                    success = True
                else:
                    print(f"⚠️ Group '{group.get('name', 'Unnamed')}' has no details → regenerating this group...")
                    retries += 1
                    try:
                        regenerated_plan = await self.plan_group(plan, replace_index=idx)
                        new_groups = regenerated_plan.get("planGroups", [])
                        if new_groups and idx < len(new_groups):
                            plan["planGroups"] = new_groups
                            group = plan["planGroups"][idx]
                            print(f"♻️ Group '{group.get('name', 'Unnamed')}' replaced successfully.")
                        else:
                            print("❌ Failed to regenerate group (no response or wrong index).")
                            break
                    except Exception as e:
                        print(f"❌ MCP regenerate error: {e}")
                        retries += 1
                        continue

            # 🚫 Sau 3 lần retry thất bại → xóa group và kéo các group sau lên
            if not success:
                print(f"🚫 Removing group '{group.get('name', 'Unnamed')}' after {retries} failed attempts.")
                plan["planGroups"].pop(idx)
                plan["planGroups"] = reschedule_after_removal(plan, idx)
                continue  # Không tăng idx vì danh sách đã thu ngắn

            idx += 1  # Tăng chỉ khi group thành công

        # ✅ Chuẩn hóa dữ liệu và gửi callback
        normalized = normalize_datetime_fields(plan)
        print("\n✅ Final plan with details:", json.dumps(normalized, indent=2, ensure_ascii=False))
        await send_callback(normalized, plan.get("userId", ""))
        return plan



async def send_callback(plan, userId):
    async with httpx.AsyncClient(timeout=5) as client:
        try:
            response = await client.post(
                "http://localhost:8083/learning-service/plan/callback",
                json= {**plan, "userId": userId}
            )
            print("✅ Callback response:", response.status_code, response.text)
        except Exception as e:
            print("⚠️ Callback failed:", e)

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
    
