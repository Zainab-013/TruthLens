import torch
import math
import numpy as np
from nltk.tokenize import sent_tokenize, word_tokenize
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

def print_raw_features():
    model, tokenizer = load_model()
    
    samples = [
        ("AI LONG", AI_TEXT),
        ("HUMAN LONG", HUMAN_TEXT),
        ("AI SHORT", AI_SHORT),
        ("HUMAN SHORT", HUMAN_SHORT)
    ]
    
    for name, text in samples:
        print(f"\n=== {name} ===")
        encodings = tokenizer(text, return_tensors="pt")
        input_ids = encodings.input_ids[0]
        
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
        
        words = word_tokenize(text.lower())
        words = [w for w in words if w.isalpha()]
        sentences = sent_tokenize(text)
        
        print(f"Tokens Count: {len(input_ids)}")
        print(f"Words Count: {len(words)}")
        print(f"Sentences Count: {len(sentences)}")
        print(f"Loss: {loss:.4f}")
        print(f"Raw Perplexity: {math.exp(loss):.2f}")
        print(f"Avg Log Rank: {avg_log_rank:.4f}")
        
        if len(sentences) >= 2:
            sentence_lengths = [len(word_tokenize(s)) for s in sentences]
            cv = np.std(sentence_lengths) / np.mean(sentence_lengths)
            print(f"Sentence Lengths: {sentence_lengths}")
            print(f"Burstiness (CV): {cv:.4f}")
        else:
            print("Burstiness: N/A (< 2 sentences)")

if __name__ == "__main__":
    print_raw_features()
