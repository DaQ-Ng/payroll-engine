package com.payrollengine.repository;

import com.payrollengine.domain.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByActiveTrue();

    /** Pessimistic write lock so two concurrent pay runs can't both read the
     * same employee's pre-run YTD wages and race to apply conflicting deltas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findWithLockById(Long id);
}
