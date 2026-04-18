package com.ahs.cvm.application.modelprofile;

import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-Service fuer alle LLM-Modellprofile (Iteration 25, CVM-56).
 * Dient der Admin-UI als Dropdown-Quelle.
 */
@Service
public class ModelProfileQueryService {

    private final LlmModelProfileRepository repository;

    public ModelProfileQueryService(LlmModelProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ModelProfileView> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getProfileKey()
                        .compareToIgnoreCase(b.getProfileKey()))
                .map(ModelProfileView::from)
                .toList();
    }
}
