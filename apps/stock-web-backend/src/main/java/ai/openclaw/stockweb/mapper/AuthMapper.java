package ai.openclaw.stockweb.mapper;

import ai.openclaw.stockweb.auth.UserAuthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.Optional;

@Mapper
public interface AuthMapper {

    Optional<UserAuthRecord> findByUsername(@Param("username") String username);

    Optional<UserAuthRecord> findById(@Param("userId") long userId);

    boolean existsByUsername(@Param("username") String username);

    int insertUser(UserAuthRecord user);

    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash, @Param("updatedAt") Instant updatedAt);
}
