package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.Doctor;
import com.pms.pmsbackend.repository.DoctorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @GetMapping
    public List<Doctor> getAll() {
        return doctorRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Doctor create(@Valid @RequestBody Doctor doctor) {
        doctor.setId(null);
        return doctorRepository.save(doctor);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    @PutMapping("/{id}")
    public Doctor update(@PathVariable Integer id, @Valid @RequestBody Doctor doctor) {
        Doctor existing = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found: " + id));
        existing.setName(doctor.getName());
        existing.setClinicName(doctor.getClinicName());
        existing.setPhone(doctor.getPhone());
        existing.setEmail(doctor.getEmail());
        return doctorRepository.save(existing);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!doctorRepository.existsById(id)) {
            throw new EntityNotFoundException("Doctor not found: " + id);
        }
        doctorRepository.deleteById(id);
    }
}
