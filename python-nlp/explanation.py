"""
TruthLens - Explanation Engine
Generates human-readable explanations for why text is classified as AI or Human.
This makes the project stand out by providing transparency.
"""


def generate_explanation(scores: dict) -> list:
    """
    Generate explanations based on individual feature scores.

    Args:
        scores: dict with perplexity, burstiness,
                vocabulary_richness, and repetition scores.

    Returns:
        List of explanation strings.
    """
    explanations = []

    perplexity = scores["perplexity"]
    burstiness = scores["burstiness"]
    vocabulary = scores["vocabulary_richness"]
    repetition = scores["repetition"]

    # --- Perplexity Explanations ---
    if perplexity < 0.3:
        explanations.append(
            "Low perplexity detected - the text is highly predictable, "
            "which is a strong indicator of AI-generated content."
        )
    elif perplexity < 0.5:
        explanations.append(
            "Moderate perplexity - the text shows some predictable patterns "
            "that may suggest AI involvement."
        )
    else:
        explanations.append(
            "High perplexity - the text has natural unpredictability, "
            "consistent with human writing."
        )

    # --- Burstiness Explanations ---
    if burstiness < 0.3:
        explanations.append(
            "Low burstiness - sentence lengths are very uniform, "
            "a common trait in AI-generated text."
        )
    elif burstiness < 0.5:
        explanations.append(
            "Moderate burstiness - some variation in sentence structure, "
            "but less than typical human writing."
        )
    else:
        explanations.append(
            "High burstiness - sentence lengths vary naturally, "
            "which is typical of human writing."
        )

    # --- Vocabulary Richness Explanations ---
    if vocabulary < 0.3:
        explanations.append(
            "Low vocabulary diversity - the text reuses similar words, "
            "suggesting AI generation."
        )
    elif vocabulary < 0.5:
        explanations.append(
            "Moderate vocabulary - word diversity is average, "
            "not strongly indicating either AI or human."
        )
    else:
        explanations.append(
            "Rich vocabulary - the text uses diverse and varied words, "
            "consistent with human authorship."
        )

    # --- Repetition Explanations ---
    if repetition < 0.3:
        explanations.append(
            "High repetition detected - repeated phrases and patterns "
            "suggest AI-generated content."
        )
    elif repetition < 0.5:
        explanations.append(
            "Some repetition found - moderate phrase repetition detected."
        )
    else:
        explanations.append(
            "Low repetition - the text has natural variation in phrasing, "
            "typical of human writing."
        )

    return explanations


def get_verdict(ai_percentage: float) -> str:
    """
    Generate a final verdict based on the AI percentage.

    Args:
        ai_percentage: The calculated AI percentage.

    Returns:
        A verdict string.
    """
    if ai_percentage >= 80:
        return "This text is very likely AI-generated."
    elif ai_percentage >= 60:
        return "This text is probably AI-generated with some human editing."
    elif ai_percentage >= 40:
        return "This text shows a mix of AI and human characteristics."
    elif ai_percentage >= 20:
        return "This text is probably human-written with minor AI assistance."
    else:
        return "This text is very likely human-written."
