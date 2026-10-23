"""
Truvira AI - detector service (Python).

In simple words: Spring Boot sends us an image, we ask a pretrained AI model
"how likely is it that this image was made by AI?", and we send back a number from 0 to 1.

IMPORTANT: the number is a GUESS by a model, not proof. Detectors make mistakes, especially on
images from new generators or images that were compressed (e.g. sent through WhatsApp).
Truvira always shows it as a probability for a HUMAN reviewer to weigh.

Run it (from the ai-service folder):
    .venv\\Scripts\\python -m uvicorn app:app --port 8000
"""

import io
import os
import time

from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image, UnidentifiedImageError
from transformers import pipeline

# Which model to use. os.getenv reads an ENVIRONMENT VARIABLE (a setting outside the code),
# so you can try another model without editing this file. The second value is the default.
MODEL_ID = os.getenv("TRUVIRA_MODEL", "haywoodsloan/ai-image-detector-deploy")

# Different models name their labels differently ("artificial", "ai", "fake"...).
# Any label in this set counts as "AI-generated".
AI_LABELS = {"artificial", "ai", "fake", "ai-generated", "generated"}

# FastAPI() creates the web app (like @SpringBootApplication does for Java).
app = FastAPI(title="Truvira AI detector")

# pipeline() downloads the model from Hugging Face the FIRST time (then reuses the cached copy)
# and wraps it in a simple function: give it an image, get back labels with scores.
# device=-1 means "use the CPU" (no graphics card needed).
# This line runs ONCE when the service starts, not on every request (loading takes seconds).
print(f"Loading model {MODEL_ID} ...")
classifier = pipeline("image-classification", model=MODEL_ID, device=-1)
print("Model loaded.")


# @app.get("/health") = like @GetMapping("/health") in Spring. Used to check "are you alive?"
@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_ID}


# @app.post("/detect") = like @PostMapping. UploadFile = like MultipartFile in Spring.
# File(...) means the form field "file" is REQUIRED.
# "async def" = this function can wait for the upload without blocking other requests.
@app.post("/detect")
async def detect(file: UploadFile = File(...)):
    data = await file.read()  # the raw bytes of the image

    # Open the bytes as an image. convert("RGB") makes PNG transparency / grayscale
    # into the 3 colour channels the model expects.
    try:
        image = Image.open(io.BytesIO(data)).convert("RGB")
    except UnidentifiedImageError:
        # HTTPException = return an error response (like throwing IllegalArgumentException -> 400)
        raise HTTPException(status_code=400, detail="Not a readable image")

    start = time.perf_counter()
    # top_k=None = give us EVERY label with its score, e.g.
    #   [{"label": "artificial", "score": 0.93}, {"label": "real", "score": 0.07}]
    results = classifier(image, top_k=None)
    elapsed_ms = round((time.perf_counter() - start) * 1000)

    # Add up the scores of all "AI" labels -> one number: probability the image is AI-generated.
    ai_score = sum(r["score"] for r in results if r["label"].lower() in AI_LABELS)

    return {
        "aiScore": round(float(ai_score), 4),  # 0.0 = looks real ... 1.0 = looks AI-generated
        "model": MODEL_ID,                     # which model gave this answer (for the report)
        "labels": results,                     # the raw answer, for transparency
        "elapsedMs": elapsed_ms,
    }
