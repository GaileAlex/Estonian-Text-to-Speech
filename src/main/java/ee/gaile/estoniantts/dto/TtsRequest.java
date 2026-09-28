package ee.gaile.estoniantts.dto;

public record TtsRequest(String text, String speakerName, Double speed) {
}
