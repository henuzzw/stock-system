package ai.openclaw.stockweb.auth;

import java.time.Instant;

public record UserAuthRecord(
        long id,
        String username,
        String passwordHash,
        Instant createdAt
) {
}
