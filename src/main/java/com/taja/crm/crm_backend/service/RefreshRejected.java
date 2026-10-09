package com.taja.crm.crm_backend.service;
import org.springframework.http.HttpStatus;
public class RefreshRejected extends QuoteException {
    public RefreshRejected(String code, String detail) { super(HttpStatus.UNAUTHORIZED, code, detail); }
}
