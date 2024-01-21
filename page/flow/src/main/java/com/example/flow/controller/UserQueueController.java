package com.example.flow.controller;

import com.example.flow.dto.AllowUserResponse;
import com.example.flow.dto.AllowedUserResponse;
import com.example.flow.dto.RankNumberResponse;
import com.example.flow.dto.RegisterUserResponse;
import com.example.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {

    private final UserQueueService service;

    @PostMapping
    public Mono<RegisterUserResponse> registerUser(@RequestParam(name="queue",defaultValue = "default") String queue, @RequestParam(name="user_id") Long userId){
        return service.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    @PostMapping("/allow")
    public Mono<?> allowUser(@RequestParam(name="queue",defaultValue = "default") String queue,
                             @RequestParam(name="count") Long count){
        return service.allowUser(queue,count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }

    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(@RequestParam(name="queue",defaultValue = "default") String queue,
                                       @RequestParam(name="user_id") Long userId,
                                       @RequestParam(name="token") String token){
        return service.isAllowedByToken(queue,userId,token)
                .map(AllowedUserResponse::new);
    }

    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRankUser(@RequestParam(name="queue",defaultValue = "default") String queue,
                                                @RequestParam(name="user_id") Long userId){
        return service.getRank(queue,userId)
                .map(RankNumberResponse::new);
    }

    @GetMapping("/touch")
    Mono<?> touch(@RequestParam(name="queue",defaultValue = "default") String queue,
                  @RequestParam(name="user_id") Long userId,
                  ServerWebExchange exchange) {
        return Mono.defer(() -> service.generateToken(queue, userId))
                .map(token -> {
                    exchange.getResponse().addCookie(
                            ResponseCookie
                                    .from("user-queue-%s-token".formatted(queue), token)
                                    .maxAge(Duration.ofSeconds(300))
                                    .path("/")
                                    .build()
                    );
                    return token;
                });
    }
}
