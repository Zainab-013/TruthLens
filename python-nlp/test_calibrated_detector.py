import torch
import math
import numpy as np
import nltk
from nltk.tokenize import sent_tokenize, word_tokenize
from collections import Counter
from model_utils import load_model

AI_TEXT = """
Artificial intelligence is rapidly transforming the global economy and reshaping the future of work. By automating routine tasks, machine learning algorithms allow employees to focus on more complex, creative endeavors. This transition, while promising, also presents significant challenges, particularly regarding job displacement and the need for widespread workforce retraining. Governments and educational institutions must collaborate to create robust programs that prepare individuals for the digital age, ensuring that the benefits of technological progress are shared equitably across society. As AI systems become more integrated into daily life, addressing ethical concerns such as algorithmic bias and data privacy will be crucial to building trust and fostering sustainable development.
"""

HUMAN_TEXT = """
I remember walking down the old cobblestone street near my grandmother's house in the middle of autumn. The wind was crisp, carrying the sharp, sweet smell of decaying maple leaves and woodsmoke from a neighbor's chimney. Every few steps, my boots would crunch against the fallen leaves, a satisfying sound that echoed in the quiet afternoon. I didn't really have a destination in mind; I just wanted to clear my head after a long week of exams and endless screen time. Looking back, those quiet, aimless walks were exactly what kept me sane. The world felt smaller then, or maybe it was just that I was more present in it, noticing the gradient of orange on a single leaf or the way the yellow streetlights began to flicker to life as the dusk settled in.
"""

# Let's write another set of shorter samples to test robustness to short length
AI_SHORT = """
AI is very useful for writing essays and code. It can help you save time and improve your productivity. However, it can also make mistakes, so you must always verify the output. Many people use AI tools every day for various tasks.
"""

HUMAN_SHORT = """
I went to the store today to buy some milk and bread. On the way back, I met an old friend from high school. We stood on the sidewalk talking for about twenty minutes, catching up on each other's lives. It was nice to see him.
"""

def calculate_improved_perplexity(text: str) -> float:
    """
    Calculate perplexity and token log-rank metrics using GPT-2.
    Combines both for a highly accurate predictability score.
    """
    model, tokenizer = load_model()
    
    encodings = tokenizer(text, return_tensors="pt", truncation=True, max_length=500)
    input_ids = encodings.input_ids[0]
    
    if len(input_ids) < 2:
        return 0.5
        
    with torch.no_grad():
        outputs = model(input_ids.unsqueeze(0), labels=input_ids.unsqueeze(0))
        loss = outputs.loss.item()
        logits = outputs.logits[0]  # Shape: (seq_len, vocab_size)
        
    # Calculate token ranks
    ranks = []
    for i in range(len(input_ids) - 1):
        token_id = input_ids[i + 1].item()
        token_logits = logits[i]
        sorted_indices = torch.argsort(token_logits, descending=True)
        rank = (sorted_indices == token_id).nonzero(as_tuple=True)[0].item() + 1
        ranks.append(rank)
        
    avg_log_rank = np.mean([math.log(r) for r in ranks])
    
    # Sigmoid normalization for raw loss (perplexity in log-space)
    # AI loss: typically 2.0 - 3.1
    # Human loss: typically 3.1 - 4.2
    loss_midpoint = 3.10
    loss_steepness = 5.0
    normalized_loss = 1 / (1 + math.exp(-loss_steepness * (loss - loss_midpoint)))
    
    # Sigmoid normalization for avg log rank (GLTR-style)
    # AI log rank: typically 1.3 - 1.62
    # Human log rank: typically 1.68 - 2.2
    rank_midpoint = 1.65
    rank_steepness = 12.0
    normalized_rank = 1 / (1 + math.exp(-rank_steepness * (avg_log_rank - rank_midpoint)))
    
    # Combine both scores (50% loss, 50% log rank)
    combined = 0.5 * normalized_loss + 0.5 * normalized_rank
    
    return round(combined, 4)

