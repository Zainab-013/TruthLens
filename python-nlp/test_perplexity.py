import torch
import math
import numpy as np
from model_utils import load_model

AI_TEXT = """
Artificial intelligence is rapidly transforming the global economy and reshaping the future of work. By automating routine tasks, machine learning algorithms allow employees to focus on more complex, creative endeavors. This transition, while promising, also presents significant challenges, particularly regarding job displacement and the need for widespread workforce retraining. Governments and educational institutions must collaborate to create robust programs that prepare individuals for the digital age, ensuring that the benefits of technological progress are shared equitably across society. As AI systems become more integrated into daily life, addressing ethical concerns such as algorithmic bias and data privacy will be crucial to building trust and fostering sustainable development.
"""

HUMAN_TEXT = """
I remember walking down the old cobblestone street near my grandmother's house in the middle of autumn. The wind was crisp, carrying the sharp, sweet smell of decaying maple leaves and woodsmoke from a neighbor's chimney. Every few steps, my boots would crunch against the fallen leaves, a satisfying sound that echoed in the quiet afternoon. I didn't really have a destination in mind; I just wanted to clear my head after a long week of exams and endless screen time. Looking back, those quiet, aimless walks were exactly what kept me sane. The world felt smaller then, or maybe it was just that I was more present in it, noticing the gradient of orange on a single leaf or the way the yellow streetlights began to flicker to life as the dusk settled in.
"""

def test_perplexity():
    model, tokenizer = load_model()
    
    for name, text in [("AI", AI_TEXT), ("Human", HUMAN_TEXT)]:
        print(f"\n--- {name} Text ---")
        encodings = tokenizer(text, return_tensors="pt")
        input_ids = encodings.input_ids
        
        with torch.no_grad():
            outputs = model(input_ids, labels=input_ids)
            loss = outputs.loss.item()
            
        raw_perplexity = math.exp(loss)
        print(f"Loss: {loss:.4f}")
        print(f"Raw Perplexity: {raw_perplexity:.2f}")
        
        # Calculate perplexity per token to see the distribution
        with torch.no_grad():
            outputs = model(input_ids)
            logits = outputs.logits  # (1, seq_len, vocab_size)
            
        # Shift logits and labels
        shift_logits = logits[..., :-1, :].contiguous()
        shift_labels = input_ids[..., 1:].contiguous()
        
        # Cross entropy loss per token
        loss_fct = torch.nn.CrossEntropyLoss(reduction='none')
        loss_per_token = loss_fct(shift_logits.view(-1, shift_logits.size(-1)), shift_labels.view(-1))
        
        # Get token-level perplexity
        token_perplexities = torch.exp(loss_per_token).tolist()
        tokens = tokenizer.convert_ids_to_tokens(input_ids[0][1:])
        
        # Sort tokens by perplexity
        token_ppl_pairs = list(zip(tokens, token_perplexities))
        # Top 10 most unexpected tokens (highest loss)
        sorted_pairs = sorted(token_ppl_pairs, key=lambda x: x[1], reverse=True)
        print("Top 10 highest perplexity (unexpected) tokens:")
        for t, ppl in sorted_pairs[:10]:
            clean_t = t.encode('ascii', errors='replace').decode('ascii')
            print(f"  {clean_t}: {ppl:.2f}")
            
        # Top 10 most predictable tokens (lowest loss)
        print("Top 10 lowest perplexity (predictable) tokens:")
        for t, ppl in sorted_pairs[-10:]:
            clean_t = t.encode('ascii', errors='replace').decode('ascii')
            print(f"  {clean_t}: {ppl:.2f}")

if __name__ == "__main__":
    test_perplexity()
