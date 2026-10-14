package com.taja.crm.crm_backend.dto.currency;

import com.taja.crm.crm_backend.model.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"code", "name", "symbol", "fractionDigits", "active"})
public record CurrencyResponse(String code, String name, String symbol, int fractionDigits, boolean active) {
    public static CurrencyResponse fromEntity(Currency currency) {
        return new CurrencyResponse(currency.getCode(), currency.getName(), currency.getSymbol(),
                currency.getFractionDigits(), currency.isActive());
    }
}
