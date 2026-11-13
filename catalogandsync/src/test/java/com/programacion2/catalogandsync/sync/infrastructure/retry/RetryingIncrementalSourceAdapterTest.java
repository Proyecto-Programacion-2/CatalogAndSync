package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceException;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;
import com.programacion2.catalogandsync.sync.domain.ports.out.IncrementalSourcePort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryingIncrementalSourceAdapterTest {

    @Test
    void retriesTransportFailures() {
        IncrementalSourcePort delegate = mock(IncrementalSourcePort.class);
        IncrementalBatch batch = new IncrementalBatch(7, List.of(), List.of(), List.of());
        when(delegate.read(7))
                .thenThrow(new IncrementalSourceUnavailableException("redis down", new RuntimeException()))
                .thenReturn(Optional.of(batch));

        assertEquals(Optional.of(batch), adapter(delegate).read(7));
        verify(delegate, times(2)).read(7);
    }

    @Test
    void doesNotRetryContractFailures() {
        IncrementalSourcePort delegate = mock(IncrementalSourcePort.class);
        when(delegate.read(7)).thenThrow(new IncrementalSourceException("broken changes payload"));

        assertThrows(IncrementalSourceException.class, () -> adapter(delegate).read(7));
        verify(delegate, times(1)).read(7);
    }

    @Test
    void doesNotRetryWhenTheHistoryIsMissing() {
        IncrementalSourcePort delegate = mock(IncrementalSourcePort.class);
        when(delegate.read(7)).thenReturn(Optional.empty());

        assertTrue(adapter(delegate).read(7).isEmpty());
        verify(delegate, times(1)).read(7);
    }

    @Test
    void givesUpAfterTheConfiguredAttempts() {
        IncrementalSourcePort delegate = mock(IncrementalSourcePort.class);
        when(delegate.read(7))
                .thenThrow(new IncrementalSourceUnavailableException("redis down", new RuntimeException()));

        assertThrows(IncrementalSourceUnavailableException.class, () -> adapter(delegate).read(7));
        verify(delegate, times(3)).read(7);
    }

    private static RetryingIncrementalSourceAdapter adapter(IncrementalSourcePort delegate) {
        return new RetryingIncrementalSourceAdapter(delegate, new FixedRetryExecutor(3, Duration.ZERO));
    }
}
