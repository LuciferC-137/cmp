package com.luciferc137.cmp.database.model;

import java.time.LocalDateTime;

/**
 * Entité représentant un tag/étiquette dans la base de données.
 */
public class TagEntity {

    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;

    public TagEntity() {
    }

    public TagEntity(String name) {
        this.name = name;
        this.color = "#808080"; // Couleur par défaut
    }

    public TagEntity(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TagEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagEntity that = (TagEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

