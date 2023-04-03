package com.example.olingo.persistence.jpa;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String title;
    private Integer page;

    public BookEntity(){}

    public BookEntity(String title, Integer page){
        this.title = title;
        this.page = page;
    }

    public Long getId() {
        return id;
    }
    public String getTitle(){
        return title;
    }

    public Integer getPage() {
        return page;
    }
}
