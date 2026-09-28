package ee.gaile.estoniantts.service;

import ee.gaile.estoniantts.config.TtsProperties;
import ee.gaile.estoniantts.dto.WorkerRequest;
import ee.gaile.estoniantts.dto.WorkerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpMessageReturnedException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsRequestService {

    private static final ParameterizedTypeReference<WorkerResponse> RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RabbitTemplate rabbitTemplate;
    private final TtsProperties properties;

    public byte[] requestTts(String text, String speaker, double speed) {
        WorkerResponse response;
        try {
            response = rabbitTemplate.convertSendAndReceiveAsType(
                    properties.exchange(),
                    properties.exchange() + "." + speaker,
                    new WorkerRequest(text, speaker, speed),
                    RESPONSE_TYPE
            );
        } catch (AmqpMessageReturnedException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No TTS worker is serving speaker '" + speaker + "'");
        } catch (AmqpException e) {
            log.error("Error sending TTS request", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ is not available");
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "TTS worker did not respond in time");
        }
        if (response.statusCode() != 200 || response.content() == null || response.content().audio() == null) {
            log.error("TTS worker error {}: {}", response.statusCode(), response.status());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TTS worker error: " + response.status());
        }

        return adjustVolume(response.content().audio());
    }

    private byte[] adjustVolume(byte[] audio) {
        try {
            return WavVolume.scale(audio, properties.volume());
        } catch (IOException | UnsupportedAudioFileException e) {
            log.error("Audio processing failed, returning audio as is", e);
            return audio;
        }
    }

}
