from transformers import RobertaTokenizer

AI_TEXT = "Artificial intelligence is rapidly transforming the global economy."
HUMAN_TEXT = "I remember walking down the old cobblestone street near my grandmother's house."

tokenizer = RobertaTokenizer.from_pretrained("roberta-base-openai-detector")

print("AI TEXT TOKENS:")
print(tokenizer.tokenize(AI_TEXT))
print(tokenizer(AI_TEXT))

print("\nHUMAN TEXT TOKENS:")
print(tokenizer.tokenize(HUMAN_TEXT))
print(tokenizer(HUMAN_TEXT))
