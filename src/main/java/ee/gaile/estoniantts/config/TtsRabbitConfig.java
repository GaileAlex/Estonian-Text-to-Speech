package ee.gaile.estoniantts.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Aleksei Gaile
 */
@Configuration
@EnableConfigurationProperties(TtsProperties.class)
public class TtsRabbitConfig {

    /**
     * Declared with the same arguments as the worker does (non-durable), so the exchange exists even before
     * the worker starts and requests without a worker are returned instead of failing on a missing exchange.
     */
    @Bean
    public DirectExchange ttsExchange(TtsProperties properties) {
        return new DirectExchange(properties.exchange(), false, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
