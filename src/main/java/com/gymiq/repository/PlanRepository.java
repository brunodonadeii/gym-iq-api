package com.gymiq.repository;

import com.gymiq.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    List<Plan> findByActiveTrue();

    boolean existsByNameIgnoreCase(String name);
}