package ee.gaile.estoniantts;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Aleksei Gaile
 */
@Configuration
public class TtsRabbitConfig {

    public static final String TTS_OUT_QUEUE = "tts.out";
    public static final String TTS_IN_QUEUE = "tts.in";
    public static final String TTS_EXCHANGE = "text-to-speech.multispeaker_d3ce4a45";

    @Bean
    public Queue ttsOutQueue() {
        return new Queue(TTS_OUT_QUEUE, true);
    }

    @Bean
    public DirectExchange ttsExchange() {
        return new DirectExchange(TTS_EXCHANGE, true, false);
    }


    @Bean
    public Queue ttsInQueue() {
        return new Queue(TTS_IN_QUEUE);
    }
}
