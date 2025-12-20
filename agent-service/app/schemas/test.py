from pydantic import BaseModel
from typing import Optional
from enum import Enum

class TestType(str, Enum):
    vocab = "VOCABULARY"
    grammar = "GRAMMAR"
    listening = "LISTENING" 

class TestRequest(BaseModel):
    id: str
    name: str
    description: str
    content: Optional[str] = None
    test_type: TestType