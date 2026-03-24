package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.auth.UserAuthRecord;
import ai.openclaw.stockweb.auth.UserBasicInfo;
import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash, created_at
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    Optional<UserAuthRecord> findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, created_at
            FROM users
            WHERE id = #{userId}
            LIMIT 1
            """)
    Optional<UserBasicInfo> findBasicById(@Param("userId") long userId);

    @Insert("""
            INSERT INTO users (username, password_hash, created_at)
            VALUES (#{username}, #{passwordHash}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash, @Param("createdAt") Instant createdAt);
}
