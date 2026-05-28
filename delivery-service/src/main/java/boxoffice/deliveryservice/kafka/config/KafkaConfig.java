package boxoffice.deliveryservice.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.hub-transfer-dispatched}")
    private String hubTransferDispatched;

    @Value("${kafka.topic.transfer-assign-success}")
    private String transferAssignSuccess;

    @Value("${kafka.topic.transfer-assign-failed}")
    private String transferAssignFailed;

    @Value("${kafka.topic.order-cancelled}")
    private String orderCancelled;

    @Bean
    public NewTopic hubTransferDispatchedTopic() {
        return TopicBuilder.name(hubTransferDispatched).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transferAssignSuccessTopic() {
        return TopicBuilder.name(transferAssignSuccess).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic transferAssignFailedTopic() {
        return TopicBuilder.name(transferAssignFailed).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(orderCancelled).partitions(3).replicas(1).build();
    }
}