package com.dogaa.backend.modules.scheduling;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.repository.ScheduledTaskRepository;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskServiceImplTest {

    @Mock
    private ScheduledTaskRepository repository;

    private ScheduledTaskServiceImpl service;

    private final UUID taskId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID someoneElse = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ScheduledTaskServiceImpl(repository);
    }

    private ScheduledTask taskWith(ScheduledTaskStatus status) {
        ScheduledTask task = new ScheduledTask();
        task.setStatus(status);
        task.setUserId(owner);
        return task;
    }

    @Test
    void pausingAnActiveTaskSetsItToPaused() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.ACTIVE)));
        when(repository.save(any(ScheduledTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.pause(owner, taskId).status()).isEqualTo(ScheduledTaskStatus.PAUSED);
    }

    @Test
    void resumingAPausedTaskSetsItBackToActive() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.PAUSED)));
        when(repository.save(any(ScheduledTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.resume(owner, taskId).status()).isEqualTo(ScheduledTaskStatus.ACTIVE);
    }

    @Test
    void resumingANonPausedTaskIsRejected() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.ACTIVE)));

        assertThatThrownBy(() -> service.resume(owner, taskId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellingACompletedTaskIsRejected() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.COMPLETED)));

        assertThatThrownBy(() -> service.cancel(owner, taskId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void actingOnAnUnknownTaskRaisesResourceNotFound() {
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pause(owner, taskId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- ownership --------------------------------------------------------

    /**
     * Someone else's task must be untouchable, and must look like it does not exist: answering
     * "forbidden" would confirm the id is real.
     */
    @Test
    void anotherUsersTaskCannotBePausedAndReadsAsMissing() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.ACTIVE)));

        assertThatThrownBy(() -> service.pause(someoneElse, taskId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(ScheduledTask.class));
    }

    @Test
    void anotherUsersTaskCannotBeResumed() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.PAUSED)));

        assertThatThrownBy(() -> service.resume(someoneElse, taskId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(ScheduledTask.class));
    }

    @Test
    void anotherUsersTaskCannotBeCancelled() {
        when(repository.findById(taskId)).thenReturn(Optional.of(taskWith(ScheduledTaskStatus.ACTIVE)));

        assertThatThrownBy(() -> service.cancel(someoneElse, taskId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(ScheduledTask.class));
    }
}
