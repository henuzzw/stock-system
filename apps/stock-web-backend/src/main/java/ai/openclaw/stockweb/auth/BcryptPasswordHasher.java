package ai.openclaw.stockweb.auth;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class BcryptPasswordHasher {
    public String hash(String password) {
        return runBcryptScript(
                password,
                """
                import bcrypt, sys
                password = sys.stdin.buffer.read()
                print(bcrypt.hashpw(password, bcrypt.gensalt()).decode(), end="")
                """,
                "Failed to generate BCrypt hash"
        );
    }

    public boolean matches(String password, String passwordHash) {
        String result = runBcryptScript(
                password + "\n" + passwordHash,
                """
                import bcrypt, sys
                password, password_hash = sys.stdin.buffer.read().split(b"\\n", 1)
                print("true" if bcrypt.checkpw(password, password_hash.strip()) else "false", end="")
                """,
                "Failed to verify BCrypt hash"
        );
        return "true".equalsIgnoreCase(result);
    }

    private String runBcryptScript(String input, String script, String fallbackMessage) {
        ProcessBuilder builder = new ProcessBuilder(
                "python3",
                "-c",
                script
        );

        try {
            Process process = builder.start();
            try (var output = process.getOutputStream()) {
                output.write(input.getBytes(StandardCharsets.UTF_8));
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0 || stdout.isBlank()) {
                throw new IllegalStateException(
                        error.isBlank() ? fallbackMessage : error
                );
            }
            return stdout;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to launch BCrypt helper", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("BCrypt helper interrupted", e);
        }
    }
}
