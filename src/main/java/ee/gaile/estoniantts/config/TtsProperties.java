package ee.gaile.estoniantts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param exchange RabbitMQ exchange of the TTS worker; the worker binds its queue with routing keys "exchange.speaker"
 * @param volume   factor applied to the synthesized audio volume
 */
@ConfigurationProperties(prefix = "tts")
public record TtsProperties(String exchange, double volume) {
}
