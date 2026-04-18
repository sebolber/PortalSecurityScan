package com.ahs.cvm.application.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.environment.EnvironmentQueryService.CreateEnvironmentCommand;
import com.ahs.cvm.application.environment.EnvironmentQueryService.EnvironmentKeyAlreadyExistsException;
import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnvironmentQueryServiceTest {

    private EnvironmentRepository repo;
    private EnvironmentQueryService service;

    @BeforeEach
    void setUp() {
        repo = mock(EnvironmentRepository.class);
        given(repo.save(any(Environment.class))).willAnswer(inv -> {
            Environment e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            return e;
        });
        service = new EnvironmentQueryService(repo);
    }

    @Test
    @DisplayName("create: gueltige Eingabe persistiert und liefert View")
    void createOk() {
        given(repo.findByKey("CI")).willReturn(Optional.empty());

        EnvironmentView view = service.create(new CreateEnvironmentCommand(
                "CI", "Continuous Integration", EnvironmentStage.DEV, "default"));

        assertThat(view.key()).isEqualTo("CI");
        assertThat(view.stage()).isEqualTo(EnvironmentStage.DEV);
        assertThat(view.tenant()).isEqualTo("default");
        assertThat(view.id()).isNotNull();
    }

    @Test
    @DisplayName("create: doppelter key fuehrt zur Ablehnung")
    void duplicate() {
        given(repo.findByKey("CI"))
                .willReturn(Optional.of(Environment.builder().key("CI").build()));

        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "CI", "CI", EnvironmentStage.DEV, null)))
                .isInstanceOf(EnvironmentKeyAlreadyExistsException.class);
    }

    @Test
    @DisplayName("create: leerer key fuehrt zu IllegalArgumentException")
    void leererKey() {
        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "  ", "Name", EnvironmentStage.DEV, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    @Test
    @DisplayName("create: fehlende stage fuehrt zu IllegalArgumentException")
    void fehlendeStage() {
        assertThatThrownBy(() -> service.create(new CreateEnvironmentCommand(
                        "CI", "Name", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stage");
    }

    @Test
    @DisplayName("create: leerer tenant wird zu null")
    void leererTenant() {
        given(repo.findByKey("CI")).willReturn(Optional.empty());
        EnvironmentView view = service.create(new CreateEnvironmentCommand(
                "CI", "CI", EnvironmentStage.DEV, "  "));
        assertThat(view.tenant()).isNull();
    }
}
