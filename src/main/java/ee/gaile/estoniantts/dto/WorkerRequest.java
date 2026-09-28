package ee.gaile.estoniantts.dto;

/**
 * Request message of the TartuNLP TTS worker.
 */
public record WorkerRequest(String text, String speaker, double speed) {
}
