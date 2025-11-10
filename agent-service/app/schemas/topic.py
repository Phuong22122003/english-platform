from pydantic import BaseModel
from enum import Enum
class LevelEnum(str, Enum):
    BEGINNER = "BEGINNER"
    INTERMEDIATE = "INTERMEDIATE"
    ADVANCED = "ADVANCED"
class TopicSuggestionRequest(BaseModel):
    name: str
    description: str
    type: str  # e.g., "grammar", "vocab", "listening"

class TopicSuggestionResponse(BaseModel):
    ids: list[str]
    type: str  # e.g., "grammar", "vocab", "listening"
    
class TopicCreateRequest(BaseModel):
    id: str
    topic_type: str
    name: str
    description: str
    level: LevelEnum