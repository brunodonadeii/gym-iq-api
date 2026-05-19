package com.gymiq.repository;

import com.gymiq.entity.Student;
import com.gymiq.dto.response.StudentOptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query("""
            SELECT new com.gymiq.dto.response.StudentOptionResponse(
                s.studentId,
                u.name,
                u.email,
                s.cpf,
                CONCAT(u.name, ' - ', s.cpf)
            )
            FROM Student s
            JOIN s.user u
            WHERE u.active = true
              AND (
                    :term IS NULL
                    OR :term = ''
                    OR LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR s.cpf LIKE CONCAT('%', :term, '%')
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))
              )
            ORDER BY u.name ASC
            """)
    List<StudentOptionResponse> findOptions(@Param("term") String term, Pageable pageable);

}
