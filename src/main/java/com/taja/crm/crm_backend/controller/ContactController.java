package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.contact.CreateContactRequest;
import com.taja.crm.crm_backend.dto.contact.ContactResponse;
import com.taja.crm.crm_backend.dto.contact.UpdateContactRequest;
import com.taja.crm.crm_backend.model.Contact;
import com.taja.crm.crm_backend.service.ContactService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.Pagination;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public PageResponse<ContactResponse> findAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.fromPage(contactService.findAllContacts(Pagination.of(page, size)).map(ContactResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ContactResponse findByIdContact(@PathVariable String id) {
        return ContactResponse.fromEntity(contactService.findByIdContact(id));
    }

    @PostMapping
    public ResponseEntity<ContactResponse> createContacts(@Valid @RequestBody CreateContactRequest request) {
        Contact created = contactService.createContacts(request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactResponse.fromEntity(created));
    }

    /** 完整更新；未提供的欄位會清空。 */
    @PutMapping("/{id}")
    public ContactResponse updateContacts(@PathVariable String id, @Valid @RequestBody UpdateContactRequest request) {
        return ContactResponse.fromEntity(contactService.updateContacts(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContacts(@PathVariable String id) {
        contactService.deleteContacts(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
