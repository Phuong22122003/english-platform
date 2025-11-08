from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    JWT:str
    GOOGLE_API_KEY:str
    PLAN_SERVICE_CALLBACK_URL:str
    VECTOR_DB_HOST:str
    model_config = SettingsConfigDict(
        env_file=".env",          # đọc file .env
        env_file_encoding="utf-8" # tránh lỗi ký tự đặc biệt
    )

