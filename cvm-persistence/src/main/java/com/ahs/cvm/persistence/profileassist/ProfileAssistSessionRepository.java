package com.ahs.cvm.persistence.profileassist;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileAssistSessionRepository
        extends JpaRepository<ProfileAssistSession, UUID> {
}
