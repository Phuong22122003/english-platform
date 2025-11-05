from fastmcp import FastMCP
import json
from datetime import datetime
import pytz      
mcp = FastMCP(
    name='Plan server'
)

@mcp.tool()
def get_user_info(user_id:str):
    '''Get user information by user ID'''
    return  'I am a beginner in English. I want to improve my English skills for grammar and vocabulary.'
@mcp.prompt()
def get_plan_prompt(user_info, current_time) -> str:
    ''''''
    prompt = f"""
    You are an expert English learning planner.

    Today's date is {current_time}.

    Your task:
    Based on the user information below, create a general learning plan for the user.  
    The plan should include:
    - A clear and concise title describing the plan.
    - A brief description (2–4 sentences) of what the plan covers and its goal.
    - A start date and an end date in ISO format (YYYY-MM-DD).

    User information:
    {user_info}

    Return ONLY a valid JSON object (no markdown, no explanation, no code block).
    Use this exact format:
    {{
    "title": "string",
    "description": "string",
    "startDate": "YYYY-MM-DD",
    "endDate": "YYYY-MM-DD"
    }}
    """
    return prompt
@mcp.prompt()
def get_plan_group_prompt(plan):
    ''''''
    plan = json.loads(plan)
    prompt=f'''You are an expert English learning planner. Based on the user information and plan information, create a plan group for the user.\n
    User Information: {plan['user_info']}\n
    Plan Information: Title: {plan['title']}, description: {plan['description']}, startDate: {plan['startDate']}, endDate: {plan['endDate']}\n
    Your response should be in JSON format as below
    [{{
        "name": "string",
        "description": "string",
        "startDate": "string",
        "endDate": "string",
    }},
    {{
        "name": "string",
        "description": "string",
        "startDate": "string",
        "endDate": "string",
    }},
    ...
    ]
    '''
    return prompt
@mcp.prompt()
def get_plan_detail_prompt(group, plan, topics) -> str:
    plan = json.loads(plan)
    group = json.loads(group)
    topics = json.loads(topics)
    prompt = f"""
    You are an English learning expert.

    Your task:
    Evaluate whether each topic below is suitable for the user's learning plan.

    User Plan:
    User Info: {plan['user_info']}
    Title: {plan['title']}
    Description: {plan['description']}
    Time Range: {plan['startDate']} → {plan['endDate']}

    Current Plan Group:
    Name: {group['name']}
    Description: {group['description']}

    Topics:
    {json.dumps(topics, ensure_ascii=False, indent=2)}

    Current Plan Group:
    Name: {group['name']}
    Description: {group['description']}

    Topics:
    {json.dumps(topics, ensure_ascii=False, indent=2)}

    For each topic, decide if it fits this plan group.
    Return a JSON array in this format:
    [
      {{"topicId": "string", "approved": true/false, "reason": "string"}}
    ]

    Only each topic exist in one group. Not duplicate topic in multiple groups.
    Return ONLY the JSON array. No explanation.
    """
    return prompt

@mcp.tool()
def get_current_time(timezone: str = "Asia/Ho_Chi_Minh") -> str:
    """Return the current system time in ISO format for a given timezone."""
    try:
        tz = pytz.timezone(timezone)
    except Exception:
        tz = pytz.utc
    now = datetime.now(tz)
    return now.isoformat()

if __name__=='__main__':
    mcp.run()