from pydantic import BaseModel
class TestRequest(BaseModel):
    id: str
    name: str
    description: str
    content: str
    type: str