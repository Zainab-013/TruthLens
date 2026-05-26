import numpy as np
from nltk.tokenize import word_tokenize

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

def print_word_length():
    samples = [
        ("AI LONG", AI_TEXT),
        ("HUMAN LONG", HUMAN_TEXT),
        ("AI SHORT", AI_SHORT),
        ("HUMAN SHORT", HUMAN_SHORT)
    ]
    for name, text in samples:
        words = word_tokenize(text.lower())
        words = [w for w in words if w.isalpha()]
        lengths = [len(w) for w in words]
        avg_len = np.mean(lengths)
        std_len = np.std(lengths)
        
        # Count words with length >= 6
        long_words = sum(1 for w in words if len(w) >= 6)
        frac_long_words = long_words / len(words)
        
        print(f"\n=== {name} ===")
        print(f"Average Word Length: {avg_len:.4f}")
        print(f"Std Word Length: {std_len:.4f}")
        print(f"Fraction of words >= 6 chars: {frac_long_words:.4f}")

if __name__ == "__main__":
    print_word_length()
