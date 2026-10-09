package com.example.demo.repository.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favoritos")
public class FavoritoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 250)
    private String nota;

    // El nombre del dominio/JSON del TP1 se conserva; en SQL es fecha_alta.
    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAgregado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    private ListaEntity lista;

    protected FavoritoEntity() { }

    public FavoritoEntity(Long id, Long productoId, String nota,
                          LocalDateTime fechaAgregado, ListaEntity lista) {
        this.id = id;
        this.productoId = productoId;
        this.nota = nota;
        this.fechaAgregado = fechaAgregado;
        this.lista = lista;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getNota() { return nota; }
    public LocalDateTime getFechaAgregado() { return fechaAgregado; }
    public ListaEntity getLista() { return lista; }
}
