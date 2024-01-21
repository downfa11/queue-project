package com.example.flow;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.redis.core.script.RedisScript;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedis {
    private final RedisServer server;

    public EmbeddedRedis() throws IOException{
        this.server = new RedisServer(63790);
    }

    @PostConstruct
    public void start() throws IOException{
        this.server.start();
    }

    @PreDestroy
    public void stop() throws IOException{
        this.server.stop();
    }
}
