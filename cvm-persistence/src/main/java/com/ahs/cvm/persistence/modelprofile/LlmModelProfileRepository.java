package com.ahs.cvm.persistence.modelprofile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmModelProfileRepository
        extends JpaRepository<LlmModelProfile, UUID> {

    Optional<LlmModelProfile> findByProfileKey(String profileKey);
}
