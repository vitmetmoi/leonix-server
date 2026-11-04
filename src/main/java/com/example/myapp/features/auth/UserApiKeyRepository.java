package com.example.myapp.features.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {
    List<UserApiKey> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndRevokedAtIsNull(Long userId);
    Optional<UserApiKey> findByHashedKeyAndRevokedAtIsNull(String hashedKey);
    Optional<UserApiKey> findByIdAndUserId(Long id, Long userId);
}