def calculate_improved_burstiness(text: str) -> float:
    sentences = sent_tokenize(text)
    
    if len(sentences) < 2:
        return 0.5
        
    sentence_lengths = [len(word_tokenize(s)) for s in sentences]
    mean_length = np.mean(sentence_lengths)
    std_length = np.std(sentence_lengths)
    
    if mean_length == 0:
        return 0.5
        
    cv = std_length / mean_length
    
    # Adjust expected variation based on number of sentences
    # Short text has naturally lower sentence variation
    num_sentences = len(sentences)
    if num_sentences < 5:
        midpoint = 0.18
        steepness = 10.0
    else:
        midpoint = 0.30
        steepness = 8.0
        
    normalized = 1 / (1 + math.exp(-steepness * (cv - midpoint)))
    return round(normalized, 4)

def calculate_improved_vocabulary(text: str) -> float:
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    
    N = len(words)
    if N < 5:
        return 0.5
        
    unique_words = set(words)
    ttr = len(unique_words) / N
    
    word_freq = Counter(words)
    hapax = sum(1 for count in word_freq.values() if count == 1)
    hapax_ratio = hapax / N
    
    combined = 0.6 * ttr + 0.4 * hapax_ratio
    
    # Length-robust normalization midpoint
    # TTR declines logarithmically as length increases
    midpoint = 0.85 - 0.08 * math.log(N / 50.0)
    # Clamp midpoint to reasonable ranges
    midpoint = max(0.45, min(0.90, midpoint))
    
    normalized = 1 / (1 + math.exp(-15.0 * (combined - midpoint)))
    return round(normalized, 4)

def calculate_improved_repetition(text: str) -> float:
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    
    N = len(words)
    if N < 10:
        return 0.5
        
    bigrams = [tuple(words[i:i+2]) for i in range(N-1)]
    trigrams = [tuple(words[i:i+3]) for i in range(N-2)]
    
    bigram_counts = Counter(bigrams)
    trigram_counts = Counter(trigrams)
    
    repeated_bigrams = sum(1 for count in bigram_counts.values() if count > 1)
    bigram_repetition = repeated_bigrams / max(len(bigram_counts), 1)
    
    repeated_trigrams = sum(1 for count in trigram_counts.values() if count > 1)
    trigram_repetition = repeated_trigrams / max(len(trigram_counts), 1)
    
    repetition_rate = 0.5 * bigram_repetition + 0.5 * trigram_repetition
    
    # Expected repetition midpoint increases logarithmically with N
    midpoint = 0.02 + 0.05 * math.log(N / 50.0)
    midpoint = max(0.01, min(0.20, midpoint))
    
    # Invert: higher repetition = lower score (AI-like)
    normalized = 1 / (1 + math.exp(25.0 * (repetition_rate - midpoint)))
    return round(normalized, 4)

def test_pipeline(text: str) -> dict:
    perplexity = calculate_improved_perplexity(text)
    burstiness = calculate_improved_burstiness(text)
    vocabulary = calculate_improved_vocabulary(text)
    repetition = calculate_improved_repetition(text)
    
    # Weighted score (0 = AI, 1 = Human)
    final_score = (
        0.4 * perplexity +
        0.2 * burstiness +
        0.2 * vocabulary +
        0.2 * repetition
    )
    
    human_percentage = round(final_score * 100, 1)
    ai_percentage = round((1 - final_score) * 100, 1)
    
    return {
        "ai": max(0.0, min(100.0, ai_percentage)),
        "human": max(0.0, min(100.0, human_percentage)),
        "scores": {
            "perplexity": perplexity,
            "burstiness": burstiness,
            "vocabulary": vocabulary,
            "repetition": repetition,
            "final": round(final_score, 4)
        }
    }

def run_tests():
    print("=== TESTING CALIBRATED DETECTOR ===")
    
    samples = [
        ("AI LONG", AI_TEXT),
        ("HUMAN LONG", HUMAN_TEXT),
        ("AI SHORT", AI_SHORT),
        ("HUMAN SHORT", HUMAN_SHORT)
    ]
    
    for name, text in samples:
        print(f"\n--- {name} ---")
        res = test_pipeline(text)
        print(f"AI: {res['ai']}%, Human: {res['human']}%")
        print(f"Scores: {res['scores']}")

if __name__ == "__main__":
    run_tests()
