# Estonian Text-to-Speech

Estonian speech synthesis for [gaile.ee](https://gaile.ee/): a small Spring Boot REST API in front of the
[TartuNLP text-to-speech worker](https://github.com/TartuNLP/text-to-speech-worker) (10 Estonian voices, GPU).
The CV application calls it at `http://estonian-tts-spring-app-1:8380/api/tts` over the shared Docker network
`kokoro-net`.

## How it works

```
CV (cv-app) ── POST /api/tts ──▶ spring-app ── AMQP ──▶ rabbitmq ──▶ tts-worker (GPU)
                 audio/wav  ◀──  volume × 0.5  ◀── base64 WAV, direct reply-to ◀──
```

1. `spring-app` sends `{text, speaker, speed}` to the exchange `text-to-speech` with the routing key
   `text-to-speech.<speaker>` and waits for the answer on the
   [direct reply-to](https://www.rabbitmq.com/docs/direct-reply-to) queue (up to 60 s).
2. The worker binds one queue with a routing key per speaker, synthesizes the text and answers with a base64 encoded
   WAV file.
3. `spring-app` scales the volume of the WAV (`tts.volume`) and returns it as `audio/wav`.

The messages are `mandatory`: when no worker serves the speaker (the worker is down or the name is unknown),
RabbitMQ returns the request at once and the API answers 503 instead of waiting for the timeout.

## Technologies

- **Java 25**, **Spring Boot 4.1** (Web MVC, Spring AMQP, Actuator)
- **RabbitMQ 4.3** with the management and Prometheus plugins
- **TartuNLP text-to-speech-worker 3.0.0** (TransformerTTS + HiFi-GAN), with **PyTorch 2.8 for CUDA 12.8**: the build
  of the image needs it for RTX 50xx (Blackwell) GPUs
- **Micrometer**: Prometheus metrics, and the traceId of CV in the log lines
- **Docker Compose**

## Project structure

```
├── src/                     Spring Boot REST API (POST /api/tts) that talks to the worker via RabbitMQ
├── docker/
│   ├── app/                 Dockerfile of the Spring Boot app
│   ├── rabbitmq/            configuration of RabbitMQ that the worker needs
│   └── tts-worker/          Dockerfile of the TartuNLP TTS worker (GPU) + its config.yaml
├── models/                  model files for the worker (not in git, see below)
└── docker-compose.yml       rabbitmq + tts-worker + spring-app
```

## Prerequisites

- **Docker** with an **NVIDIA GPU** and its driver (Docker Desktop with WSL 2 on the host of gaile.ee)
- The network **`kokoro-net`**: the CV project creates it, or `docker network create kokoro-net`
- The **models** in `models/`, see [Models](#models)
- For development: **Java 25** (Maven comes with the wrapper `mvnw`)

## Running

```bash
# build and start rabbitmq, the worker and the API (also a deploy after a change)
docker compose up -d --build

# only the API, after a change of src/
docker compose up -d --build spring-app

# logs
docker compose logs -f spring-app tts-worker
```

The API listens on `127.0.0.1:8380` of the host and on `estonian-tts-spring-app-1:8380` in `kokoro-net`.
RabbitMQ is not published to the host. Until the worker has loaded the models (`Ready to process requests.` in its
log), the requests get 503.

### Development

RabbitMQ and the worker run in Docker; the API starts locally with the profile `dev` on port 8385 and connects to
`localhost:5672`, so for this the port of RabbitMQ must be published (e.g. `ports: ["5672:5672"]` in a
`docker-compose.override.yml`).

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw verify            # tests
```

## API

### `POST /api/tts`

| Field         | Type   | Required | Description                                                     |
|---------------|--------|----------|-----------------------------------------------------------------|
| `text`        | string | yes      | Estonian text; long texts are split into sentences by the worker |
| `speakerName` | string | yes      | one of the voices below                                          |
| `speed`       | number | no       | tempo, `1.0` by default; higher is faster                        |

Voices: `albert`, `indrek`, `kalev`, `kylli`, `liivika`, `mari`, `meelis`, `peeter`, `tambet`, `vesta`
(`docker/tts-worker/config.yaml`).

```bash
curl -o tere.wav -H "Content-Type: application/json" \
     -d '{"text": "Tere! Kuidas läheb?", "speakerName": "vesta"}' \
     http://127.0.0.1:8380/api/tts
```

The answer is a WAV file (`audio/wav`). The errors are `application/problem+json` with the reason in `detail`:

| Status | When                                                         |
|--------|--------------------------------------------------------------|
| 400    | `text` or `speakerName` is blank                             |
| 502    | the worker answered with an error                            |
| 503    | no worker serves the speaker, or RabbitMQ is not available   |
| 504    | the worker did not answer in 60 s                            |

## Configuration

`src/main/resources/application.properties`; in Docker the environment variables of `docker-compose.yml` override it.

| Property                             | Default                   | Description                                       |
|--------------------------------------|---------------------------|---------------------------------------------------|
| `server.port`                        | `8380`                    | port of the API                                   |
| `spring.rabbitmq.host` / `.port`     | `estonian-tts-rabbitmq-1` / `5672` | `MQ_HOST`, `MQ_PORT`                              |
| `spring.rabbitmq.username` / `.password` | `guest` / `guest`     | `MQ_USERNAME`, `MQ_PASSWORD`                      |
| `spring.rabbitmq.template.reply-timeout` | `60s`                 | how long a request waits for the worker           |
| `tts.exchange`                       | `text-to-speech`          | exchange of the worker                            |
| `tts.volume`                         | `0.5`                     | factor of the volume of the synthesized audio     |
| `management.server.port`             | `8081`                    | Actuator: health and Prometheus metrics           |

## Monitoring

The observability project of the host (Prometheus, Grafana, Loki) scrapes:

- `estonian-tts-spring-app-1:8081/actuator/prometheus`: the requests (`http_server_requests`, with a histogram
  for p95) and the JVM;
- `estonian-tts-rabbitmq-1:15692`: RabbitMQ (the `rabbitmq_prometheus` plugin of the image).

The log lines of a synthesis carry the traceId of the request of CV (the `traceparent` header); no span leaves the
application. Health: `/actuator/health` on port 8081.

## Dependencies and updates

- GitHub **Dependabot alerts** and **security updates** are on for the repository.
- `pom.xml` sets newer versions of **Tomcat**, **Jackson** and the **RabbitMQ Java client** than Spring Boot 4.1.1
  manages: the managed ones have open advisories. Each override goes away when Spring Boot manages a version at least
  as new.
- **RabbitMQ 4.3** denies non-durable, non-exclusive queues by default, and the worker declares exactly such a queue.
  `docker/rabbitmq/20-tts-worker.conf` permits them. The minor version of the image is pinned, so that a new minor
  that removes the feature does not arrive with a restart. RabbitMQ keeps nothing durable here: before an upgrade of
  the minor, `docker compose up -d --renew-anon-volumes rabbitmq` starts it with a clean data directory.
- The worker stays on **3.0.0**. 3.1.0 has another layout of the models (one HiFi-GAN vocoder from Hugging Face),
  another `config.yaml` and PyTorch 2.1, so the move to it is a migration of its own, with the CUDA 12.8 build
  checked again.

## Models

The worker 3.0.0 uses the models of its release
[v3.0.0](https://github.com/TartuNLP/text-to-speech-worker/releases/tag/v3.0.0) (the release contains the model files
or their download instructions; a release without model information uses the models of the previous one). The models
of 3.1.0 have another layout and do not work with 3.0.0.

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

From the root of the project:

```bash
release=https://github.com/TartuNLP/text-to-speech-worker/releases/download/v3.0.0
wget -P models/tts/ $release/multispeaker.zip
wget -P models/hifigan/ $release/ljspeech.zip $release/vctk.zip
unzip -d models/tts/ models/tts/multispeaker.zip
unzip -d models/hifigan/ models/hifigan/ljspeech.zip
unzip -d models/hifigan/ models/hifigan/vctk.zip
```

## License

MIT, see [LICENSE](LICENSE). The worker and the models are by [TartuNLP](https://github.com/TartuNLP)
(University of Tartu).
