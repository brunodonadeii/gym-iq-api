package com.gymiq.repository;

import com.gymiq.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    Optional<Student> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    Optional<Student> findByUserUserId(Integer userId);

    @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
            "s.cpf LIKE CONCAT('%', :term, '%') OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Student> searchByTerm(@Param("term") String term);
}
