from fastapi import FastAPI, APIRouter
from app.api.v1 import *
from app.core import settings
print("VECTOR DB HOST:",settings.VECTOR_DB_HOST)
app = FastAPI()
agent_service_router = APIRouter(prefix="/agent-service", tags=["agent-service"])

agent_service_router.include_router(agent.router, tags=["agent"])
agent_service_router.include_router(pronunciation.router, tags=["pronunciation"])
agent_service_router.include_router(topic.router, tags=['topic'])

app.include_router(agent_service_router)