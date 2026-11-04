package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiDispatchGroupRepository extends JpaRepository<AiDispatchGroup, Long> {
    Optional<AiDispatchGroup> findByGroupCode(String groupCode);
}
