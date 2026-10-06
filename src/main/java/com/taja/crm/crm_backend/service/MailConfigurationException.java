package com.taja.crm.crm_backend.service;
import org.springframework.mail.MailException;
public class MailConfigurationException extends MailException {
    public MailConfigurationException() {
        super("寄信設定未完成");
    }
}
