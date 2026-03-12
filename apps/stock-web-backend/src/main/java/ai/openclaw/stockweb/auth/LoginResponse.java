package ai.openclaw.stockweb.auth;

public record LoginResponse(
        boolean success,
        String token,
        UserBasicInfo user
) {
}
