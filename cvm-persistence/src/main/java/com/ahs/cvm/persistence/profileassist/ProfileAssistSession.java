package com.ahs.cvm.persistence.profileassist;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.profile.ContextProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stateful Dialog-Session fuer den Profil-Assistenten
 * (Iteration 18, CVM-43). TTL 24 h; abgelaufene Sessions werden
 * vom Service auf {@code EXPIRED} gehoben.
 */
@Entity
@Table(name = "profile_assist_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProfileAssistSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false, updatable = false)
    private Environment environment;

    @Column(name = "started_by", nullable = false, updatable = false)
    private String startedBy;

    @Column(name = "dialog_json", nullable = false, columnDefinition = "text")
    private String dialogJson;

    @Column(name = "pending_question", columnDefinition = "text")
    private String pendingQuestion;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_draft_id")
    private ContextProfile finalizedDraft;

    @PrePersist
    void init() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (dialogJson == null) {
            dialogJson = "[]";
        }
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
