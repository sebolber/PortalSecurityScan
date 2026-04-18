package com.ahs.cvm.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.persistence.alert.AlertDispatch;
import com.ahs.cvm.persistence.alert.AlertDispatchRepository;
import com.ahs.cvm.persistence.alert.AlertRule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertDispatcherTest {

    private final AlertTemplateRenderer renderer = new AlertTemplateRenderer();
    private final Instant t0 = Instant.parse("2026-04-18T11:00:00Z");
    private final Clock clock = Clock.fixed(t0, ZoneOffset.UTC);

    private AlertRule rule() {
        AlertRule r = AlertRule.builder()
                .name("KEV-Hit")
                .triggerArt(AlertTriggerArt.KEV_HIT)
                .severity(AlertSeverity.CRITICAL)
                .templateName("alert-kev-hit")
                .recipients(List.of("alerts@ahs.test"))
                .subjectPrefix("[CVM]")
                .cooldownMinutes(60)
                .enabled(Boolean.TRUE)
                .build();
        r.setId(UUID.randomUUID());
        return r;
    }

    private AlertContext ctx() {
        return new AlertContext(
                AlertTriggerArt.KEV_HIT, "CVE-2025-1|prod",
                AhsSeverity.CRITICAL,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "summary", t0,
                Map.of("cveKey", "CVE-2025-1", "produktVersion", "1.14.2-test"));
    }

    @Test
    @DisplayName("Dry-Run: Mail-Sender wird nicht gerufen, AlertDispatch dryRun=true")
    void dryRunRuftKeinenMailSender() {
        MailSenderPort sender = mock(MailSenderPort.class);
        AlertDispatchRepository repo = mock(AlertDispatchRepository.class);
        AlertConfig config = new AlertConfig("dry-run", 120, 360, "from@ahs.test");
        AlertDispatcher dispatcher = new AlertDispatcher(renderer, sender, repo, config, clock);

        dispatcher.dispatch(rule(), ctx());

        verify(sender, never()).send(anyList(), anyString(), anyString());
        ArgumentCaptor<AlertDispatch> captor = ArgumentCaptor.forClass(AlertDispatch.class);
        verify(repo, times(1)).save(captor.capture());
        AlertDispatch dispatch = captor.getValue();
        assertThat(dispatch.getDryRun()).isTrue();
        assertThat(dispatch.getSubject()).contains("KEV-Hit").contains("CVE-2025-1");
        assertThat(dispatch.getError()).isNull();
        assertThat(dispatch.getRecipients()).containsExactly("alerts@ahs.test");
    }

    @Test
    @DisplayName("Real-Modus: Mail-Sender wird gerufen, kein Fehler im Audit")
    void realModeRuftMailSender() {
        MailSenderPort sender = mock(MailSenderPort.class);
        AlertDispatchRepository repo = mock(AlertDispatchRepository.class);
        AlertConfig config = new AlertConfig("real", 120, 360, "from@ahs.test");
        AlertDispatcher dispatcher = new AlertDispatcher(renderer, sender, repo, config, clock);

        dispatcher.dispatch(rule(), ctx());

        verify(sender, times(1)).send(eq(List.of("alerts@ahs.test")), anyString(), anyString());
        ArgumentCaptor<AlertDispatch> captor = ArgumentCaptor.forClass(AlertDispatch.class);
        verify(repo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDryRun()).isFalse();
        assertThat(captor.getValue().getError()).isNull();
    }

    @Test
    @DisplayName("Mail-Sender wirft: Audit speichert Fehlermeldung und MailDispatchException wird weitergereicht")
    void senderFehlerWirdAudited() {
        MailSenderPort sender = mock(MailSenderPort.class);
        doThrow(new MailDispatchException("smtp down", null))
                .when(sender).send(anyList(), anyString(), anyString());
        AlertDispatchRepository repo = mock(AlertDispatchRepository.class);
        AlertConfig config = new AlertConfig("real", 120, 360, "from@ahs.test");
        AlertDispatcher dispatcher = new AlertDispatcher(renderer, sender, repo, config, clock);

        try {
            dispatcher.dispatch(rule(), ctx());
        } catch (MailDispatchException expected) {
            // erwartet.
        }

        ArgumentCaptor<AlertDispatch> captor = ArgumentCaptor.forClass(AlertDispatch.class);
        verify(repo, times(1)).save(captor.capture());
        assertThat(captor.getValue().getError()).contains("smtp down");
    }
}
