from typing_extensions import List, TypedDict

class PlanDetail(TypedDict):
    topicType:str
    topicId: str

class PlanGroup(TypedDict):
    name: str
    description: str
    startDate: str
    endDate: str
    details: List[PlanDetail] 
    
class Plan(TypedDict):
    userId: str
    user_info: str
    title: str
    description: str
    startDate: str
    endDate:str
    planGroups: List[PlanGroup]
    level: str
    
