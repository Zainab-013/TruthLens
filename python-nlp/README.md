# TruthLens - Python NLP Engine

AI vs Human text detection engine using NLP techniques.

## Features
- **Perplexity** (GPT-2 based) — measures text predictability
- **Burstiness** — measures sentence length variation
- **Vocabulary Richness** — measures word diversity (TTR)
- **Repetition Score** — detects repeated n-grams

## How to Run

### 1. Activate virtual environment
```bash
# Windows
..\venv\Scripts\Activate.ps1
```

### 2. Install dependencies
```bash
pip install -r requirements.txt
```

### 3. Start the API server
```bash
python main.py
```

### 4. API runs at
```
http://localhost:5000
```

### 5. Swagger Docs (auto-generated)
```
http://localhost:5000/docs
```

## API Endpoints

### `GET /health`
Health check — returns API status.

### `POST /analyze`
Analyze text for AI vs Human detection.

**Request:**
```json
{
    "text": "Your text to analyze here..."
}
```

**Response:**
```json
{
    "ai_percentage": 72.5,
    "human_percentage": 27.5,
    "verdict": "This text is very likely AI-generated.",
    "scores": {
        "perplexity": 0.05,
        "burstiness": 0.15,
        "vocabulary_richness": 0.45,
        "repetition": 0.30,
        "final_score": 0.275
    },
    "explanation": [
        "Low perplexity detected...",
        "Low burstiness..."
    ]
}
```

## File Structure
```
python-nlp/
├── main.py          ← Entry point (FastAPI server)
├── analyzer.py      ← Core logic (combines all features)
├── features.py      ← Feature calculations (perplexity, burstiness, etc.)
├── explanation.py   ← Explanation generator
├── model_utils.py   ← GPT-2 model loader (singleton)
├── requirements.txt ← Dependencies
└── README.md        ← This file
```

## Score Formula
```
Final Score = 0.4 × Perplexity + 0.2 × Burstiness + 0.2 × Vocabulary + 0.2 × Repetition
```
