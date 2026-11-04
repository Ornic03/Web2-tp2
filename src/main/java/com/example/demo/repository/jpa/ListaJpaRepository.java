package com.example.demo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListaJpaRepository extends JpaRepository<ListaEntity, Long> {
    List<ListaEntity> findByUsuarioIdOrderByIdAsc(Long usuarioId);
}
