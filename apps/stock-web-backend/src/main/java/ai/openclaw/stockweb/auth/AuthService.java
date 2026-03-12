package ai.openclaw.stockweb.auth;

import ai.openclaw.stockweb.account.AccountRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BcryptPasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            BcryptPasswordHasher passwordHasher,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();

        validate(username, password);

        try {
            long userId = userRepository.createUser(username, passwordHasher.hash(password));
            accountRepository.createDefaultAccount(userId);
            return new RegisterResponse(true, "Registration successful");
        } catch (DuplicateKeyException ex) {
            throw new RegistrationException("Username already exists");
        }
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        String password = request.password() == null ? "" : request.password();

        if (username.isBlank() || password.isBlank()) {
            throw new AuthException("Username and password are required");
        }

        UserAuthRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("Invalid username or password"));

        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new AuthException("Invalid username or password");
        }

        UserBasicInfo basicInfo = new UserBasicInfo(user.id(), user.username(), user.createdAt());
        return new LoginResponse(true, jwtService.createToken(basicInfo), basicInfo);
    }

    public UserBasicInfo getCurrentUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        long userId = jwtService.validateAndExtractUserId(token)
                .orElseThrow(() -> new AuthException("Invalid or expired token"));
        return userRepository.findBasicById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AuthException("Missing Authorization header");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new AuthException("Authorization header must use Bearer token");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new AuthException("Bearer token is required");
        }
        return token;
    }

    private void validate(String username, String password) {
        if (username.isBlank()) {
            throw new RegistrationException("Username must not be empty");
        }
        if (password.length() < 8) {
            throw new RegistrationException("Password must be at least 8 characters");
        }
    }
}
