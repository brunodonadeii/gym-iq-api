package com.gymiq.repository;

import com.gymiq.entity.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, Integer> {

    List<Plano> findByAtivoTrue();

    boolean existsByNomeIgnoreCase(String nome);
}
