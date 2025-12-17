from fastapi import APIRouter, Depends
from app.schemas.topic import *
from app.schemas.test import *
from app.service import agent_service
from fastapi.responses import JSONResponse
router = APIRouter(prefix='/agent')

@router.post("/plan")
async def create_plan(user_info: dict):
    print("Creating plan...", user_info)
    await agent_service.invoke({"user_info": user_info})
    return JSONResponse({"message": "Plan created successfully"})
@router.post("/topics")
async def create_topic(topic_type, description:str):
    return await agent_service.create_topic(topic_type,description)
@router.post("/tests")
async def create_test(test: TestRequest):
    return await agent_service.create_test(test)