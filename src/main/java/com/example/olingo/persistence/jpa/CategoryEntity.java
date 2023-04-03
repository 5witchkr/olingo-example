package com.example.olingo.persistence.jpa;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category")
    private List<BookEntity> books = new ArrayList<>();

    public CategoryEntity(){}

    public CategoryEntity(String name){
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
