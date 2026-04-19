# Run
- uvicorn main:app --host 0.0.0.0 --port 5000 --workers 2
- uvicorn main:app --reload --host 0.0.0.0 --port 5000
# Create env: python -m venv .venv
- activate: .venv\Scripts\activate
- deactivate
- pip install requirements.txt
- pip freeze > requirements_stable.txt 