package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.CustomerHistoryResponse;
import com.pms.pmsbackend.dto.CustomerInsuranceRequest;
import com.pms.pmsbackend.entity.Customer;
import com.pms.pmsbackend.entity.InsuranceProvider;
import com.pms.pmsbackend.repository.CustomerRepository;
import com.pms.pmsbackend.repository.InsuranceProviderRepository;
import com.pms.pmsbackend.service.PrescriptionService;
import com.pms.pmsbackend.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PrescriptionService prescriptionService;
    private final SaleService saleService;

    @GetMapping
    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@Valid @RequestBody Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Integer id, @Valid @RequestBody Customer customer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        existing.setName(customer.getName());
        existing.setPhone(customer.getPhone());
        existing.setEmail(customer.getEmail());
        existing.setAddress(customer.getAddress());
        existing.setDateOfBirth(customer.getDateOfBirth());
        existing.setGender(customer.getGender());
        existing.setAllergies(customer.getAllergies());
        existing.setMedicalNotes(customer.getMedicalNotes());
        return customerRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    // Dedicated endpoint for setting/clearing a customer's insurance info,
    // kept separate from the general update() above so that saving basic
    // contact details never accidentally touches coverage data (and vice versa).
    @PatchMapping("/{id}/insurance")
    public Customer updateInsurance(@PathVariable Integer id, @RequestBody CustomerInsuranceRequest request) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));

        if (request.getInsuranceProviderId() == null) {
            existing.setInsuranceProvider(null);
            existing.setInsurancePolicyNumber(null);
            existing.setInsuranceCoveragePct(null);
        } else {
            InsuranceProvider provider = insuranceProviderRepository.findById(request.getInsuranceProviderId())
                    .orElseThrow(() -> new EntityNotFoundException("Insurance provider not found: " + request.getInsuranceProviderId()));
            existing.setInsuranceProvider(provider);
            existing.setInsurancePolicyNumber(request.getInsurancePolicyNumber());
            existing.setInsuranceCoveragePct(request.getInsuranceCoveragePct());
        }

        return customerRepository.save(existing);
    }

    // Patient Management's "medication history" view -- pulls this
    // customer's prescriptions and dispensed sales together read-only.
    @GetMapping("/{id}/history")
    public CustomerHistoryResponse getHistory(@PathVariable Integer id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found: " + id);
        }
        CustomerHistoryResponse response = new CustomerHistoryResponse();
        response.setPrescriptions(prescriptionService.findByCustomer(id));
        response.setSales(saleService.findByCustomer(id));
        return response;
    }
}