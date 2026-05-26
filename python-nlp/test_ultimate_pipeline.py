import torch
import math
import numpy as np
from nltk.tokenize import sent_tokenize, word_tokenize
from collections import Counter
from model_utils import load_model

AI_TEXT = """
Artificial intelligence is rapidly transforming the global economy and reshaping the future of work. By automating routine tasks, machine learning algorithms allow employees to focus on more complex, creative endeavors. This transition, while promising, also presents significant challenges, particularly regarding job displacement and the need for widespread workforce retraining. Governments and educational institutions must collaborate to create robust programs that prepare individuals for the digital age, ensuring that the benefits of technological progress are shared equitably across society. As AI systems become more integrated into daily life, addressing ethical concerns such as algorithmic bias and data privacy will be crucial to building trust and fostering sustainable development.
"""

HUMAN_TEXT = """
I remember walking down the old cobblestone street near my grandmother's house in the middle of autumn. The wind was crisp, carrying the sharp, sweet smell of decaying maple leaves and woodsmoke from a neighbor's chimney. Every few steps, my boots would crunch against the fallen leaves, a satisfying sound that echoed in the quiet afternoon. I didn't really have a destination in mind; I just wanted to clear my head after a long week of exams and endless screen time. Looking back, those quiet, aimless walks were exactly what kept me sane. The world felt smaller then, or maybe it was just that I was more present in it, noticing the gradient of orange on a single leaf or the way the yellow streetlights began to flicker to life as the dusk settled in.
"""

AI_SHORT = """
AI is very useful for writing essays and code. It can help you save time and improve your productivity. However, it can also make mistakes, so you must always verify the output. Many people use AI tools every day for various tasks.
"""

HUMAN_SHORT = """
I went to the store today to buy some milk and bread. On the way back, I met an old friend from high school. We stood on the sidewalk talking for about twenty minutes, catching up on each other's lives. It was nice to see him.
"""

