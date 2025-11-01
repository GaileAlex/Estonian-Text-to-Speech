package ee.gaile.estoniantts;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

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

    public void requestTts(String text, String speakerName, Integer speed) {
        String correlationId = UUID.randomUUID().toString();
        try {
            Map<String, Object> requestBody = Map.of(
                    "text", text,
                    "speaker", speakerName,
                    "speed", speed
            );
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            rabbitTemplate.convertAndSend(
                    TtsRabbitConfig.TTS_EXCHANGE, // exchange воркера
                    jsonBody,                      // тело сообщения
                    message -> {
                        message.getMessageProperties().setCorrelationId(correlationId);
                        message.getMessageProperties().setReplyTo(TtsRabbitConfig.TTS_OUT_QUEUE);
                        return message;
                    }
            );


            log.info("TTS запрос отправлен [{}]: Спикер={}, Текст={}", correlationId, speakerName, text);

        } catch (Exception e) {
            log.error("Ошибка при отправке TTS запроса", e);
        }
    }
}
