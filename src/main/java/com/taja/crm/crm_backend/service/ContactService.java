package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Contact;
import com.taja.crm.crm_backend.repo.ContactRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;

    public Page<Contact> findAllContacts(Pageable pageable) {
        return contactRepository.findAll(pageable);
    }

    public Contact findByIdContact(String id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Contact：" + id));
    }

    @Transactional
    public Contact createContacts(@NotNull @Valid Contact contact) {
        if (contact.getId() != null) {
            throw new IllegalArgumentException("新增 Contact 時不可指定 id");
        }
        return contactRepository.save(contact);
    }

    /** 完整更新指定 Contact，未提供的欄位會清空。 */
    @Transactional
    public Contact updateContacts(String id, @NotNull @Valid Contact contact) {
        Contact existing = findByIdContact(id);
        existing.setName(contact.getName());
        existing.setCompany(contact.getCompany());
        existing.setEmail(contact.getEmail());
        existing.setPhone(contact.getPhone());
        existing.setOwner(contact.getOwner());
        existing.setCustomerId(contact.getCustomerId());
        return contactRepository.save(existing);
    }

    @Transactional
    public void deleteContacts(String id) {
        Contact contact = findByIdContact(id);
        contactRepository.delete(contact);
    }
}
