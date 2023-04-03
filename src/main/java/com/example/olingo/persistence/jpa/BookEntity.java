package com.example.olingo.persistence.jpa;


import javax.persistence.*;

@Entity
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String title;
    private Integer page;

    @ManyToOne
    @JoinColumn(name = "categoryId")
    private CategoryEntity category;

    public BookEntity(){}

    public BookEntity(String title, Integer page, CategoryEntity category){
        this.title = title;
        this.page = page;
        this.category=category;
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
    public CategoryEntity getCategory() {
        return category;
    }
}
