package ai.openclaw.stockweb.auth;

import java.time.Instant;

public record UserBasicInfo(
        long id,
        String username,
        Instant createdAt
) {
}
