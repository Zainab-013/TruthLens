from transformers import RobertaTokenizer

AI_TEXT = "Artificial intelligence is rapidly transforming the global economy."
HUMAN_TEXT = "I remember walking down the old cobblestone street near my grandmother's house."

tokenizer = RobertaTokenizer.from_pretrained("roberta-base-openai-detector")

tokens_ai = [t.encode('ascii', errors='replace').decode('ascii') for t in tokenizer.tokenize(AI_TEXT)]
tokens_human = [t.encode('ascii', errors='replace').decode('ascii') for t in tokenizer.tokenize(HUMAN_TEXT)]

print("AI TEXT TOKENS:")
print(tokens_ai)
print("AI TEXT IDS:", tokenizer(AI_TEXT)["input_ids"])

print("\nHUMAN TEXT TOKENS:")
print(tokens_human)
print("HUMAN TEXT IDS:", tokenizer(HUMAN_TEXT)["input_ids"])
