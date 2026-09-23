package com.taja.crm.crm_backend.dto.customer;

import com.taja.crm.crm_backend.model.Customer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerRequest {
    private String name;
    private String company;
    private String email;
    private String phone;
    private String owner;

    public Customer toEntity() {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setCompany(company);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setOwner(owner);
        return customer;
    }
}
