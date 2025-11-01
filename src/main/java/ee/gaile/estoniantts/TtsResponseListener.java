package ee.gaile.estoniantts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;

/**
 * @author Aleksei Gaile
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TtsResponseListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = TtsRabbitConfig.TTS_OUT_QUEUE)
    public void handleTtsResponse(Message message) {
        try {
            // Парсим JSON
            String json = new String(message.getBody());
            Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});

            // Берем содержимое "content"
            Map<String, Object> content = (Map<String, Object>) response.get("content");
            String audioBase64 = (String) content.get("audio");

            if (audioBase64 == null) {
                log.error("Пустой TTS ответ: {}", response.get("error"));
                return;
            }

            // Декодируем Base64 и сохраняем как WAV напрямую
            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
            String filename = "tts_output.wav";

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                fos.write(audioBytes);
            }

            Integer sampleRate = (Integer) content.get("sampling_rate");
            log.info("TTS ответ получен. SampleRate={}, файл={}", sampleRate, filename);

        } catch (Exception e) {
            log.error("Ошибка обработки TTS ответа", e);
        }
    }
}
