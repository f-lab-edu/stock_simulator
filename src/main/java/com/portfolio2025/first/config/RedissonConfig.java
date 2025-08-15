package com.portfolio2025.first.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class RedissonConfig {
    @Value("${spring.data.redis.host}") String host;
    @Value("${spring.data.redis.port}") int port;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config cfg = new Config();
        cfg.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(cfg);
    }
}
