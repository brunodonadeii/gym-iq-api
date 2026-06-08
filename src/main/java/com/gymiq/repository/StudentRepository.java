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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Student> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Student> findByUserActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Student> findByUserActiveFalse(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Student> findById(UUID id);

    Optional<Student> findByCpfHash(String cpfHash);

    boolean existsByCpfHash(String cpfHash);

    Optional<Student> findByUserUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    Optional<Student> findByUserEmailHash(String emailHash);

    @EntityGraph(attributePaths = "user")
    Optional<Student> findByCpfHashOrUserEmailHash(String cpfHash, String emailHash);

    @Query("""
            SELECT COUNT(s)
            FROM Student s
            WHERE s.createdAt >= :startDate
              AND s.createdAt < :endDate
            """)
    long countCreatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT s
            FROM Student s
            JOIN s.user u
            WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR (:cpfHash IS NOT NULL AND s.cpfHash = :cpfHash)
               OR (:emailHash IS NOT NULL AND u.emailHash = :emailHash)
            """)
    @EntityGraph(attributePaths = "user")
    Page<Student> searchByTerm(
            @Param("term") String term,
            @Param("emailHash") String emailHash,
            @Param("cpfHash") String cpfHash,
            Pageable pageable);

    @Query("""
            SELECT s
            FROM Student s
            JOIN s.user u
            WHERE u.active = :active
              AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR (:cpfHash IS NOT NULL AND s.cpfHash = :cpfHash)
                    OR (:emailHash IS NOT NULL AND u.emailHash = :emailHash)
              )
            """)
    @EntityGraph(attributePaths = "user")
    Page<Student> searchByTermAndUserActive(
            @Param("term") String term,
            @Param("emailHash") String emailHash,
            @Param("cpfHash") String cpfHash,
            @Param("active") Boolean active,
            Pageable pageable);

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
                    OR (:cpfHash IS NOT NULL AND s.cpfHash = :cpfHash)
                    OR (:emailHash IS NOT NULL AND u.emailHash = :emailHash)
              )
            ORDER BY u.name ASC
            """)
    List<StudentOptionResponse> findOptions(
            @Param("term") String term,
            @Param("emailHash") String emailHash,
            @Param("cpfHash") String cpfHash,
            Pageable pageable);

}
