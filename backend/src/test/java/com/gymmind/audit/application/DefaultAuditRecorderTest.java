package com.gymmind.audit.application;

import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAuditRecorderTest {

    @Test
    void actorAndTenantComeFromCurrentActorNotEvent() {
        OperationAuditRepository repository = mock(OperationAuditRepository.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        when(actors.requireCurrent()).thenReturn(actor(7L, 11L));
        DefaultAuditRecorder recorder = new DefaultAuditRecorder(repository, actors, mock(RejectedAuditWriter.class));

        recorder.record(new AuditEvent("USER_UPDATED", "USER", 99L, AuditResult.SUCCESS.name(), "trace-1", Map.of()));

        var captured = org.mockito.ArgumentCaptor.forClass(OperationAudit.class);
        verify(repository).save(captured.capture());
        assertEquals(7L, captured.getValue().getActorId());
        assertEquals(11L, captured.getValue().getTenantId());
        assertNotEquals(99L, captured.getValue().getActorId());
    }

    @Test
    void rejectedEventsUseIndependentWriter() {
        OperationAuditRepository repository = mock(OperationAuditRepository.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        RejectedAuditWriter rejected = mock(RejectedAuditWriter.class);
        when(actors.requireCurrent()).thenReturn(actor(7L, 11L));
        DefaultAuditRecorder recorder = new DefaultAuditRecorder(repository, actors, rejected);

        recorder.record(new AuditEvent("USER_UPDATED", "USER", 1L, AuditResult.REJECTED.name(), "trace-1", Map.of()));

        verify(rejected).record(any(OperationAudit.class));
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void noCurrentActorMeansRecordingFailsClosed() {
        OperationAuditRepository repository = mock(OperationAuditRepository.class);
        CurrentActorProvider actors = mock(CurrentActorProvider.class);
        when(actors.requireCurrent()).thenThrow(new IllegalStateException("no actor"));
        DefaultAuditRecorder recorder = new DefaultAuditRecorder(repository, actors, mock(RejectedAuditWriter.class));

        assertThrows(IllegalStateException.class, () -> recorder.record(
                new AuditEvent("USER_UPDATED", "USER", 1L, AuditResult.SUCCESS.name(), "trace-1", Map.of())));
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    private static CurrentActor actor(long userId, long tenantId) {
        return new CurrentActor(userId, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of("user:write"), 0L, "token");
    }
}
