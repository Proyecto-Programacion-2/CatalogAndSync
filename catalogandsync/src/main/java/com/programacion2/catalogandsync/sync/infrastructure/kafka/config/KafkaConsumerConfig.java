package com.programacion2.catalogandsync.sync.infrastructure.kafka.config;

import com.programacion2.catalogandsync.sync.infrastructure.config.IntegrationProperties;
import com.programacion2.catalogandsync.sync.infrastructure.config.SyncProperties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumidor Kafka dedicado al topico del catalogo de la catedra (hito 1.7).
 * <p>
 * Se configura desde el bloque {@code integration} (bootstrap, group y topico)
 * y {@code sync.kafka} (tuning), no desde {@code spring.kafka.*}: una sola
 * fuente de verdad, igual que el {@code catedraRedisTemplate} dedicado. El
 * broker real no pide SASL ni TLS (verificado 2026-11-13), pero si algun dia lo
 * hiciera, el cambio queda en este unico punto.
 * <p>
 * Deserializadores {@code String}: el evento se valida en
 * {@code CatalogUpdatedEventMapper}, con el mismo {@code JsonMapper} que usa la
 * hidratacion de Redis. {@code enable.auto.commit=false} + ack manual
 * inmediato: el offset se confirma solo despues de que el listener termino de
 * persistir (IR seccion 16).
 */
@Configuration
@ConditionalOnProperty(prefix = "sync.kafka", name = "enabled", havingValue = "true")
public class KafkaConsumerConfig {

    public static final String CATEDRA_CONSUMER_FACTORY = "catedraCatalogConsumerFactory";
    public static final String CATALOG_UPDATED_CONTAINER_FACTORY = "catalogUpdatedListenerContainerFactory";

    @Bean(CATEDRA_CONSUMER_FACTORY)
    public ConsumerFactory<String, String> catedraCatalogConsumerFactory(
            IntegrationProperties integration, SyncProperties sync) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, integration.kafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, integration.kafkaConsumerGroupId());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "catalogandsync-catalog-updated");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, sync.kafka().autoOffsetReset());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, sync.kafka().maxPollRecords());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, sync.kafka().maxPollIntervalMs());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(CATALOG_UPDATED_CONTAINER_FACTORY)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> catalogUpdatedListenerContainerFactory(
            @Qualifier(CATEDRA_CONSUMER_FACTORY) ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // El listener atrapa sus propias fallas y hace ack: el error handler
        // solo cubre lo imprevisto y no reintenta (la reconciliacion periodica
        // es la red de recuperacion, no el container).
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
        return factory;
    }
}
