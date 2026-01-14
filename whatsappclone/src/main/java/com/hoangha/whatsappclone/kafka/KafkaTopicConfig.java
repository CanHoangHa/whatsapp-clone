package com.hoangha.whatsappclone.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageTopic() {
        return TopicBuilder.name("message-topic") // Tên topic
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userStatusTopic() {
        return TopicBuilder.name("user-status")
                .partitions(1)
                .replicas(1)
                .build();
    }
}