package ru.practicum.stats_client.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import ru.practicum.stats_client.RestStatClient;

@AutoConfiguration
public class StatClientAutoConfiguration {

    @Bean
    public RestStatClient restStatClient(
            DiscoveryClient discoveryClient,
            @Value("${discovery.services.stats-server-id}") String statsServiceId) {
        return new RestStatClient(discoveryClient, statsServiceId);
    }
}