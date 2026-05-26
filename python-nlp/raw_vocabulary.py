from nltk.tokenize import word_tokenize
from collections import Counter

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

def print_vocab():
    samples = [
        ("AI LONG", AI_TEXT),
        ("HUMAN LONG", HUMAN_TEXT),
        ("AI SHORT", AI_SHORT),
        ("HUMAN SHORT", HUMAN_SHORT)
    ]
    for name, text in samples:
        print(f"\n=== {name} ===")
        words = word_tokenize(text.lower())
        words = [w for w in words if w.isalpha()]
        N = len(words)
        unique_words = set(words)
        ttr = len(unique_words) / N
        
        word_freq = Counter(words)
        hapax = sum(1 for count in word_freq.values() if count == 1)
        hapax_ratio = hapax / N
        
        combined = 0.6 * ttr + 0.4 * hapax_ratio
        
        # Check repetitions
        bigrams = [tuple(words[i:i+2]) for i in range(N-1)]
        trigrams = [tuple(words[i:i+3]) for i in range(N-2)]
        bigram_counts = Counter(bigrams)
        trigram_counts = Counter(trigrams)
        repeated_bigrams = sum(1 for count in bigram_counts.values() if count > 1)
        bigram_rep = repeated_bigrams / max(len(bigram_counts), 1)
        repeated_trigrams = sum(1 for count in trigram_counts.values() if count > 1)
        trigram_rep = repeated_trigrams / max(len(trigram_counts), 1)
        rep_rate = 0.5 * bigram_rep + 0.5 * trigram_rep
        
        print(f"Words (N): {N}")
        print(f"Unique words: {len(unique_words)}")
        print(f"TTR: {ttr:.4f}")
        print(f"Hapax Ratio: {hapax_ratio:.4f}")
        print(f"Combined Vocab: {combined:.4f}")
        print(f"Repetition Rate: {rep_rate:.4f}")

if __name__ == "__main__":
    print_vocab()
