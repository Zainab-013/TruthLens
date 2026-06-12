# Use a lightweight official Python image
FROM python:3.11-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PORT=5000

# Install build essential tools for compiled python libraries
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy dependency list
COPY requirements.txt .

# Install dependencies (CPU-only PyTorch to keep container size under 1GB)
RUN pip install --no-cache-dir torch==2.6.0 --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir -r requirements.txt

# Pre-compile/optimize and save GPT-2 in bfloat16 to avoid runtime RAM spikes during load
RUN python -c "from transformers import GPT2LMHeadModel, GPT2TokenizerFast; import torch; m = GPT2LMHeadModel.from_pretrained('gpt2'); m.to(torch.bfloat16).save_pretrained('/app/optimized_model'); t = GPT2TokenizerFast.from_pretrained('gpt2'); t.save_pretrained('/app/optimized_model')"

# Copy the rest of the application
COPY . .


# Expose port
EXPOSE 5000

# Command to run FastAPI server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]
