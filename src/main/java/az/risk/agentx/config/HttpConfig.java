package az.risk.agentx.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class HttpConfig {
    @Bean
    public CloseableHttpClient closeableHttpClient() {
        return HttpClients.createDefault();
    }
}
