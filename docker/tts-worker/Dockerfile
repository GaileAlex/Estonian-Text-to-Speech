FROM ghcr.io/tartunlp/text-to-speech-worker:3.0.0

# CUDA 12.8 build of PyTorch is required for RTX 50xx (Blackwell) GPUs.
# Versions are pinned to the ones the worker was tested with: an unpinned upgrade pulls whatever is latest.
RUN pip install torch==2.8.0 torchvision==0.23.0 torchaudio==2.8.0 --index-url https://download.pytorch.org/whl/cu128

# The base image already switches to the non-root "app" user, repeated here to make it explicit
USER app
