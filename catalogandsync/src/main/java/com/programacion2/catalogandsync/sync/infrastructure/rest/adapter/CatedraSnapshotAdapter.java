package com.programacion2.catalogandsync.sync.infrastructure.rest.adapter;

import com.programacion2.catalogandsync.sync.application.exception.SnapshotFetchException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import com.programacion2.catalogandsync.sync.infrastructure.rest.config.CatedraProperties;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.ProblemResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.SnapshotResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.mapper.SnapshotMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;

/**
 * Adaptador real de {@link SnapshotSourcePort} contra el servicio de la catedra.
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>armar el request con JWT tecnico y timeouts;</li>
 *   <li>traducir errores de transporte y respuestas no exitosas a
 *       {@link SnapshotFetchException};</li>
 *   <li>delegar el mapeo del contrato en {@link SnapshotMapper}.</li>
 * </ul>
 * No reintenta: la robustez de H1.9 (3 intentos fijos) se agrega despues como
 * una capa aparte, sin tocar este adaptador.
 */
@Component
public class CatedraSnapshotAdapter implements SnapshotSourcePort {

    private static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

    private final RestClient restClient;
    private final SnapshotMapper mapper;

    public CatedraSnapshotAdapter(CatedraProperties properties, SnapshotMapper mapper) {
        this.mapper = mapper;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(stripTrailingSlashes(properties.baseUrl()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.jwt())
                .build();
    }

    @Override
    public Snapshot fetchSnapshot() {
        try {
            SnapshotResponse response = restClient.get()
                    .uri(SNAPSHOT_PATH)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SnapshotResponse.class);
            return mapper.toDomain(response);
        } catch (RestClientResponseException ex) {
            String code = extractCode(ex);
            String message = "Catedra snapshot request failed with HTTP " + ex.getStatusCode().value()
                    + (code != null ? " (" + code + ")" : "");
            throw new SnapshotFetchException(message, ex, ex.getStatusCode().value(), code);
        } catch (ResourceAccessException ex) {
            throw new SnapshotFetchException(
                    "Catedra snapshot request could not reach the server (timeout or connection error)",
                    ex, null, null);
        } catch (RestClientException ex) {
            throw new SnapshotFetchException("Catedra snapshot request failed", ex, null, null);
        }
    }

    private String extractCode(RestClientResponseException ex) {
        try {
            ProblemResponse problem = ex.getResponseBodyAs(ProblemResponse.class);
            return problem != null ? problem.code() : null;
        } catch (RuntimeException ignored) {
            // El body de error no siempre es problem+json (401/403 de Spring Security).
            return null;
        }
    }

    private static String stripTrailingSlashes(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
