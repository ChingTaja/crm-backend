package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.repo.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Customer：" + id));
    }

    @Transactional
    public Customer create(@NotNull @Valid Customer customer) {
        if (customer.getId() != null) {
            throw new IllegalArgumentException("新增 Customer 時不可指定 id");
        }
        return customerRepository.save(customer);
    }

    /** 完整更新指定 Customer，未提供的欄位會清空。 */
    @Transactional
    public Customer update(String id, @NotNull @Valid Customer customer) {
        Customer existing = findById(id);
        existing.setName(customer.getName());
        existing.setCompany(customer.getCompany());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setOwner(customer.getOwner());
        return customerRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        Customer customer = findById(id);
        customerRepository.delete(customer);
    }
}
