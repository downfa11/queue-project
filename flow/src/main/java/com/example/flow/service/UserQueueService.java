package com.example.flow.service;

import com.example.flow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final String WAIT_KEY ="users:queue:%s:wait";
    private final String WAIT_KEY_FOR_SCAN ="users:queue:*:wait";
    private final String PROCEED_KEY ="users:queue:%s:proceed";

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    public Mono<Long> registerWaitQueue(final String queue, final Long userId){
        // redis SortedSet {userId,timestamp}
        var unixTimestamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(WAIT_KEY.formatted(queue),userId.toString(),unixTimestamp)
                .filter(i -> i) // 성공하면 flatMap+1 반환
                .switchIfEmpty(Mono.error(ErrorCode.QUEUE_ALREADY_REGISTER_USER.build()))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(WAIT_KEY.formatted(queue),userId.toString()))
                .map(i -> i>=0 ? i+1 :i); // index 0 -> i++

    }

    public Mono<Long> allowUser(final String queue, final Long count){

        return reactiveRedisTemplate.opsForZSet().popMin(WAIT_KEY.formatted(queue),count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet()
                        .add(PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
                .count();
    }

    public Mono<Boolean> isAllowed(final String queue,final Long userId){
        return reactiveRedisTemplate.opsForZSet().rank(PROCEED_KEY.formatted(queue),userId.toString())
                .defaultIfEmpty(-1L).map(rank -> rank >=0);
        // empty이면 -1, rank가 -1보다 작으면 false
    }

    public Mono<Boolean> isAllowedByToken(final String queue,final Long userId,final String token){

        return this.generateToken(queue,userId)
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Long> getRank(final String queue,final Long userId){
        return reactiveRedisTemplate.opsForZSet().rank(PROCEED_KEY.formatted(queue),userId.toString())
                .defaultIfEmpty(-1L).map(rank -> rank>=0 ? rank+1 :rank);
        // empty이면 -1, rank가 -1보다 작으면 false
    }

    public Mono<String> generateToken(final String queue,final Long userId){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");

        var input = "user-queue-%s-%d".formatted(queue,userId);

        byte[] encodeHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for(byte b : encodeHash){
            hexString.append(String.format("%02x",b));
        }
        return Mono.just(hexString.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    public void scheduleAllowUser(){

        if(!scheduling){
             log.info("passed scheduling..");
             return;
        }
        var maxAllowUserCount =3L;
        log.info("called scheduling..");

        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(WAIT_KEY_FOR_SCAN)
                .count(100).build())
                .map(key -> key.split(":")[2])
                .flatMap(queue -> allowUser(queue,maxAllowUserCount).map(allowed-> Tuples.of(queue,allowed)))
                .doOnNext(tuple -> log.info("Tried %d, allowed %d, members of %s queue".formatted(maxAllowUserCount,tuple.getT2(),tuple.getT1())))
                .subscribe();
    }
}
