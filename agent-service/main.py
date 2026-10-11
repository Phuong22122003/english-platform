from fastapi import FastAPI, APIRouter
from app.api.v1 import *
from app.core import settings
app = FastAPI()
agent_service_router = APIRouter(prefix="/agent-service", tags=["agent-service"])

agent_service_router.include_router(agent.router, tags=["agent"])

agent_service_router.include_router(pronunciation.router, tags=["pronunciation"])

app.include_router(agent_service_router)