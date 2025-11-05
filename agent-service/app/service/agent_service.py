from langgraph.graph import START, StateGraph, END
from app.schemas.plan import *
from langchain.chat_models import init_chat_model
from .topic_service import topic_service
import json
import os
from app.core import settings
import httpx
import asyncio
from fastmcp import Client
import threading, requests
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
        result = await self.graph.ainvoke({'user_info': input_data, 'userId': input_data.get("user_info", "").get("userId", "")})
        return result

    async def plan(self, plan: Plan):
        print("userId:", plan.get("userId", "ko co"))
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

    async def plan_group(self, plan: Plan):
        print("Creating plan groups...")
        client = await MCPClientHolder.get_client()
        prompt = await client.get_prompt("get_plan_group_prompt", arguments={"plan": plan})
        prompt = prompt.messages[0].content.text
        
        response = self.llm.invoke(prompt)
        plan_group_json = response.content
        if plan_group_json.startswith("```"):
            plan_group_json = plan_group_json.strip("`")       # xóa dấu `
            plan_group_json = plan_group_json.replace("json", "", 1).strip()  # xóa chữ 'json' ở đầu nếu có
        plan_groups = json.loads(plan_group_json)
        plan['planGroups'] = plan_groups
        return plan 
        
    async def plan_detail(self, plan:Plan):
        existTopic = set()
        print("Creating plan details...")
        print("userId:", plan.get("userId", "ko co"))
        client = await MCPClientHolder.get_client()
        for group in plan["planGroups"]: 
            data = topic_service.search(group['name'] + group['description'])
            if (data is None) or (len(data) == 0):
                continue
            prompt = await client.get_prompt("get_plan_detail_prompt", arguments={"group": group, "plan": plan, "topics": data})
            prompt = prompt.messages[0].content.text

            response = self.llm.invoke(prompt)
            result_json = response.content.strip()
            if result_json.startswith("```"):
                result_json = result_json.strip("`").replace("json", "").strip()
            evaluation = json.loads(result_json)

            plan_detail = []
            for item in evaluation:
                if item.get("approved"):
                    topic_id = item["topicId"]
                    topic = next((t for t in data if t["id"] == topic_id), None)
                    if topic:
                        if topic_id in existTopic:
                            continue
                        plan_detail.append({"topicType": topic["topic_type"], "topicId": topic["id"]})
                        existTopic.add(topic_id)
            if plan_detail:
                group.setdefault("details", []).extend(plan_detail)
        print("Final plan:", normalize_datetime_fields(plan))
        await send_callback(normalize_datetime_fields(plan), plan.get("userId", ""))
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
    
