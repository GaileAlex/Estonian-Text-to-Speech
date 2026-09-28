package ee.gaile.estoniantts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response message of the TartuNLP TTS worker; the audio is a base64 encoded WAV file.
 */
public record WorkerResponse(@JsonProperty("status_code") int statusCode, String status, Content content) {

    public record Content(byte[] audio) {
    }
}
