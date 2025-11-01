package ee.gaile.estoniantts;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * @author Aleksei Gaile 1 Nov 2025
 */
@RestController
@RequiredArgsConstructor
public class TtsController {

    private final TtsRequestService ttsRequestService;

    // Обновленный DTO
    public record TtsRequest(String text, String speakerName, Integer speed) {}

    @PostMapping("/api/tts")
    public Map<String, String> submitTtsJob(@RequestBody TtsRequest request) {
        String jobId = UUID.randomUUID().toString();

        // Вызываем обновленный сервис
        ttsRequestService.requestTts(request.text(), request.speakerName(), request.speed);

        return Map.of(
                "message", "Задача на синтез речи принята",
                "jobId", jobId
        );
    }
}
