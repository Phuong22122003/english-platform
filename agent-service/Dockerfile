FROM python:3.11-slim


WORKDIR /code

RUN apt-get update && apt-get install -y ffmpeg

COPY ./requirements.txt /code/requirements.txt
RUN pip install --no-cache-dir \
    torch==2.3.0+cpu \
    torchaudio==2.3.0+cpu \
    -f https://download.pytorch.org/whl/torch_stable.html
    
RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt

COPY . .

EXPOSE 5000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000","--workers", "2"]
