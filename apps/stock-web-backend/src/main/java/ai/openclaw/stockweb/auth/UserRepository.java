package ai.openclaw.stockweb.auth;

import ai.openclaw.stockweb.mapper.AuthMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class UserRepository {
    private final AuthMapper authMapper;

    public UserRepository(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public long createUser(String username, String passwordHash) {
        UserAuthRecord user = new UserAuthRecord(username, passwordHash, Instant.now());
        authMapper.insertUser(user);
        return user.id();
    }

    public Optional<UserAuthRecord> findByUsername(String username) {
        return authMapper.findByUsername(username);
    }

    public Optional<UserBasicInfo> findBasicById(long userId) {
        return authMapper.findById(userId).map(u -> new UserBasicInfo(u.id(), u.username(), 
            java.time.LocalDateTime.ofInstant(u.createdAt(), java.time.ZoneId.systemDefault())));
    }

    public boolean existsByUsername(String username) {
        return authMapper.existsByUsername(username);
    }
}
