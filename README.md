## Project structure

```
├── src/                     Spring Boot REST API (POST /api/tts) that talks to the worker via RabbitMQ
├── docker/
│   ├── app/                 Dockerfile of the Spring Boot app
│   └── tts-worker/          Dockerfile of the TartuNLP TTS worker (GPU) + its config.yaml
├── models/                  model files for the worker (not in git, see below)
└── docker-compose.yml       rabbitmq + tts-worker + spring-app
```

## Models

[The releases section](https://github.com/TartuNLP/text-to-speech-worker/releases) contains the model files or their
download instructions. If a release does not specify the model information, the model from the previous release can
be used. We advise always using the latest available version to ensure best model quality and code compatibility.

The model configuration files included in `docker/tts-worker/config.yaml` correspond to the following `models/` directory
structure:

```
models
├── hifigan
│   ├── ljspeech
│   │   ├── config.json
│   │   └── model.pt
│   ├── vctk
│   │   ├── config.json
│   │   └── model.pt
└── tts
    └── multispeaker
        ├── config.yaml
        └── model_weights.hdf5
```
