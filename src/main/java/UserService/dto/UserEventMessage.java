package UserService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventMessage {
    private String eventType;
    private Long userId;
    private String userName;
    private String userEmail;
    private String timestamp;
}
