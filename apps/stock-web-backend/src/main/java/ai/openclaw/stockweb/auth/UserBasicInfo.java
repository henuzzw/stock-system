package ai.openclaw.stockweb.auth;

import java.time.LocalDateTime;

public record UserBasicInfo(
        long id,
        String username,
        LocalDateTime createdAt
) {
}
