package com.gymiq.repository;

import com.gymiq.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    Optional<Instructor> findByCref(String cref);

    boolean existsByCref(String cref);

    Optional<Instructor> findByUserUserId(Integer userId);

    @Query("SELECT i FROM Instructor i JOIN i.user u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(i.cref) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "LOWER(i.specialty) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Instructor> searchByTerm(@Param("term") String term);
}
