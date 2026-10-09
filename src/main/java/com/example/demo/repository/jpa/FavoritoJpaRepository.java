package com.example.demo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoritoJpaRepository extends JpaRepository<FavoritoEntity, Long> {
    List<FavoritoEntity> findByListaIdOrderByIdAsc(Long listaId);
}
