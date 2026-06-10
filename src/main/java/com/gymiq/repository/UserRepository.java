package com.gymiq.repository;

import com.gymiq.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);

    Page<User> findByRoleIn(Collection<User.Role> roles, Pageable pageable);

    Page<User> findByRole(User.Role role, Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.role IN :roles
              AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR (:emailHash IS NOT NULL AND u.emailHash = :emailHash)
              )
            """)
    Page<User> searchByRoleInAndTerm(
            @Param("roles") Collection<User.Role> roles,
            @Param("term") String term,
            @Param("emailHash") String emailHash,
            Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.role = :role
              AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR (:emailHash IS NOT NULL AND u.emailHash = :emailHash)
              )
            """)
    Page<User> searchByRoleAndTerm(
            @Param("role") User.Role role,
            @Param("term") String term,
            @Param("emailHash") String emailHash,
            Pageable pageable);
}
