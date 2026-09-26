package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.repo.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Page<Customer> findAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Customer findByIdCustomer(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Customer：" + id));
    }

    @Transactional
    public Customer createCustomers(@NotNull @Valid Customer customer) {
        if (customer.getId() != null) {
            throw new IllegalArgumentException("新增 Customer 時不可指定 id");
        }
        return customerRepository.save(customer);
    }

    /** 完整更新指定 Customer，未提供的欄位會清空。 */
    @Transactional
    public Customer updateCustomers(String id, @NotNull @Valid Customer customer) {
        Customer existing = findByIdCustomer(id);
        existing.setName(customer.getName());
        existing.setCompany(customer.getCompany());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setOwner(customer.getOwner());
        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomers(String id) {
        Customer customer = findByIdCustomer(id);
        customerRepository.delete(customer);
    }
}
