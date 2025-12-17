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
def get_plan_group_prompt(plan: str, existingGroups: str = "[]", replaceIndex: int = -1):
    """
    Generate or update study groups (planGroups) for the user's learning plan.
    Preserve all existing group details if present.
    """
    plan = json.loads(plan)
    existing_groups = json.loads(existingGroups)

    prompt = f"""
    You are an expert English learning planner.

    Your task:
    - If no groups exist yet (existingGroups is empty), create a full list of study groups for the user's learning plan.
    - If existingGroups is not empty, **keep all groups unchanged except the one at replaceIndex**, which must be regenerated.
    - If any group in existingGroups already includes a "details" field, **keep those details exactly as they are**.
    - Always return the complete, final list of groups (not only the new one).

    --- User Information ---
    {json.dumps(plan['user_info'], ensure_ascii=False, indent=2)}

    --- Plan Information ---
    Title: {plan['title']}
    Description: {plan['description']}
    Study Period: {plan['startDate']} → {plan['endDate']}
    Preferred Study Time: {plan.get('study_time', 'not specified')}

    --- Existing Groups (Preserve "details" if present) ---
    {json.dumps(existing_groups, ensure_ascii=False, indent=2)}

    --- Replace Group Index ---
    {replaceIndex}

    --- Instructions ---
    - Each group should have:
    • "name": short descriptive title (≤ 60 chars)
    • "description": 2–3 sentences explaining what to learn
    • "startDate" and "endDate" within the plan’s time range
    • "details": optional list of topics already generated → **keep them unchanged**
    • study time aligned with user preference:
        "morning" → 06:00–11:59
        "afternoon" → 12:00–17:59
        "evening" → 18:00–22:00
    - When regenerating:
    • Only replace the group at replaceIndex.
    • Preserve all other groups (including their details) exactly as shown in existingGroups.
    • Maintain consistent chronological order between startDate and endDate.

    Return strictly in valid JSON array format:
    [
    {{
        "name": "string",
        "description": "string",
        "startDate": "YYYY-MM-DDTHH:mm:ss",
        "endDate": "YYYY-MM-DDTHH:mm:ss",
        "details": [{{ "topicType": "string", "topicId": "string" }}]  ← optional if exists
    }},
    ...
    ]
    """
    return prompt


@mcp.prompt()
def get_plan_detail_prompt(group, plan, topics, existTopic) -> str:
    plan = json.loads(plan)
    group = json.loads(group)
    topics = json.loads(topics)
    existTopic = json.loads(existTopic)

    prompt = f"""
    You are an English learning expert.

    Your task:
    Evaluate whether each topic below is suitable for the user's learning plan.

    User Plan:
    User Info: {json.dumps(plan['user_info'], ensure_ascii=False)}
    Title: {plan['title']}
    Description: {plan['description']}
    Time Range: {plan['startDate']} → {plan['endDate']}

    Current Plan Group:
    Name: {group['name']}
    Description: {group['description']}

    Topics to consider:
    {json.dumps(topics, ensure_ascii=False, indent=2)}

    ❗ Already used in other groups (should NOT be reused):
    {json.dumps(existTopic, ensure_ascii=False, indent=2)}

    Instructions:
    - Review each topic and decide if it fits this plan group.
    - Do NOT approve any topic whose ID appears in the excluded list above.
    - Each topic should appear in only one group.
    - Return only the following JSON array format:
      [
        {{"topicId": "string", "approved": true/false, "reason": "string"}}
      ]

    Return ONLY the JSON array. No explanations outside JSON.
    """
    return prompt

@mcp.prompt()
def get_vocab_topic_prompt(description:str) ->str:
    return f'''You are an AI assistant that generates English vocabulary topics for learners.

    INPUT:
    - topic_description: "{description}"

    TASK:
    Based on the given topic_description, think of a suitable vocabulary topic and generate vocabulary data.

    REQUIREMENTS:
    1. This is a VOCABULARY topic for learning English.
    2. You must decide an appropriate topic name and description based on the input.
    3. Choose a suitable level from: BEGINNER, INTERMEDIATE, ADVANCED.
    4. Generate a list of vocabulary words related to the topic.
    5. Each vocabulary item MUST follow EXACTLY this header order:

    [
        "word",
        "phonetic (IPA)",
        "meaning",
        "example",
        "exampleMeaning",
        "imageName",
        "audioName"
    ]

    6. imageName must be "{{word}}.jpg"
    7. audioName must be "{{word}}.mp3"
    8. Use simple, clear English meanings and examples suitable for the selected level.
    9. Do NOT add any extra text, explanation, or markdown.
    10. The response MUST be a valid JSON-like object in the exact format below.

    RESPONSE FORMAT (STRICT):
    {{
    name: "<topic name>",
    description: "<short description of this vocabulary topic>",
    level: "BEGINNER | INTERMEDIATE | ADVANCED",
    vocab: [
        [
        "word",
        "/ipa/",
        "meaning",
        "example sentence.",
        "example meaning explanation.",
        "word.jpg",
        "word.mp3"
        ]
    ]
    }}

    IMPORTANT:
    - Return ONLY the object.
    - Do NOT wrap the response in markdown.
    - Do NOT explain anything.
'''
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