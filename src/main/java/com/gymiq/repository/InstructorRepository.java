package com.gymiq.repository;

import com.gymiq.entity.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    Optional<Instructor> findByCref(String cref);

    boolean existsByCref(String cref);

    Optional<Instructor> findByUserUserId(Integer userId);

    @EntityGraph(attributePaths = "user")
    Optional<Instructor> findByUserEmailIgnoreCase(String email);

    @Query("SELECT i FROM Instructor i JOIN i.user u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(i.cref) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(i.specialty) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<Instructor> searchByTerm(@Param("term") String term, Pageable pageable);
}
