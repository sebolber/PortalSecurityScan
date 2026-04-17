package com.ahs.cvm.application.assessment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssessmentExpiryJobTest {

    @Test
    @DisplayName("Scheduler ruft expireIfDue mit aktuellem Zeitpunkt")
    void schedulerDelegiert() {
        AssessmentWriteService writeService = mock(AssessmentWriteService.class);
        given(writeService.expireIfDue(any(Instant.class))).willReturn(3);

        new AssessmentExpiryJob(writeService).laufeTaeglich();

        verify(writeService, times(1)).expireIfDue(any(Instant.class));
    }
}
