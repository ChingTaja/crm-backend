package com.taja.crm.crm_backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class DeletionGuard {
    private final JdbcTemplate jdbc;
    private final EntityManager em;
    public void customer(String id) {
        check(id,"CUSTOMER_IN_USE","此客戶仍有聯絡人、商機、報價、訂單或 Lead 審核關聯，無法刪除。",
            "select exists(select 1 from contacts where customer_id=?) or exists(select 1 from opportunities where customer_id=?) or exists(select 1 from quote_versions where customer_id=?) or exists(select 1 from sales_orders where customer_id=?) or exists(select 1 from leads where qualification_customer_id=?)",5);
    }
    public void contact(String id) {
        check(id,"CONTACT_IN_USE","此聯絡人仍有商機或 Lead 審核關聯，無法刪除。",
            "select exists(select 1 from opportunities where contact_id=?) or exists(select 1 from leads where qualification_contact_id=?)",2);
    }
    public void lead(String id) {
        check(id,"LEAD_IN_USE","此 Lead 仍有關聯商機，無法刪除。",
            "select exists(select 1 from opportunities where lead_id=?)",1);
    }
    public void opportunity(String id) {
        check(id,"OPPORTUNITY_IN_USE","此商機仍有 Lead 審核或訂單關聯，無法刪除。",
            "select exists(select 1 from leads where qualification_opportunity_id=?) or exists(select 1 from sales_orders where opportunity_id=?)",2);
    }
    public void delete(String code, String detail, Runnable action) {
        try { action.run(); }
        catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            throw new QuoteException(HttpStatus.CONFLICT, code, detail);
        }
    }
    private void check(String id,String code,String detail,String sql,int parameters) {
        em.flush();
        Object[] values=new Object[parameters];java.util.Arrays.fill(values,id);
        if(Boolean.TRUE.equals(jdbc.queryForObject(sql,Boolean.class,values)))
            throw new QuoteException(HttpStatus.CONFLICT,code,detail);
    }
}
