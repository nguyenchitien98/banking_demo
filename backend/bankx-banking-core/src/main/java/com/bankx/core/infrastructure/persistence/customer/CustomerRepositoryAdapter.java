package com.bankx.core.infrastructure.persistence.customer;

import com.bankx.core.domain.customer.model.Customer;
import com.bankx.core.domain.customer.repository.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter triển khai Port Interface {@link CustomerRepository} trong Clean Architecture.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Repository}: Khai báo Spring Bean lưu trữ dữ liệu cho Customer Module.</li>
 * </ul>
 * </p>

 * @author BankX Engineering Team
 * @version 1.0
 */
@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final SpringDataCustomerRepository jpaRepository;

    public CustomerRepositoryAdapter(SpringDataCustomerRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Customer> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByCifNumber(String cifNumber) {
        return jpaRepository.findByCifNumber(cifNumber).map(this::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = toEntity(customer);
        CustomerJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        Customer customer = new Customer(
                entity.getId(),
                entity.getUserId(),
                entity.getCifNumber(),
                entity.getFullName(),
                entity.getIdentityNumber(),
                entity.getDateOfBirth(),
                entity.getAddress(),
                entity.getStatus()
        );
        customer.setCreatedAt(entity.getCreatedAt());
        customer.setUpdatedAt(entity.getUpdatedAt());
        return customer;
    }

    private CustomerJpaEntity toEntity(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.setId(customer.getId());
        entity.setUserId(customer.getUserId());
        entity.setCifNumber(customer.getCifNumber());
        entity.setFullName(customer.getFullName());
        entity.setIdentityNumber(customer.getIdentityNumber());
        entity.setDateOfBirth(customer.getDateOfBirth());
        entity.setAddress(customer.getAddress());
        entity.setStatus(customer.getStatus());
        return entity;
    }
}
