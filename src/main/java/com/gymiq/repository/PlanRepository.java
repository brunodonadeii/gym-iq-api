package com.gymiq.repository;

import com.gymiq.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    Page<Plan> findByActiveTrue(Pageable pageable);

    Page<Plan> findByActiveFalse(Pageable pageable);

    @Query("""
            SELECT p
            FROM Plan p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<Plan> searchByTerm(@Param("term") String term, Pageable pageable);

    @Query("""
            SELECT p
            FROM Plan p
            WHERE p.active = :active
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%'))
              )
            """)
    Page<Plan> searchByTermAndActive(
            @Param("term") String term,
            @Param("active") Boolean active,
            Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
