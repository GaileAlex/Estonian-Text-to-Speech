## Models

[The releases section](https://github.com/TartuNLP/text-to-speech-worker/releases) contains the model files or their
download instructions. If a release does not specify the model information, the model from the previous release can
be used. We advise always using the latest available version to ensure best model quality and code compatibility.

The model configuration files included in `config/config.yaml` correspond to the following `models/` directory
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
