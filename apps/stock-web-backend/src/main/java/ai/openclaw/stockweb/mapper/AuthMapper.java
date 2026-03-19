package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.auth.UserAuthRecord;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface AuthMapper {

    @Select("""
            SELECT id, username, password_hash, created_at
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    Optional<UserAuthRecord> findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password_hash, created_at
            FROM users
            WHERE id = #{userId}
            LIMIT 1
            """)
    Optional<UserAuthRecord> findById(@Param("userId") long userId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM users
            WHERE username = #{username}
            """)
    boolean existsByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO users (username, password_hash, created_at)
            VALUES (#{username}, #{passwordHash}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserAuthRecord user);

    @Update("""
            UPDATE users
            SET password_hash = #{passwordHash}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash, @Param("updatedAt") Instant updatedAt);
}
