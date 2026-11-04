package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.VersionSourceException;
import com.programacion2.catalogandsync.sync.application.exception.VersionSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.ports.out.RemoteVersionPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryingRemoteVersionAdapterTest {

    @Test
    void retriesTransportFailuresOnCurrentVersion() {
        RemoteVersionPort delegate = mock(RemoteVersionPort.class);
        when(delegate.currentVersion())
                .thenThrow(new VersionSourceUnavailableException("redis down", new RuntimeException()))
                .thenReturn(5L);

        assertEquals(5L, adapter(delegate).currentVersion());
        verify(delegate, times(2)).currentVersion();
    }

    @Test
    void retriesTransportFailuresOnOldestAvailableVersion() {
        RemoteVersionPort delegate = mock(RemoteVersionPort.class);
        when(delegate.oldestAvailableVersion())
                .thenThrow(new VersionSourceUnavailableException("redis down", new RuntimeException()))
                .thenReturn(3L);

        assertEquals(3L, adapter(delegate).oldestAvailableVersion());
        verify(delegate, times(2)).oldestAvailableVersion();
    }

    @Test
    void doesNotRetryAnInvalidVersionValue() {
        RemoteVersionPort delegate = mock(RemoteVersionPort.class);
        when(delegate.currentVersion())
                .thenThrow(new VersionSourceException("Catedra Redis key is missing"));

        assertThrows(VersionSourceException.class, () -> adapter(delegate).currentVersion());
        verify(delegate, times(1)).currentVersion();
    }

    @Test
    void givesUpAfterTheConfiguredAttempts() {
        RemoteVersionPort delegate = mock(RemoteVersionPort.class);
        when(delegate.currentVersion())
                .thenThrow(new VersionSourceUnavailableException("redis down", new RuntimeException()));

        assertThrows(VersionSourceUnavailableException.class, () -> adapter(delegate).currentVersion());
        verify(delegate, times(3)).currentVersion();
    }

    private static RetryingRemoteVersionAdapter adapter(RemoteVersionPort delegate) {
        return new RetryingRemoteVersionAdapter(delegate, new FixedRetryExecutor(3, Duration.ZERO));
    }
}
