package com.taja.crm.crm_backend.dto.customer;

import com.taja.crm.crm_backend.model.Customer;

public record CustomerResponse(
        String id,
        String name,
        String company,
        String email,
        String phone,
        String owner) {

    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getCompany(),
                customer.getEmail(), customer.getPhone(), customer.getOwner());
    }
}
