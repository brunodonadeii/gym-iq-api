package com.gymiq.repository;

import com.gymiq.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Student> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Student> findById(Integer id);

    Optional<Student> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    Optional<Student> findByUserUserId(Integer userId);

    @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "s.cpf LIKE CONCAT('%', :term, '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))")
    @EntityGraph(attributePaths = "user")
    Page<Student> searchByTerm(@Param("term") String term, Pageable pageable);

}
