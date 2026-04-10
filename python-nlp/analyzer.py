"""
TruthLens - Core Analyzer (BRAIN)
Calls all features, combines them using weights, and returns final result.

Flow:
    main.py → analyzer.py → features.py + explanation.py
                                ↓
                          model_utils.py

Final Score Formula:
    0.4 x Perplexity + 0.2 x Burstiness + 0.2 x Vocabulary + 0.2 x Repetition
"""

from features import (
    calculate_perplexity,
    calculate_burstiness,
    vocabulary_richness,
    repetition_score
)
from explanation import generate_explanation, get_verdict


def analyze_text(text: str) -> dict:
    """
    Analyze text and return AI vs Human scores with all features.

    Args:
        text: The input text to analyze.

    Returns:
        dict with AI%, Human%, individual scores, verdict, and explanations.
    """
    # Step 1: Calculate all features
    perplexity = calculate_perplexity(text)
    burstiness = calculate_burstiness(text)
    vocabulary = vocabulary_richness(text)
    repetition = repetition_score(text)

    # Step 2: Weighted final score (0 = AI, 1 = Human)
    final_score = (
        0.4 * perplexity +
        0.2 * burstiness +
        0.2 * vocabulary +
        0.2 * repetition
    )

    # Step 3: Convert to percentages
    human_percentage = round(final_score * 100, 1)
    ai_percentage = round((1 - final_score) * 100, 1)

    # Clamp values between 0 and 100
    human_percentage = max(0, min(100, human_percentage))
    ai_percentage = max(0, min(100, ai_percentage))

    # Step 4: Build scores dict
    scores = {
        "perplexity": perplexity,
        "burstiness": burstiness,
        "vocabulary_richness": vocabulary,
        "repetition": repetition,
        "final_score": round(final_score, 4)
    }

    # Step 5: Generate explanations and verdict
    explanations = generate_explanation(scores)
    verdict = get_verdict(ai_percentage)

    # Step 6: Return complete result
    return {
        "ai_percentage": ai_percentage,
        "human_percentage": human_percentage,
        "verdict": verdict,
        "scores": scores,
        "explanation": explanations
    }
