package ai.openclaw.stockweb.auth;

import java.time.Instant;

public record UserAuthRecord(
        Long id,
        String username,
        String passwordHash,
        Instant createdAt
) {
    public UserAuthRecord(String username, String passwordHash, Instant createdAt) {
        this(null, username, passwordHash, createdAt);
    }
}
