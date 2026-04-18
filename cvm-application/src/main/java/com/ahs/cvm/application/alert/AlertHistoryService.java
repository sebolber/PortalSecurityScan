package com.ahs.cvm.application.alert;

import com.ahs.cvm.persistence.alert.AlertDispatchRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-Service fuer die Alert-Historie (Iteration 27c, CVM-63).
 *
 * <p>Liefert die zuletzt verschickten Alert-Dispatches an das UI.
 * Paging absichtlich schlank: der Default-Wert von 50 reicht fuer
 * die Anzeige und vermeidet teure JSON-Deserialisierung.
 */
@Service
public class AlertHistoryService {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final AlertDispatchRepository repository;

    public AlertHistoryService(AlertDispatchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryView> recent(int limit) {
        int effective = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return repository
                .findAllByOrderByDispatchedAtDesc(PageRequest.of(0, effective))
                .stream()
                .map(AlertHistoryView::from)
                .toList();
    }
}
