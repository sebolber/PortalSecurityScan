package com.ahs.cvm.persistence.assessment;

import com.ahs.cvm.domain.enums.AssessmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    Optional<Assessment> findFirstByFindingIdOrderByVersionDesc(UUID findingId);

    List<Assessment> findByFindingIdOrderByVersionAsc(UUID findingId);

    List<Assessment>
            findByCveIdAndProductVersionIdAndEnvironmentIdAndStatusAndSupersededAtIsNull(
                    UUID cveId, UUID productVersionId, UUID environmentId,
                    AssessmentStatus status);
}
