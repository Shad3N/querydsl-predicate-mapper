package io.github.shad3n.predicatemapper.examples.sender.config;

import io.github.shad3n.predicatemapper.examples.sender.client.ProductClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration for HTTP clients using HttpExchange.
 */
@Configuration
public class ClientConfig {

    @Value("${receiver.base-url}")
    private String receiverBaseUrl;

    @Bean
    public ProductClient productClient() {
        RestClient restClient = RestClient.builder()
                                          .baseUrl(receiverBaseUrl)
                                          .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(adapter)
                .build();

        return factory.createClient(ProductClient.class);
    }
}