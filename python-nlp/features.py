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
nltk.download('stopwords', quiet=True)


# ============================================================
# Feature 1: PERPLEXITY (Weight: 60%)
# Calibrated using token-level log-rank and cross-entropy loss.
# Adjusted by word-length complexity to handle simple stories.
# ============================================================
def calculate_perplexity(text: str) -> float:
    """
    Calculate predictability using GPT-2 loss and log-rank, calibrated
    against expected human writing complexity (average word length).
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    import torch

    # Enforce a uniform start and end state for the BPE tokenizer to align with calibrated baselines
    if not text.startswith("\n"):
        text = "\n" + text
    if not text.endswith("\n"):
        text = text + "\n"

    model, tokenizer = load_model()

    # Truncate text to 500 tokens (optimal range for accuracy)
    encodings = tokenizer(text, return_tensors="pt", truncation=True, max_length=500)
    input_ids = encodings.input_ids[0]

    if len(input_ids) < 2:
        return 0.5  # Not enough text to analyze

    # Get model outputs and logits
    with torch.no_grad():
        outputs = model(input_ids.unsqueeze(0), labels=input_ids.unsqueeze(0))
        loss = outputs.loss.item()
        logits = outputs.logits[0]  # Shape: (seq_len, vocab_size)

    # Calculate token ranks (GLTR-style)
    ranks = []
    for i in range(len(input_ids) - 1):
        token_id = input_ids[i + 1].item()
        token_logits = logits[i]
        sorted_indices = torch.argsort(token_logits, descending=True)
        rank = (sorted_indices == token_id).nonzero(as_tuple=True)[0].item() + 1
        ranks.append(rank)

    avg_log_rank = np.mean([math.log(r) for r in ranks])

    # Word list for length calibration
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    avg_word_len = np.mean([len(w) for w in words]) if len(words) > 0 else 4.0

    # Expected human perplexity metrics based on average word length complexity (linear baseline)
    expected_loss = 1.25 * avg_word_len - 2.24
    expected_log_rank = 0.95 * avg_word_len - 2.44

    # Calculate how much lower (more predictable/AI-like) the actual metrics are compared to human baseline
    loss_diff = loss - expected_loss
    rank_diff = avg_log_rank - expected_log_rank

    # Pass the differences through calibrated sigmoids
    score_loss = 1 / (1 + math.exp(-12.0 * (loss_diff + 0.05)))
    score_rank = 1 / (1 + math.exp(-15.0 * (rank_diff + 0.04)))

    # Combine Loss & Log-Rank scores
    perplexity_score = 0.5 * score_loss + 0.5 * score_rank
    perplexity_score = max(0.01, min(0.99, perplexity_score))
    return round(perplexity_score, 4)



# ============================================================
# Feature 2: BURSTINESS (Weight: 20%)
# Calibrated sentence variation based on sentence counts.
# ============================================================
def calculate_burstiness(text: str) -> float:
    """
    Calculate burstiness based on sentence length variation.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    sentences = sent_tokenize(text)
    num_sentences = len(sentences)

    if num_sentences < 2:
        return 0.5  # Not enough sentences

    # Calculate word count per sentence
    sentence_lengths = [len(word_tokenize(s)) for s in sentences]

    mean_length = np.mean(sentence_lengths)
    std_length = np.std(sentence_lengths)

    if mean_length == 0:
        return 0.5

    # Coefficient of variation (CV) as burstiness measure
    cv = std_length / mean_length

    # Adjust expected variation based on sentence count
    if num_sentences < 5:
        midpoint = 0.18
        steepness = 10.0
    else:
        midpoint = 0.28
        steepness = 8.0

    normalized = 1 / (1 + math.exp(-steepness * (cv - midpoint)))
    return round(normalized, 4)


# ============================================================
# Feature 3: VOCABULARY RICHNESS (Weight: 10%)
# Length-robust Type-Token Ratio adjusted by word length.
# ============================================================
def vocabulary_richness(text: str) -> float:
    """
    Calculate vocabulary richness using Type-Token Ratio (TTR) and stop-word density.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    N = len(words)

    if N < 5:
        return 0.5  # Not enough words

    # Calculate stop word density (connectives/conversational structure words vs. AI content dense output)
    from nltk.corpus import stopwords
    stop_words = set(stopwords.words('english'))
    stops_in_text = [w for w in words if w in stop_words]
    stop_fraction = len(stops_in_text) / N

    # Stop words ratio score: human writing typically has >45% stops, AI typically ~30%
    stop_word_score = (stop_fraction - 0.25) / 0.30
    stop_word_score = max(0.01, min(0.99, stop_word_score))

    # Type-Token Ratio
    unique_words = set(words)
    ttr = len(unique_words) / N

    # Length-robust normalization midpoint for TTR (which declines logarithmically as length increases)
    ttr_midpoint = 0.85 - 0.08 * math.log(N / 50.0)
    ttr_midpoint = max(0.45, min(0.90, ttr_midpoint))
    ttr_diff = ttr - ttr_midpoint

    # Sigmoid calibration: higher TTR difference than human baseline = more AI-like = lower score
    ttr_score = 1.0 - (1 / (1 + math.exp(-12.0 * ttr_diff)))
    ttr_score = max(0.01, min(0.99, ttr_score))

    # Combine both scores: stop word density (60%) and TTR score (40%)
    vocab_score = 0.60 * stop_word_score + 0.40 * ttr_score
    vocab_score = max(0.01, min(0.99, vocab_score))
    return round(vocab_score, 4)



# ============================================================
# Feature 4: REPETITION SCORE (Weight: 10%)
# Length-robust n-gram repetition detection.
# ============================================================
def repetition_score(text: str) -> float:
    """
    Calculate repetition score based on n-gram analysis.
    Returns a normalized score between 0 and 1.
    Lower score = more likely AI-generated.
    """
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    N = len(words)

    if N < 10:
        return 0.5  # Not enough words

    # Check bigram and trigram repetition
    bigrams = [tuple(words[i:i+2]) for i in range(N-1)]
    trigrams = [tuple(words[i:i+3]) for i in range(N-2)]

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

    # Expected repetition midpoint increases logarithmically with N
    rep_midpoint = 0.02 + 0.05 * math.log(N / 50.0)
    rep_midpoint = max(0.01, min(0.20, rep_midpoint))

    # Invert: higher repetition = lower score (AI-like)
    normalized = 1 / (1 + math.exp(25.0 * (repetition_rate - rep_midpoint)))
    return round(normalized, 4)
