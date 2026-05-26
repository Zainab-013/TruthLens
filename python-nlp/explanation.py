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
            "Predictability: The text is too perfect and predictable, with no spelling mistakes, "
            "typos, or natural writing errors. AI models write by picking the statistically most "
            "likely words, which makes the text flow in a very uniform, robotic way."
        )
    elif perplexity < 0.5:
        explanations.append(
            "Predictability: Some sentences are highly predictable and error-free, while others feel natural. "
            "This suggests a human might have used an AI writing assistant, translation tool, or editor to help."
        )
    else:
        explanations.append(
            "Predictability: The writing uses creative, unexpected phrasing and natural, conversational flow. "
            "Humans naturally write with spontaneous, unpredictable patterns that AI cannot easily replicate."
        )

    # --- Burstiness Explanations ---
    if burstiness < 0.3:
        explanations.append(
            "Sentence Variety: The sentences are all about the same length and structure. Humans naturally "
            "mix short and long sentences to create flow, whereas AI tends to write in a very steady, flat rhythm."
        )
    elif burstiness < 0.5:
        explanations.append(
            "Sentence Variety: The sentences have very similar lengths and lack the dynamic rise and fall of "
            "natural, human storytelling."
        )
    else:
        explanations.append(
            "Sentence Variety: The sentences have great variety, mixing short, punchy lines with longer, "
            "descriptive sentences. This rhythmic contrast is typical of human writing."
        )

    # --- Vocabulary Richness Explanations ---
    if vocabulary < 0.3:
        explanations.append(
            "Word Variety: The writing relies on a small group of simple words, repeating them frequently. "
            "AI models tend to play it safe by using common, generic vocabulary over and over."
        )
    elif vocabulary < 0.5:
        explanations.append(
            "Word Variety: The variety of words used is standard, neither extremely repetitive nor exceptionally rich."
        )
    else:
        explanations.append(
            "Word Variety: The writing uses a rich variety of different words and synonyms. "
            "This expressive vocabulary is a strong indicator of human creativity."
        )

    # --- Repetition Explanations ---
    if repetition < 0.3:
        explanations.append(
            "Repetition: The same phrases or sentence structures are repeated multiple times. "
            "AI models often get stuck in loops and repeat themselves unnecessarily."
        )
    elif repetition < 0.5:
        explanations.append(
            "Repetition: There is some repetition of word patterns, making some parts feel a bit redundant."
        )
    else:
        explanations.append(
            "Repetition: Very few words or phrases are repeated. The writing feels fresh and uses unique "
            "expressions, which points to high-quality human writing."
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
