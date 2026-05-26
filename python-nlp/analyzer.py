"""
TruthLens - Core Analyzer (BRAIN)
Calls all features, combines them using weights, and returns final result.

Flow:
    main.py → analyzer.py → features.py + explanation.py
                                ↓
                          model_utils.py
"""

import re
from features import (
    calculate_perplexity,
    calculate_burstiness,
    vocabulary_richness,
    repetition_score
)
from explanation import generate_explanation, get_verdict


def clean_text(text: str) -> str:
    """
    Cleans OCR-extracted text by removing noise characters, typical layout lines,
    and normalizing whitespace.
    """
    if not text:
        return ""
    # Remove vertical bars, bullets, brackets, tildes, stars, backslashes, braces
    text = re.sub(r'[\|•\[\]_~*\\/{}#]', '', text)
    # Collapse multiple whitespaces and linebreaks into a single space
    text = re.sub(r'\s+', ' ', text)
    return text.strip()


def analyze_text(text: str) -> dict:
    """
    Analyze text and return AI vs Human scores with all features.

    Args:
        text: The input text to analyze.

    Returns:
        dict with AI%, Human%, individual scores, verdict, and explanations.
    """
    # Step 0: Clean the text to remove OCR noise and layout artifacts
    cleaned_text = clean_text(text)

    # If cleaning resulted in an empty string, fallback to original to prevent crashes
    if not cleaned_text:
        cleaned_text = text

    # Step 1: Calculate all features using cleaned text
    perplexity = calculate_perplexity(cleaned_text)
    burstiness = calculate_burstiness(cleaned_text)
    vocabulary = vocabulary_richness(cleaned_text)
    repetition = repetition_score(cleaned_text)

    # Step 2: Dynamic weighting ensemble based on text length and perplexity confidence
    # Count words to adjust weights for short inputs
    words_list = [w for w in re.findall(r'\b\w+\b', cleaned_text)]
    num_words = len(words_list)

    if num_words < 80:
        # For short texts, stylistic features (burstiness, vocabulary richness) are
        # statistically noisy. We rely almost entirely (90%) on perplexity.
        final_score = (
            0.90 * perplexity +
            0.05 * vocabulary +
            0.05 * burstiness
        )
    else:
        # For longer texts, we can use stylistic features more safely.
        # If perplexity indicates high confidence (either clearly AI or clearly Human),
        # we heavily weigh perplexity to avoid stylistic diluting.
        if perplexity < 0.25 or perplexity > 0.75:
            final_score = (
                0.85 * perplexity +
                0.08 * vocabulary +
                0.07 * burstiness
            )
        else:
            # Standard balanced weights for borderline cases
            final_score = (
                0.55 * perplexity +
                0.25 * vocabulary +
                0.20 * burstiness
            )

    # Step 3: Apply calibration scaling to push clear decisions closer to 0% and 100%
    if final_score < 0.35:
        scaled_score = final_score * 0.4
    elif final_score > 0.65:
        scaled_score = final_score + (1.0 - final_score) * 0.6
    else:
        # Linear interpolation in the middle zone
        scaled_score = 0.14 + (final_score - 0.35) * (0.79 - 0.14) / (0.65 - 0.35)

    # Convert to percentages
    human_percentage = round(scaled_score * 100, 1)
    ai_percentage = round((1 - scaled_score) * 100, 1)

    # Clamp values between 0 and 100
    human_percentage = max(0.0, min(100.0, human_percentage))
    ai_percentage = max(0.0, min(100.0, ai_percentage))

    # Step 4: Build scores dict
    scores = {
        "perplexity": perplexity,
        "burstiness": burstiness,
        "vocabulary_richness": vocabulary,
        "repetition": repetition,
        "final_score": round(scaled_score, 4)
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

