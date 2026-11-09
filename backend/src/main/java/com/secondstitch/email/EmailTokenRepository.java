package com.secondstitch.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenHashAndPurpose(String tokenHash, EmailToken.Purpose purpose);

    Optional<EmailToken> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, EmailToken.Purpose purpose);

    /** Uses up every open link of this kind for a user (e.g. all reset links once the password is changed). */
    @Modifying
    @Query("update EmailToken t set t.usedAt = :now "
            + "where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
    int useAll(@Param("userId") Long userId, @Param("purpose") EmailToken.Purpose purpose, @Param("now") Instant now);
}
