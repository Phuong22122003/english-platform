from fastapi import APIRouter
from app.schemas.topic import *
from app.schemas.test import *
from app.service import agent_service
router = APIRouter(prefix='/agent')

@router.post("/topics")
async def create_topic(topic_type: TopicType, description:str):
    return await agent_service.create_topic(topic_type,description)

@router.post("/tests")
async def create_test(test: TestRequest):
    return await agent_service.create_test(test)