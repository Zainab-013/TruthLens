"""
TruthLens - Python NLP API (ENTRY POINT)
FastAPI-based REST API that exposes the NLP analysis engine.

Endpoints:
    POST /analyze  →  Analyze text and return AI vs Human scores
    GET  /health   →  Health check

Run:
    uvicorn main:app --host 0.0.0.0 --port 5000 --reload
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from analyzer import analyze_text

# ============================================================
# App Setup
# ============================================================
app = FastAPI(
    title="TruthLens NLP Engine",
    description="AI vs Human Text Detection API",
    version="1.0.0"
)

# Allow cross-origin requests from Android app & Spring Boot
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================
# Request/Response Models
# ============================================================
class AnalyzeRequest(BaseModel):
    text: str


class ScoresResponse(BaseModel):
    perplexity: float
    burstiness: float
    vocabulary_richness: float
    repetition: float
    final_score: float


class AnalyzeResponse(BaseModel):
    ai_percentage: float
    human_percentage: float
    verdict: str
    scores: ScoresResponse
    explanation: list[str]


# ============================================================
# Endpoints
# ============================================================
@app.get("/health")
def health_check():
    """Health check endpoint to verify the API is running."""
    return {
        "status": "ok",
        "service": "TruthLens NLP Engine",
        "version": "1.0.0"
    }


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    """
    Analyze text for AI vs Human detection.

    Request Body:
        { "text": "The text to analyze..." }

    Response:
        {
            "ai_percentage": 72.5,
            "human_percentage": 27.5,
            "verdict": "This text is very likely AI-generated.",
            "scores": { ... },
            "explanation": [ ... ]
        }
    """
    text = request.text.strip()

    # Count words
    word_count = len(text.split())

    # Validate: minimum 50 words for meaningful analysis
    if word_count < 50:
        raise HTTPException(
            status_code=400,
            detail=f"Text is too short ({word_count} words). "
                   f"Please provide at least 50 words for accurate analysis. "
                   f"Recommended: 300-500 words."
        )

    # Limit input to 500 words for optimal accuracy (as per scoring model)
    if word_count > 500:
        words = text.split()[:500]
        text = " ".join(words)
        word_count = 500

    try:
        # Run analysis
        result = analyze_text(text)
        return result

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Analysis failed: {str(e)}"
        )


# ============================================================
# Run the server
# ============================================================
if __name__ == "__main__":
    import uvicorn
    print("=" * 50)
    print("[NLP] TruthLens NLP Engine Starting...")
    print("[API] Running at: http://localhost:5000")
    print("[DOCS] Swagger UI: http://localhost:5000/docs")
    print("=" * 50)
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)
