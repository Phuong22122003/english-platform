from pydantic import BaseModel
from enum import Enum
class LevelEnum(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"
    
class TopicType(str, Enum):
    vocab = "VOCABULARY"
    grammar = "GRAMMAR"
    listening = "LISTENING" 
       
class TopicCreateRequest(BaseModel):
    id: str
    topic_type: str
    name: str
    description: str
    level: LevelEnum