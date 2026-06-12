"""
TruthLens - Model Utilities
Loads GPT-2 model ONCE and reuses it across requests.
Prevents reloading the model again and again (performance boost).
"""

import threading

_model = None
_tokenizer = None
_lock = threading.Lock()


def load_model():
    """
    Load GPT-2 model and tokenizer (lazy loading).
    The model is loaded only on the first call.
    Subsequent calls return the cached model.
    """
    global _model, _tokenizer

    if _model is None:
        with _lock:
            if _model is None:
                print("[LOADING] GPT-2 model (first time only)...")
                import torch
                import gc
                
                # Restrict PyTorch thread count to prevent CPU thread thrashing under concurrent requests
                torch.set_num_threads(1)
                
                from transformers import GPT2LMHeadModel, GPT2TokenizerFast

                _tokenizer = GPT2TokenizerFast.from_pretrained("gpt2")
                _model = GPT2LMHeadModel.from_pretrained(
                    "gpt2",
                    torch_dtype=torch.bfloat16,
                    low_cpu_mem_usage=True
                )
                _model.eval()  # Set to evaluation mode (no training)
                
                # Clean up memory allocated during loading process
                gc.collect()
                print("[OK] GPT-2 model loaded successfully in bfloat16!")

    return _model, _tokenizer


