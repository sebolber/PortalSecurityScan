package com.ahs.cvm.persistence.profileassist;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileAssistSessionRepository
        extends JpaRepository<ProfileAssistSession, UUID> {

    /**
     * Kandidaten fuer den Cleanup-Cron: abgelaufen oder finalisiert
     * und aelter als {@code grenze} (Go-Live-Nachzug zu
     * Iteration&nbsp;18).
     */
    @Query("SELECT s FROM ProfileAssistSession s "
            + "WHERE (s.status = 'EXPIRED' OR s.status = 'FINALIZED') "
            + "  AND COALESCE(s.updatedAt, s.createdAt) < :grenze")
    List<ProfileAssistSession> findCleanupKandidaten(
            @Param("grenze") Instant grenze);

    @Modifying
    @Query("DELETE FROM ProfileAssistSession s WHERE s.id IN :ids")
    int deleteByIds(@Param("ids") List<UUID> ids);
}
