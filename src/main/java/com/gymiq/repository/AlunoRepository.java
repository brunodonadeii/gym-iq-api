package com.gymiq.repository;

import com.gymiq.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Integer> {

    Optional<Aluno> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    Optional<Aluno> findByUsuarioIdUsuario(Integer idUsuario);

    @Query("SELECT a FROM Aluno a JOIN a.usuario u WHERE " +
           "LOWER(u.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "a.cpf LIKE CONCAT('%', :termo, '%') OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Aluno> buscarPorTermo(@Param("termo") String termo);
}
