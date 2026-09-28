package ee.gaile.estoniantts.controller;

import ee.gaile.estoniantts.dto.TtsRequest;
import ee.gaile.estoniantts.service.TtsRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Aleksei Gaile 1 Nov 2025
 */
@RestController
@RequiredArgsConstructor
public class TtsController {

    private static final double DEFAULT_SPEED = 1.0;

    private final TtsRequestService ttsRequestService;

    @PostMapping(value = "/api/tts", produces = "audio/wav")
    public byte[] submitTtsJob(@RequestBody TtsRequest request) {
        if (!StringUtils.hasText(request.text())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text must not be blank");
        }
        if (!StringUtils.hasText(request.speakerName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "speakerName must not be blank");
        }
        double speed = request.speed() != null ? request.speed() : DEFAULT_SPEED;

        return ttsRequestService.requestTts(request.text(), request.speakerName(), speed);
    }

}
