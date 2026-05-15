package com.gymiq.repository;

import com.gymiq.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Exercise> findByNameIgnoreCase(String name);

    Page<Exercise> findByActiveTrue(Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE " +
            "LOWER(e.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(e.muscleGroup) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<Exercise> searchByTerm(@Param("term") String term, Pageable pageable);
}
