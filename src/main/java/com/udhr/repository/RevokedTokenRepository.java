package com.udhr.repository;

import com.udhr.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenId(String tokenId);

    // Rows whose token has expired on its own are no longer doing any work —
    // the token is refused on its expiry alone. Cleared periodically so the
    // table doesn't grow without bound.
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken r WHERE r.tokenExpiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
