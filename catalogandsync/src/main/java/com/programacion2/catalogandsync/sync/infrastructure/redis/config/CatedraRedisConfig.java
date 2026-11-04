package com.programacion2.catalogandsync.sync.infrastructure.redis.config;

import com.programacion2.catalogandsync.sync.infrastructure.config.IntegrationProperties;
import com.programacion2.catalogandsync.sync.infrastructure.config.SyncProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Cliente Redis dedicado al namespace de la catedra (IR §14).
 * <p>
 * Se define un {@link LettuceConnectionFactory} propio para no mezclar esta
 * conexion con un Redis local futuro: distinto host, usuario ACL y timeouts.
 * La conexion es perezosa: crear los beans no abre sockets.
 */
@Configuration
public class CatedraRedisConfig {

    public static final String CATEDRA_REDIS_TEMPLATE = "catedraRedisTemplate";

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory catedraRedisConnectionFactory(
            IntegrationProperties integration,
            SyncProperties syncProperties) {

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                integration.redisHost(), integration.redisPort());
        if (integration.redisUsername() != null && !integration.redisUsername().isBlank()) {
            standalone.setUsername(integration.redisUsername());
        }
        standalone.setPassword(RedisPassword.of(integration.redisPassword()));

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(syncProperties.redis().connectTimeout())
                .build();
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(syncProperties.redis().commandTimeout())
                .clientOptions(ClientOptions.builder().socketOptions(socketOptions).build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfiguration);
        factory.setEagerInitialization(false);
        return factory;
    }

    @Bean(CATEDRA_REDIS_TEMPLATE)
    public StringRedisTemplate catedraRedisTemplate(LettuceConnectionFactory catedraRedisConnectionFactory) {
        return new StringRedisTemplate(catedraRedisConnectionFactory);
    }
}
