package com.example.flow.controller;

import com.example.flow.EmbeddedRedis;
import com.example.flow.exception.ApplicationException;
import com.example.flow.service.UserQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class UserQueueContollerTest {
    @Autowired
    private UserQueueService service;
    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach //각 메소드를 실행하기 전에 호출
    public void beforeEach(){
        ReactiveRedisConnection connect =  reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        connect.serverCommands().flushAll().subscribe(); // init
    }

    @Test
    void registerUser() {
        StepVerifier.create(service.registerWaitQueue("default",100L))
                .expectNext(1L).verifyComplete();
        StepVerifier.create(service.registerWaitQueue("default",101L))
                .expectNext(2L).verifyComplete();
        StepVerifier.create(service.registerWaitQueue("default",102L))
                .expectNext(3L).verifyComplete();
    }

    @Test
    void alreadyRegisterWaitQueue(){
        StepVerifier.create(service.registerWaitQueue("default",100L))
                .expectNext(1L).verifyComplete();

        StepVerifier.create(service.registerWaitQueue("default",100L))
                .expectError(ApplicationException.class).verify();
    }
    @Test
    void emptyAllowUser() {
        StepVerifier.create(service.allowUser("default",10L))
                .expectNext(0L).verifyComplete();
    }

    @Test
    void allowUser() {
        StepVerifier.create(service.registerWaitQueue("default",10L)
                .then(service.registerWaitQueue("default",11L))
                        .then(service.registerWaitQueue("default",12L))
                        .then(service.allowUser("default",2L)))
                .expectNext(2L).verifyComplete();
    }

    @Test
    void isNotAllowedUser() {
        StepVerifier.create(service.isAllowed("default",10L))
                .expectNext(false).verifyComplete();
    }

    @Test
    void getRank() {
        StepVerifier.create(service.registerWaitQueue("defualt",100L)
                .then(service.getRank("default",100L)))
                .expectNext(1L).verifyComplete();

        StepVerifier.create(service.registerWaitQueue("defualt",101L)
                        .then(service.getRank("default",101L)))
                .expectNext(2L).verifyComplete();
    }

    @Test
    void emptyRank() {
        StepVerifier.create(service.getRank("default",100L))
                .expectNext(-1L).verifyComplete();

    }
}