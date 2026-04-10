"""
TruthLens - Feature Extraction
All NLP feature calculations are kept here for clean, modular code.

Features:
    1. Perplexity      → Measures predictability (GPT-2 based)
    2. Burstiness      → Measures sentence length variation
    3. Vocabulary       → Measures word diversity (TTR)
    4. Repetition      → Detects repeated n-grams
"""

import math
import numpy as np
import nltk
from nltk.tokenize import sent_tokenize, word_tokenize
from collections import Counter
from model_utils import load_model

# Download required NLTK data
nltk.download('punkt', quiet=True)
nltk.download('punkt_tab', quiet=True)


# ============================================================
# Feature 1: PERPLEXITY (Weight: 40%)
# Low perplexity = predictable = likely AI-generated
# High perplexity = unpredictable = likely human-written
# ============================================================
def calculate_perplexity(text: str) -> float:
    """
    Calculate perplexity using GPT-2.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    import torch

    model, tokenizer = load_model()

    # Truncate text to 500 tokens (optimal range for accuracy)
    encodings = tokenizer(text, return_tensors="pt", truncation=True, max_length=500)
    input_ids = encodings.input_ids

    if input_ids.shape[1] < 2:
        return 0.5  # Not enough text to analyze

    with torch.no_grad():
        outputs = model(input_ids, labels=input_ids)
        loss = outputs.loss.item()

    # Raw perplexity
    raw_perplexity = math.exp(loss)

    # Improved normalization using sigmoid curve
    # AI text: perplexity typically 15-50
    # Human text: perplexity typically 60-300+
    # Midpoint at ~55 gives best separation
    midpoint = 55
    steepness = 0.04
    normalized = 1 / (1 + math.exp(-steepness * (raw_perplexity - midpoint)))

    return round(normalized, 4)


# ============================================================
# Feature 2: BURSTINESS (Weight: 20%)
# Low burstiness = uniform sentences = likely AI
# High burstiness = varied sentences = likely human
# ============================================================
def calculate_burstiness(text: str) -> float:
    """
    Calculate burstiness based on sentence length variation.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    sentences = sent_tokenize(text)

    if len(sentences) < 2:
        return 0.5  # Not enough sentences

    # Calculate word count per sentence
    sentence_lengths = [len(word_tokenize(s)) for s in sentences]

    mean_length = np.mean(sentence_lengths)
    std_length = np.std(sentence_lengths)

    if mean_length == 0:
        return 0.5

    # Coefficient of variation (CV) as burstiness measure
    cv = std_length / mean_length

    # Normalize: AI text usually has CV 0.1-0.3, human text 0.4-1.0+
    normalized = min(max((cv - 0.1) / 0.8, 0), 1)

    return round(normalized, 4)


# ============================================================
# Feature 3: VOCABULARY RICHNESS (Weight: 20%)
# Low richness = repetitive vocabulary = likely AI
# High richness = diverse vocabulary = likely human
# ============================================================
def vocabulary_richness(text: str) -> float:
    """
    Calculate vocabulary richness using Type-Token Ratio (TTR).
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    words = word_tokenize(text.lower())

    # Filter out punctuation
    words = [w for w in words if w.isalpha()]

    if len(words) < 5:
        return 0.5  # Not enough words

    # Type-Token Ratio
    unique_words = set(words)
    ttr = len(unique_words) / len(words)

    # Also calculate Hapax Legomena ratio (words appearing only once)
    word_freq = Counter(words)
    hapax = sum(1 for count in word_freq.values() if count == 1)
    hapax_ratio = hapax / len(words)

    # Combined score (weighted average of TTR and Hapax ratio)
    combined = 0.6 * ttr + 0.4 * hapax_ratio

    # Normalize: AI text usually has TTR 0.3-0.5, human text 0.5-0.8
    normalized = min(max((combined - 0.2) / 0.6, 0), 1)

    return round(normalized, 4)


# ============================================================
# Feature 4: REPETITION SCORE (Weight: 20%)
# High repetition = likely AI
# Low repetition = likely human
# ============================================================
def repetition_score(text: str) -> float:
    """
    Calculate repetition score based on n-gram analysis.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated (more repetition found).
    """
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]

    if len(words) < 10:
        return 0.5  # Not enough words

    # Check bigram and trigram repetition
    bigrams = [tuple(words[i:i+2]) for i in range(len(words)-1)]
    trigrams = [tuple(words[i:i+3]) for i in range(len(words)-2)]

    # Count repeated n-grams
    bigram_counts = Counter(bigrams)
    trigram_counts = Counter(trigrams)

    # Ratio of repeated bigrams
    repeated_bigrams = sum(1 for count in bigram_counts.values() if count > 1)
    bigram_repetition = repeated_bigrams / max(len(bigram_counts), 1)

    # Ratio of repeated trigrams
    repeated_trigrams = sum(1 for count in trigram_counts.values() if count > 1)
    trigram_repetition = repeated_trigrams / max(len(trigram_counts), 1)

    # Combined repetition score
    repetition_rate = 0.5 * bigram_repetition + 0.5 * trigram_repetition

    # Invert: high repetition = low score (AI-like)
    normalized = 1 - min(repetition_rate * 3, 1)  # Scale up for sensitivity

    return round(normalized, 4)
