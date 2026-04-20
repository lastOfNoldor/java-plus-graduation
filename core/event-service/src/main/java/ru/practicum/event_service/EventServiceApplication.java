package ru.practicum.event_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@EnableFeignClients(basePackages = "ru.practicum.interaction_api.contract")
@ComponentScan(basePackages = {
        "ru.practicum.event_service",
        "ru.practicum.ewm.stats.client"
})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }

}