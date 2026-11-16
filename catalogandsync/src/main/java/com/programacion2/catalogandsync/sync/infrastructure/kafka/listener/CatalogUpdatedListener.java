package com.programacion2.catalogandsync.sync.infrastructure.kafka.listener;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.application.service.SyncService;
import com.programacion2.catalogandsync.sync.domain.model.CatalogUpdatedNotification;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.ProcessedEventPort;
import com.programacion2.catalogandsync.sync.infrastructure.kafka.config.KafkaConsumerConfig;
import com.programacion2.catalogandsync.sync.infrastructure.kafka.mapper.CatalogUpdatedEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada del evento {@code CatalogUpdated} (R1 de
 * SYNC_RESPONSABILIDADES.md, hito 1.7).
 * <p>
 * Kafka <b>notifica</b>, no es la fuente del catalogo. El evento se valida y,
 * si es nuevo, dispara el mismo {@link SyncService#sync()} que usa el
 * scheduler: el planner lee las versiones reales de Redis y decide snapshot,
 * incremental o nada. El {@code newVersion} del evento no define el rango.
 * <p>
 * Politica de fallas (decidida 2026-11-13):
 * <ul>
 *   <li>contrato roto (JSON, {@code eventType}, {@code schemaVersion}, key
 *       distinta de la version) -> {@code ERROR} y ack: el evento no tiene
 *       efectos y el estado no depende de su payload;</li>
 *   <li>otro ciclo en curso -> {@code INFO} y ack: el ciclo que corre ya lee la
 *       version vigente y converge;</li>
 *   <li>falla del ciclo (transporte, contrato en Redis, persistencia) ->
 *       {@code ERROR} y ack: los adaptadores ya reintentan 3x/2 s; el ledger no
 *       avanza y el scheduler de 15 min recupera. Reintentar aca multiplicaria
 *       los intentos y bloquearia la particion;</li>
 *   <li>falla al registrar el {@code eventId} -> {@code WARN} y ack: un
 *       duplicado posterior correria un ciclo idempotente ({@code NOTHING}).</li>
 * </ul>
 * El ack va en {@code finally}: el offset solo se confirma cuando el listener
 * termino, y con {@code MANUAL_IMMEDIATE} la confirmacion es inmediata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sync.kafka", name = "enabled", havingValue = "true")
public class CatalogUpdatedListener {

    private final CatalogUpdatedEventMapper eventMapper;
    private final ProcessedEventPort processedEventPort;
    private final SyncService syncService;

    @KafkaListener(
            topics = "${integration.kafka-catalog-topic}",
            groupId = "${integration.kafka-consumer-group-id}",
            containerFactory = KafkaConsumerConfig.CATALOG_UPDATED_CONTAINER_FACTORY)
    public void onCatalogUpdated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String context = "CatalogUpdated partition=" + record.partition() + " offset=" + record.offset();
        long startedAt = System.currentTimeMillis();

        try {
            process(record, context, startedAt);
        } catch (CatedraContractException ex) {
            log.error("{} is invalid and will be discarded: {}", context, ex.getMessage());
            log.debug("CatalogUpdated contract violation detail", ex);
        } catch (RuntimeException ex) {
            log.error("{} could not be applied; the periodic reconciliation will retry: {}",
                    context, ex.getMessage());
            log.debug("CatalogUpdated failure detail", ex);
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private void process(ConsumerRecord<String, String> record, String context, long startedAt) {
        CatalogUpdatedNotification notification =
                eventMapper.toNotification(record.key(), record.value(), context);

        if (processedEventPort.exists(notification.eventId())) {
            log.debug("{} is a duplicate (eventId={}, newVersion={}); skipped",
                    context, notification.eventId(), notification.newVersion());
            return;
        }

        SyncOutcome outcome;
        try {
            outcome = syncService.sync();
        } catch (SyncAlreadyRunningException ex) {
            log.info("{} skipped: another sync cycle is running ({}); that cycle converges on its own",
                    context, ex.getMessage());
            return;
        }

        recordProcessedEvent(notification, outcome, context);
        log.info("{} applied: eventId={}, newVersion={}, source={}, mode={}, localVersion={}, duration={}ms",
                context, notification.eventId(), notification.newVersion(), notification.source(),
                outcome.mode(), outcome.appliedVersion(), System.currentTimeMillis() - startedAt);
    }

    private void recordProcessedEvent(CatalogUpdatedNotification notification, SyncOutcome outcome, String context) {
        try {
            processedEventPort.record(notification.eventId(), outcome.appliedVersion());
        } catch (RuntimeException ex) {
            log.warn("{} was applied but could not be recorded (eventId={}): {}. "
                            + "A duplicate would rerun an idempotent cycle.",
                    context, notification.eventId(), ex.getMessage());
        }
    }
}
