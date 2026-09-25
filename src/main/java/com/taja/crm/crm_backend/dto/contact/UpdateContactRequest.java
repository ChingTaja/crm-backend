package com.taja.crm.crm_backend.dto.contact;

import com.taja.crm.crm_backend.model.Contact;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateContactRequest {
    private String name;
    private String company;
    private String email;
    private String phone;
    private String owner;
    private String customerId;

    public Contact toEntity() {
        Contact contact = new Contact();
        contact.setName(name);
        contact.setCompany(company);
        contact.setEmail(email);
        contact.setPhone(phone);
        contact.setOwner(owner);
        contact.setCustomerId(customerId);
        return contact;
    }
}
