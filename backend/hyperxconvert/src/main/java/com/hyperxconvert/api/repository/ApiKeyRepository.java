package com.hyperxconvert.api.repository;

import com.hyperxconvert.api.entity.ApiKey;
import com.hyperxconvert.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUser(User user);
    
    Optional<ApiKey> findByApiKey(String apiKey);
    
    List<ApiKey> findByUserAndStatus(User user, ApiKey.ApiKeyStatus status);
}
