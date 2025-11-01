package ee.gaile.estoniantts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * @author Aleksei Gaile 1 Nov 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TtsRequestService {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public byte[] requestTts(String text, String speakerName, Integer speed) {
        String correlationId = UUID.randomUUID().toString();
        try {
            Map<String, Object> requestBody = Map.of(
                    "text", text,
                    "speaker", speakerName,
                    "speed", speed
            );
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Object reply =rabbitTemplate.convertSendAndReceive(
                    TtsRabbitConfig.TTS_EXCHANGE,
                    jsonBody,
                    m -> {
                        m.getMessageProperties().setCorrelationId(correlationId);
                        m.getMessageProperties().setReplyTo(TtsRabbitConfig.TTS_OUT_QUEUE);
                        return m;
                    }
            );

            if (reply instanceof byte[] bytes) {
                String json = new String(bytes, StandardCharsets.UTF_8);
                Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>(){});

                Map<String, Object> content = (Map<String, Object>) response.get("content");
                String audioBase64 = (String) content.get("audio");

                if (audioBase64 == null) {
                    log.error("Пустой TTS ответ: {}", response.get("error"));
                    return null;
                }

                return Base64.getDecoder().decode(audioBase64);
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке TTS запроса", e);
        }

        return null;
    }
}
