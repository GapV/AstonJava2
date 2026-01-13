package UserService.kafka;

import UserService.dto.UserEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, UserEventMessage> kafkaTemplate;

    private static final String TOPIC = "user-events";

    public void sendUserCreatedEvent(Long userId, String userName, String userEmail) {
        UserEventMessage message = UserEventMessage.builder()
                .eventType("USER_CREATED")
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();

        sendMessage(message);
        log.info("Отправлено событие создания пользователя: {}", message);
    }

    public void sendUserDeletedEvent(Long userId, String userName, String userEmail) {
        UserEventMessage message = UserEventMessage.builder()
                .eventType("USER_DELETED")
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .timestamp(java.time.LocalDateTime.now().toString())
                .build();

        sendMessage(message);
        log.info("Отправлено событие удаления пользователя: {}", message);
    }

    private void sendMessage(UserEventMessage message) {
        kafkaTemplate.send(TOPIC, message.getUserEmail(), message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Сообщение успешно отправлено в топик {}: {}", TOPIC, message);
                    } else {
                        log.error("Ошибка отправки сообщения в Kafka: {}", ex.getMessage());
                    }
                });
    }
}
