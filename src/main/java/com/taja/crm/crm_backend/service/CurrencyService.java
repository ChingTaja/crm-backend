package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.currency.*;
import com.taja.crm.crm_backend.model.Currency;
import com.taja.crm.crm_backend.repo.CurrencyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CurrencyService {
    private final CurrencyRepository currencies;
    private final PermissionService access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public List<CurrencyResponse> findAll(String actorId) {
        access.require(actorId, "currencies.read");
        return currencies.findAll(Sort.by("code")).stream().map(CurrencyResponse::fromEntity).toList();
    }

    public CurrencyResponse findByCode(String actor, String code) {
        access.require(actor, "currencies.read");
        return CurrencyResponse.fromEntity(find(code));
    }

    @Transactional
    public CurrencyResponse create(String actor, CreateCurrencyRequest request) {
        access.require(actor, "currencies.create");
        // Atomic insert also handles simultaneous requests for the same code.
        int inserted = jdbc.update("insert into currencies(code,name,symbol,fraction_digits,active) values (?,?,?,?,?) on conflict (code) do nothing",
                request.code(), request.name().strip(), request.symbol().strip(), request.fractionDigits(), request.active());
        if (inserted == 0)
            throw new QuoteException(HttpStatus.CONFLICT, "CURRENCY_CODE_EXISTS", "幣別代碼已存在。");
        return CurrencyResponse.fromEntity(find(request.code()));
    }

    @Transactional
    public CurrencyResponse update(String actor, String code, UpdateCurrencyRequest request) {
        access.require(actor, "currencies.update");
        Currency currency = find(code);
        if (request.code() != null && !code.equals(request.code()))
            throw new QuoteException(HttpStatus.BAD_REQUEST, "CURRENCY_CODE_IMMUTABLE", "幣別代碼不可修改。");
        currency.setName(request.name().strip());
        currency.setSymbol(request.symbol().strip());
        currency.setActive(request.active());
        return CurrencyResponse.fromEntity(currencies.saveAndFlush(currency));
    }

    @Transactional
    public void delete(String actor, String code) {
        access.require(actor, "currencies.delete");
        Currency currency = find(code);
        currencies.flush();
        boolean referenced = Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from products where currency=?) or exists(select 1 from quote_versions where currency=?) or exists(select 1 from sales_orders where currency=?)",
                Boolean.class, code, code, code));
        if (referenced) throw inUse();
        try {
            currencies.delete(currency);
            currencies.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            // Foreign keys protect against a reference created after the check.
            throw inUse();
        }
    }

    private Currency find(String code) {
        return currencies.findById(code).orElseThrow(() ->
                new QuoteException(HttpStatus.NOT_FOUND, "CURRENCY_NOT_FOUND", "找不到幣別：" + code));
    }

    private QuoteException inUse() {
        return new QuoteException(HttpStatus.CONFLICT, "CURRENCY_IN_USE", "幣別已被商品或交易引用，不可刪除，請改以停用處理。");
    }

    public Currency require(String code, boolean requireActive) {
        if (code == null || !code.matches("[A-Z]{3}"))
            throw invalid("INVALID_CURRENCY", "請指定有效的三碼大寫幣別。");
        Currency currency = currencies.findById(code)
                .orElseThrow(() -> invalid("INVALID_CURRENCY", "幣別不存在：" + code));
        if (currency.getFractionDigits() != 2)
            throw invalid("UNSUPPORTED_CURRENCY_PRECISION", "目前僅支援兩位小數幣別。");
        if (requireActive && !currency.isActive())
            throw invalid("CURRENCY_INACTIVE", "此幣別已停用，不可用於新商品或新交易。");
        return currency;
    }

    private QuoteException invalid(String code, String detail) {
        return new QuoteException(HttpStatus.valueOf(422), code, detail);
    }
}
