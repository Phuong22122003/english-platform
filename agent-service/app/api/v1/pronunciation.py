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

    # IPA
    ipa_text = ipa.convert(text)

    # Create audio 
    speech = gTTS(text, lang='en')
    audio_io = io.BytesIO()
    speech.write_to_fp(audio_io) 
    audio_io.seek(0)

    # Convert to base64
    audio_base64 = base64.b64encode(audio_io.read()).decode("utf-8")

    return JSONResponse({
        "text": text,
        "ipa": ipa_text,
        "audio_base64": audio_base64
    })
    
    

@router.post("/pronunciation")
async def check_pronunciation(file:UploadFile=File(...), text:str=Form(...)):
    
    print("Checking pronunciation for text:", text)
    audio_bytes = await file.read()
    audio = AudioSegment.from_file(io.BytesIO(audio_bytes))
    audio = audio.set_channels(1)
    audio_array = np.array(audio.get_array_of_samples()).astype(np.float32)
    audio_array /= audio.max_possible_amplitude
    samplerate = audio.frame_rate
    
    result = pronoun_service.get_ipa_confidence(text_input=text,audio_array=audio_array,sample_rate = samplerate)
    
    return result