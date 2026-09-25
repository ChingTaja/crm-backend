package com.taja.crm.crm_backend.dto.contact;

import com.taja.crm.crm_backend.model.Contact;

public record ContactResponse(
        String id,
        String name,
        String company,
        String email,
        String phone,
        String owner,
        String customerId) {

    public static ContactResponse fromEntity(Contact contact) {
        return new ContactResponse(contact.getId(), contact.getName(), contact.getCompany(),
                contact.getEmail(), contact.getPhone(), contact.getOwner(), contact.getCustomerId());
    }
}
