package com.legalmetrology.auth.repository;

import com.legalmetrology.auth.entity.RefreshToken;
import com.legalmetrology.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user = :user and rt.revoked = false")
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.token = :token")
    void revokeByToken(@Param("token") String token);
}
