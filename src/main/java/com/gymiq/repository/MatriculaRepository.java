package com.gymiq.repository;

import com.gymiq.entity.Matricula;
import com.gymiq.entity.Matricula.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

    List<Matricula> findByAlunoIdAluno(Integer idAluno);

    List<Matricula> findByStatus(StatusMatricula status);

    Optional<Matricula> findByAlunoIdAlunoAndStatus(Integer idAluno, StatusMatricula status);

    boolean existsByAlunoIdAlunoAndStatus(Integer idAluno, StatusMatricula status);

    /** Matrículas que vencem nos próximos N dias (para alertas de renovação). */
    @Query("SELECT m FROM Matricula m WHERE m.status = 'ATIVO' " +
           "AND m.dataFim BETWEEN :hoje AND :limite")
    List<Matricula> findVencendoEm(@Param("hoje") LocalDate hoje,
                                   @Param("limite") LocalDate limite);
}
