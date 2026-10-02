package com.taja.crm.crm_backend.service;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
public class QuoteException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public QuoteException(HttpStatus status, String code, String detail) { super(detail); this.status = status; this.code = code; }
}
