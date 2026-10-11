from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    JWT:str
    GOOGLE_API_KEY:str
    CONTENT_SERVICE_URL:str
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8"
    )

