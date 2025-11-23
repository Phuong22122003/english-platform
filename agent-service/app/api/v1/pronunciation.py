from fastapi import APIRouter, File, UploadFile, Form
from app.schemas import *
from app.service import pronoun_service
import io
from gtts import gTTS
import base64
import numpy as np
from pydub import AudioSegment
from fastapi.responses import JSONResponse
import eng_to_ipa as ipa

router = APIRouter()

@router.post("/pronunciation/{text}")
def get_pronunciation(text:str):
    print("Getting pronunciation for text:", text)
    text = text.strip()

    # 1️⃣ Lấy phiên âm IPA
    ipa_text = ipa.convert(text)

    # 2️⃣ Tạo file âm thanh trong bộ nhớ
    speech = gTTS(text, lang='en')
    audio_io = io.BytesIO()
    speech.write_to_fp(audio_io)   # <-- không tạo file thật
    audio_io.seek(0)

    # 3️⃣ Chuyển sang base64
    audio_base64 = base64.b64encode(audio_io.read()).decode("utf-8")

    # 4️⃣ Trả kết quả JSON
    return JSONResponse({
        "text": text,
        "ipa": ipa_text,
        "audio_base64": audio_base64
    })
    
    

@router.post("/pronunciation")
async def check_pronunciation(file:UploadFile=File(...), text:str=Form(...)):
    print("Checking pronunciation for text:", text)
    # Đọc toàn bộ bytes từ UploadFile
    audio_bytes = await file.read()
    
    audio = AudioSegment.from_file(io.BytesIO(audio_bytes))
    audio = audio.set_channels(1).set_frame_rate(16000)
    # audio_array = np.array(audio.get_array_of_samples())
    audio_array = np.array(audio.get_array_of_samples()).astype(np.float32)
    audio_array /= 32768.0
    samplerate = audio.frame_rate  # sẽ là 16000 theo set_frame_rate
    # Xuất ra file WAV
    text = text.strip()
    result = pronoun_service.get_ipa_confidence(text_correct=text,audio_array=audio_array,sample_rate = samplerate)

    return result