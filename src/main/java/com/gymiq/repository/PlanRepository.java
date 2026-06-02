package com.gymiq.repository;

import com.gymiq.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    Page<Plan> findByActiveTrue(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
