package ai.openclaw.stockweb.auth;

public record LoginRequest(
        String username,
        String password
) {
}
