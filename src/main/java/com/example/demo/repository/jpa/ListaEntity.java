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

    protected ListaEntity() { }

    public ListaEntity(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
