package ee.gaile.estoniantts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

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

            Object reply = rabbitTemplate.convertSendAndReceive(
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
                Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {
                });
                Map<String, Object> content = (Map<String, Object>) response.get("content");
                String audioBase64 = (String) content.get("audio");

                if (audioBase64 == null) {
                    log.error("Empty TTS response: {}", response.get("error"));
                    return null;
                }

                // Декодируем Base64 и сразу обрабатываем
                byte[] rawAudio = Base64.getDecoder().decode(audioBase64);
                return processAudio(rawAudio);
            }

        } catch (Exception e) {
            log.error("Error sending TTS request", e);
        }
        return null;
    }

    private byte[] processAudio(byte[] audioBytes) {
        File inputFile = null;
        File outputFile = null;
        try {
            inputFile = File.createTempFile("tts_input", ".wav");
            outputFile = File.createTempFile("tts_output", ".wav");

            Files.write(inputFile.toPath(), audioBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "python3", "/app/process_tts.py",
                    inputFile.getAbsolutePath(),
                    outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.warn("[PY] {}", line);
                }
            }

            int exit = process.waitFor();
            log.info("Python exit code: {}", exit);

            if (!outputFile.exists() || outputFile.length() == 0) {
                log.error("Python didn't produce output file");
                return audioBytes;
            }

            return Files.readAllBytes(outputFile.toPath());
        } catch (Exception e) {
            log.error("Audio processing failed", e);
            return audioBytes;
        } finally {
            if (inputFile != null) inputFile.delete();
            if (outputFile != null) outputFile.delete();
        }
    }

}
