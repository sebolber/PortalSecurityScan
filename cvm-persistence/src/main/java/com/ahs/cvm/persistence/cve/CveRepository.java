package com.ahs.cvm.persistence.cve;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CveRepository extends JpaRepository<Cve, UUID> {

    Optional<Cve> findByCveId(String cveId);

    /**
     * Server-seitige Paginierung mit optionalen Filtern (Iteration 39,
     * CVM-83). Parameter die {@code null} sind, werden ignoriert.
     *
     * <p>Severity wird aus CVSS abgeleitet:
     * CRITICAL >= 9.0, HIGH >= 7.0, MEDIUM >= 4.0, LOW > 0.0,
     * sonst INFORMATIONAL. Die Query uebergibt die (optionalen)
     * Lower-/Upper-Grenzen; den Mapping-Teil uebernimmt der Service.
     *
     * @param searchLower  Kleinbuchstaben-Suchstring; leerer String
     *                     bedeutet "kein Filter". Darf nicht {@code null}
     *                     sein - PG-JDBC bindet Null-Strings sonst als
     *                     {@code bytea} und die LIKE-Typpruefung schlaegt
     *                     fehl (siehe CveQueryService#findPage).
     * @param minScore     Optional (exklusive/inklusive gem. Caller)
     * @param maxScore     Optional
     * @param informational Wenn {@code true}: nur CVEs mit CVSS = 0 oder null
     * @param onlyKev      Wenn {@code true}: kev_listed = true
     */
    @Query("SELECT c FROM Cve c WHERE "
            + "(:searchLower = '' "
            + "  OR LOWER(c.cveId) LIKE CONCAT('%', :searchLower, '%') "
            + "  OR LOWER(c.summary) LIKE CONCAT('%', :searchLower, '%')) "
            + "AND (:minScore IS NULL OR c.cvssBaseScore >= :minScore) "
            + "AND (:maxScore IS NULL OR c.cvssBaseScore < :maxScore) "
            + "AND (:informational = FALSE "
            + "     OR c.cvssBaseScore IS NULL OR c.cvssBaseScore <= 0) "
            + "AND (:onlyKev = FALSE OR c.kevListed = TRUE)")
    Page<Cve> searchPage(
            @Param("searchLower") String searchLower,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            @Param("informational") boolean informational,
            @Param("onlyKev") boolean onlyKev,
            Pageable pageable);
}
