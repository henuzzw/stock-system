package ai.openclaw.stockweb.auth;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public UserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long createUser(String username, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                """
                INSERT INTO users (username, password_hash)
                VALUES (:username, :passwordHash)
                """,
                new MapSqlParameterSource()
                        .addValue("username", username)
                        .addValue("passwordHash", passwordHash),
                keyHolder
        );

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create user");
        }
        return key.longValue();
    }

    public Optional<UserAuthRecord> findByUsername(String username) {
        List<UserAuthRecord> users = jdbc.query(
                """
                SELECT id, username, password_hash, created_at
                FROM users
                WHERE username = :username
                """,
                new MapSqlParameterSource("username", username),
                (rs, rowNum) -> new UserAuthRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        return users.stream().findFirst();
    }

    public Optional<UserBasicInfo> findBasicById(long userId) {
        List<UserBasicInfo> users = jdbc.query(
                """
                SELECT id, username, created_at
                FROM users
                WHERE id = :userId
                """,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new UserBasicInfo(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        return users.stream().findFirst();
    }
}
