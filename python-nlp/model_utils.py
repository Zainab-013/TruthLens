"""
TruthLens - Model Utilities
Loads GPT-2 model ONCE and reuses it across requests.
Prevents reloading the model again and again (performance boost).
"""

_model = None
_tokenizer = None


def load_model():
    """
    Load GPT-2 model and tokenizer (lazy loading).
    The model is loaded only on the first call.
    Subsequent calls return the cached model.
    """
    global _model, _tokenizer

    if _model is None:
        print("[LOADING] GPT-2 model (first time only)...")
        from transformers import GPT2LMHeadModel, GPT2TokenizerFast

        _tokenizer = GPT2TokenizerFast.from_pretrained("gpt2")
        _model = GPT2LMHeadModel.from_pretrained("gpt2")
        _model.eval()  # Set to evaluation mode (no training)
        print("[OK] GPT-2 model loaded successfully!")

    return _model, _tokenizer
