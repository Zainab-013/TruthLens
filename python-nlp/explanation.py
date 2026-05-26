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
            "Perplexity (Text Predictability): The text shows exceptionally low perplexity, "
            "indicating highly predictable patterns. Large language models inherently generate text "
            "by selecting statistically probable next words, leading to a uniform flow. "
            "This predictability is a strong mathematical indicator of AI-generated content."
        )
    elif perplexity < 0.5:
        explanations.append(
            "Perplexity (Text Predictability): The text exhibits moderate perplexity. "
            "While some phrasing is dynamic, other segments align closely with common statistical "
            "patterns, suggesting partial AI involvement, co-authoring, or heavy machine editing."
        )
    else:
        explanations.append(
            "Perplexity (Text Predictability): The text exhibits high perplexity, meaning the vocabulary "
            "choices and transitions are highly creative and unpredictable. This linguistic variety "
            "is a hallmark of authentic human thought and spontaneous writing."
        )

    # --- Burstiness Explanations ---
    if burstiness < 0.3:
        explanations.append(
            "Burstiness (Sentence Length Variety): The sentences have very similar lengths and rhythmic "
            "structures. While human authors naturally vary sentence structure to create tempo and voice, "
            "AI models tend to output text with extremely consistent, uniform sentence lengths, creating a flat cadence."
        )
    elif burstiness < 0.5:
        explanations.append(
            "Burstiness (Sentence Length Variety): The sentence lengths show limited variation. "
            "The rhythm is somewhat mechanical and lacks the natural, dynamic transitions between short "
            "and long sentences common in human prose."
        )
    else:
        explanations.append(
            "Burstiness (Sentence Length Variety): The text features a highly dynamic rhythm with mixed "
            "sentence lengths. The interplay of brief and detailed statements indicates a natural, "
            "expressive voice characteristic of human authorship."
        )

    # --- Vocabulary Richness Explanations ---
    if vocabulary < 0.3:
        explanations.append(
            "Vocabulary Diversity (TTR): The lexical richness is low, relying heavily on a small set "
            "of repetitive words. AI generators frequently default to high-probability vocabulary, "
            "whereas human writers tend to employ a broader, more expressive lexicon."
        )
    elif vocabulary < 0.5:
        explanations.append(
            "Vocabulary Diversity (TTR): The word selection is moderately diverse, using standard "
            "terminology. It does not strongly lean toward either AI or human writing patterns on a lexical level."
        )
    else:
        explanations.append(
            "Vocabulary Diversity (TTR): The text demonstrates outstanding lexical diversity, using a wide "
            "range of unique words and precise synonyms. This sophisticated use of vocabulary points "
            "strongly to organic, creative human writing."
        )

    # --- Repetition Explanations ---
    if repetition < 0.3:
        explanations.append(
            "Repetition & Redundancy: High frequency of repeated words or nested phrases. "
            "AI language models often suffer from structural loops or semantic redundancy, which leads "
            "to artificial patterns of repetition."
        )
    elif repetition < 0.5:
        explanations.append(
            "Repetition & Redundancy: Some repetitive phrases or recurring structures are present, "
            "indicating moderate phrasing constraints or stylistic redundancy."
        )
    else:
        explanations.append(
            "Repetition & Redundancy: Extremely low repetition. The text flows naturally with unique "
            "syntax and varied phrasing, characteristic of high-quality human writing."
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
