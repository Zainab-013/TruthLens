import torch
import math
import numpy as np
import nltk
from nltk.tokenize import sent_tokenize, word_tokenize
from collections import Counter
from model_utils import load_model
from analyzer import analyze_text

# Text Samples
AI_TEXT = """
Artificial intelligence is rapidly transforming the global economy and reshaping the future of work. By automating routine tasks, machine learning algorithms allow employees to focus on more complex, creative endeavors. This transition, while promising, also presents significant challenges, particularly regarding job displacement and the need for widespread workforce retraining. Governments and educational institutions must collaborate to create robust programs that prepare individuals for the digital age, ensuring that the benefits of technological progress are shared equitably across society. As AI systems become more integrated into daily life, addressing ethical concerns such as algorithmic bias and data privacy will be crucial to building trust and fostering sustainable development.
"""

HUMAN_TEXT = """
I remember walking down the old cobblestone street near my grandmother's house in the middle of autumn. The wind was crisp, carrying the sharp, sweet smell of decaying maple leaves and woodsmoke from a neighbor's chimney. Every few steps, my boots would crunch against the fallen leaves, a satisfying sound that echoed in the quiet afternoon. I didn't really have a destination in mind; I just wanted to clear my head after a long week of exams and endless screen time. Looking back, those quiet, aimless walks were exactly what kept me sane. The world felt smaller then, or maybe it was just that I was more present in it, noticing the gradient of orange on a single leaf or the way the yellow streetlights began to flicker to life as the dusk settled in.
"""

def test_current():
    print("=== TESTING CURRENT ANALYZER ===")
    print("AI Text Analysis:")
    res_ai = analyze_text(AI_TEXT)
    print(f"  AI %: {res_ai['ai_percentage']}%, Human %: {res_ai['human_percentage']}%")
    print(f"  Verdict: {res_ai['verdict']}")
    print(f"  Scores: {res_ai['scores']}")
    
    print("\nHuman Text Analysis:")
    res_human = analyze_text(HUMAN_TEXT)
    print(f"  AI %: {res_human['ai_percentage']}%, Human %: {res_human['human_percentage']}%")
    print(f"  Verdict: {res_human['verdict']}")
    print(f"  Scores: {res_human['scores']}")

def test_improved_features():
    print("\n=== TESTING IMPROVED FEATURES ===")
    model, tokenizer = load_model()
    
    for name, text in [("AI", AI_TEXT), ("Human", HUMAN_TEXT)]:
        print(f"\n--- Analyzing {name} Text ---")
        encodings = tokenizer(text, return_tensors="pt")
        input_ids = encodings.input_ids[0]
        
        if len(input_ids) < 2:
            print("Text too short")
            continue
            
        # Get logits
        with torch.no_grad():
            outputs = model(input_ids.unsqueeze(0))
            logits = outputs.logits[0]  # Shape: (seq_len, vocab_size)
            
        # For each token (starting from second), get its rank in the previous token's predictions
        ranks = []
        top_10_count = 0
        top_100_count = 0
        
        # We shift logits by 1 since logits[i] predicts input_ids[i+1]
        for i in range(len(input_ids) - 1):
            token_id = input_ids[i + 1].item()
            token_logits = logits[i]
            
            # Sort logits descending
            sorted_indices = torch.argsort(token_logits, descending=True)
            # Find rank of actual token_id (0-indexed rank, so +1 for 1-indexed)
            rank = (sorted_indices == token_id).nonzero(as_tuple=True)[0].item() + 1
            ranks.append(rank)
            
            if rank <= 10:
                top_10_count += 1
            if rank <= 100:
                top_100_count += 1
                
        frac_top_10 = top_10_count / len(ranks)
        frac_top_100 = top_100_count / len(ranks)
        avg_rank = np.mean(ranks)
        median_rank = np.median(ranks)
        # Log rank is often more robust to extreme outlier ranks
        avg_log_rank = np.mean([math.log(r) for r in ranks])
        
        print(f"Tokens count: {len(input_ids)}")
        print(f"Fraction in Top 10: {frac_top_10:.4f}")
        print(f"Fraction in Top 100: {frac_top_100:.4f}")
        print(f"Average Rank: {avg_rank:.2f}")
        print(f"Median Rank: {median_rank:.2f}")
        print(f"Average Log Rank: {avg_log_rank:.4f}")

if __name__ == "__main__":
    test_current()
    test_improved_features()
