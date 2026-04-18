package com.ahs.cvm.persistence.alert;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertDispatchRepository extends JpaRepository<AlertDispatch, UUID> {

    /**
     * Letzte Dispatches absteigend nach {@code dispatchedAt}
     * (Iteration 27c, CVM-63). Dient der Alert-Historie im UI.
     */
    List<AlertDispatch> findAllByOrderByDispatchedAtDesc(Pageable pageable);
}