def analyze_text_ultimate(text: str) -> dict:
    model, tokenizer = load_model()
    
    # 1. Basic Tokenization & Counts
    encodings = tokenizer(text, return_tensors="pt", truncation=True, max_length=500)
    input_ids = encodings.input_ids[0]
    
    words = word_tokenize(text.lower())
    words = [w for w in words if w.isalpha()]
    N = len(words)
    
    sentences = sent_tokenize(text)
    num_sentences = len(sentences)
    
    # Check minimum token/word count
    if len(input_ids) < 2 or N < 5:
        return {
            "perplexity": 0.5,
            "burstiness": 0.5,
            "vocabulary": 0.5,
            "repetition": 0.5,
            "final": 0.5,
            "ai": 50.0,
            "human": 50.0
        }
        
    # 2. GPT-2 Predictions: Loss & Log-Rank
    with torch.no_grad():
        outputs = model(input_ids.unsqueeze(0), labels=input_ids.unsqueeze(0))
        loss = outputs.loss.item()
        logits = outputs.logits[0]
        
    ranks = []
    for i in range(len(input_ids) - 1):
        token_id = input_ids[i + 1].item()
        token_logits = logits[i]
        sorted_indices = torch.argsort(token_logits, descending=True)
        rank = (sorted_indices == token_id).nonzero(as_tuple=True)[0].item() + 1
        ranks.append(rank)
        
    avg_log_rank = np.mean([math.log(r) for r in ranks])
    
    # 3. Lexical Complexity (Word Lengths)
    word_lengths = [len(w) for w in words]
    avg_word_len = np.mean(word_lengths)
    
    # 4. Compute Perplexity Score (Weight: 60% in final classification)
    # Calibrated Sigmoid for Loss (predictability)
    loss_midpoint = 3.10
    loss_steepness = 5.0
    score_loss = 1 / (1 + math.exp(-loss_steepness * (loss - loss_midpoint)))
    
    # Calibrated Sigmoid for Log-Rank
    rank_midpoint = 1.65
    rank_steepness = 12.0
    score_rank = 1 / (1 + math.exp(-rank_steepness * (avg_log_rank - rank_midpoint)))
    
    # Combine Loss & Log-Rank for baseline predictability
    perplexity_score = 0.5 * score_loss + 0.5 * score_rank
    
    # Adjust perplexity based on Lexical Complexity
    if avg_word_len < 4.3:
        shift = (4.3 - avg_word_len) / 0.6
        shift = max(0.0, min(1.0, shift))
        perplexity_score = perplexity_score + 0.55 * (1.0 - perplexity_score) * shift
    elif avg_word_len > 4.8:
        shift = (avg_word_len - 4.8) / 1.0
        shift = max(0.0, min(1.0, shift))
        perplexity_score = perplexity_score - 0.45 * perplexity_score * shift
        
    perplexity_score = max(0.01, min(0.99, perplexity_score))
    
    # 5. Compute Burstiness Score
    if num_sentences < 2:
        burstiness_score = 0.5
    else:
        sentence_lengths = [len(word_tokenize(s)) for s in sentences]
        cv = np.std(sentence_lengths) / np.mean(sentence_lengths)
        
        if num_sentences < 5:
            midpoint = 0.18
            steepness = 10.0
        else:
            midpoint = 0.28
            steepness = 8.0
            
        burstiness_score = 1 / (1 + math.exp(-steepness * (cv - midpoint)))
        
    # 6. Compute Vocabulary Richness Score
    unique_words = set(words)
    ttr = len(unique_words) / N
    word_freq = Counter(words)
    hapax = sum(1 for count in word_freq.values() if count == 1)
    hapax_ratio = hapax / N
    combined_vocab = 0.6 * ttr + 0.4 * hapax_ratio
    
    vocab_midpoint = 0.85 - 0.08 * math.log(N / 50.0)
    vocab_midpoint = max(0.45, min(0.90, vocab_midpoint))
    
    vocab_score = 1 / (1 + math.exp(-15.0 * (combined_vocab - vocab_midpoint)))
    
    # Adjust vocabulary score by average word length
    if avg_word_len > 5.0:
        vocab_score = vocab_score * 0.6
    elif avg_word_len < 4.2:
        vocab_score = vocab_score + 0.3 * (1.0 - vocab_score)
        
    vocab_score = max(0.01, min(0.99, vocab_score))
    
    # 7. Compute Repetition Score
    bigrams = [tuple(words[i:i+2]) for i in range(N-1)]
    trigrams = [tuple(words[i:i+3]) for i in range(N-2)]
    
    bigram_counts = Counter(bigrams)
    trigram_counts = Counter(trigrams)
    
    repeated_bigrams = sum(1 for count in bigram_counts.values() if count > 1)
    bigram_repetition = repeated_bigrams / max(len(bigram_counts), 1)
    
    repeated_trigrams = sum(1 for count in trigram_counts.values() if count > 1)
    trigram_repetition = repeated_trigrams / max(len(trigram_counts), 1)
    
    repetition_rate = 0.5 * bigram_repetition + 0.5 * trigram_repetition
    
    rep_midpoint = 0.02 + 0.05 * math.log(N / 50.0)
    rep_midpoint = max(0.01, min(0.20, rep_midpoint))
    
    repetition_score = 1 / (1 + math.exp(25.0 * (repetition_rate - rep_midpoint)))
    
    # 8. Combine Scores with Robust Weights
    final_score = (
        0.60 * perplexity_score +
        0.20 * burstiness_score +
        0.10 * vocab_score +
        0.10 * repetition_score
    )
    
    human_percentage = round(final_score * 100, 1)
    ai_percentage = round((1 - final_score) * 100, 1)
    
    return {
        "perplexity": round(perplexity_score, 4),
        "burstiness": round(burstiness_score, 4),
        "vocabulary_richness": round(vocab_score, 4),
        "repetition": round(repetition_score, 4),
        "final": round(final_score, 4),
        "ai": max(0.0, min(100.0, ai_percentage)),
        "human": max(0.0, min(100.0, human_percentage))
    }

def run_tests():
    print("=== TESTING ULTIMATE PIPELINE ===")
    
    samples = [
        ("AI LONG", AI_TEXT),
        ("HUMAN LONG", HUMAN_TEXT),
        ("AI SHORT", AI_SHORT),
        ("HUMAN SHORT", HUMAN_SHORT)
    ]
    
    for name, text in samples:
        print(f"\n--- {name} ---")
        res = analyze_text_ultimate(text)
        print(f"AI: {res['ai']}%, Human: {res['human']}%")
        print(f"Scores: {res}")

if __name__ == "__main__":
    run_tests()
