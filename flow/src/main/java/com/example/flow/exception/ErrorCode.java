package com.example.flow.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ErrorCode {

    QUEUE_ALREADY_REGISTER_USER(HttpStatus.CONFLICT,"uq-0001","Already registered in queue.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String reason;

    public ApplicationException build(){
        return new ApplicationException(httpStatus,code,reason);
    }
}
