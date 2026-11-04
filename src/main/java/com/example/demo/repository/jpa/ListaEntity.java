package com.example.demo.repository.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "listas")
public class ListaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    protected ListaEntity() { }

    public ListaEntity(Long id, String nombre, UsuarioEntity usuario) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public UsuarioEntity getUsuario() { return usuario; }
}
