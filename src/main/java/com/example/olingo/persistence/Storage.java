package com.example.olingo.persistence;

import com.example.olingo.persistence.jpa.BookEntity;
import com.example.olingo.persistence.jpa.BookJpaRepository;
import com.example.olingo.persistence.jpa.CategoryEntity;
import com.example.olingo.persistence.jpa.CategoryJpaRepository;
import com.example.olingo.provider.ConstModel;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class Storage {
    private final BookJpaRepository bookJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public Storage(BookJpaRepository bookJpaRepository, CategoryJpaRepository categoryJpaRepository) {
        this.bookJpaRepository = bookJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
        initSampleData();
    }

    /* PUBLIC FACADE */
    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) {
        if(edmEntitySet.getName().equals(ConstModel.ES_BOOKS_NAME)){
            return getBooks();
        }
        if(edmEntitySet.getName().equals(ConstModel.ES_CATEGORIES_NAME)){
            return getCategories();
        }
        return null;
    }

    public EntityCollection readRelateEntitySetData(EdmEntitySet edmEntitySet1 , List<UriParameter> uriParameters) {
        EdmEntityType edmEntityType1 = edmEntitySet1.getEntityType();
        if(edmEntityType1.getName().equals(ConstModel.ET_CATEGORY_NAME)){
            return getRelateBooks(edmEntityType1, uriParameters);
        }
        return null;
    }


    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        if(edmEntityType.getName().equals(ConstModel.ET_BOOK_NAME)){
            return getBook(edmEntityType, keyParams);
        }
        if(edmEntityType.getName().equals(ConstModel.ET_CATEGORY_NAME)){
            return getCategory(edmEntityType, keyParams);
        }
        return null;
    }

    public Entity createEntityData(EdmEntitySet edmEntitySet, Entity requestEntity) {
        if (edmEntitySet.getName().equals(ConstModel.ES_BOOKS_NAME)) {
            BookEntity bookEntity = new BookEntity(
                    (String) requestEntity.getProperty("Title").getValue(),
                    (Integer) requestEntity.getProperty("Page").getValue(),
                    categoryJpaRepository.findByName((String) requestEntity.getProperty("Category").getValue())
            );
            BookEntity savedBookEntity = bookJpaRepository.save(bookEntity);
            return jpaBookToODataBook(savedBookEntity);
        }
        if (edmEntitySet.getName().equals(ConstModel.ES_CATEGORIES_NAME)) {
            CategoryEntity categoryEntity = new CategoryEntity(
                    (String) requestEntity.getProperty("Name").getValue()
            );
            CategoryEntity savedCategoryEntity = categoryJpaRepository.save(categoryEntity);
            return jpaCategoryToOdataCategory(savedCategoryEntity);
        }
        return null;
    }
    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates, Entity requestEntity, HttpMethod httpMethod) {
    }

    public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyPredicates) {
    }

    private void initSampleData(){

        // add some sample product entities
        //sample entities
        CategoryEntity category1 = new CategoryEntity("동화");
        CategoryEntity category2 = new CategoryEntity("개발");
        categoryJpaRepository.save(category1);
        categoryJpaRepository.save(category2);
        BookEntity book1 = new BookEntity("이펙티브자바",100, category2);
        BookEntity book2 = new BookEntity("콩쥐팥쥐",200, category1);
        BookEntity book3 = new BookEntity("데이터중심앱설계",300, category2);
        bookJpaRepository.save(book1);
        bookJpaRepository.save(book2);
        bookJpaRepository.save(book3);
    }

    private EntityCollection getBooks(){
        EntityCollection retEntitySet = new EntityCollection();
        for(BookEntity bookEntity : bookJpaRepository.findAll()){
            Entity e = jpaBookToODataBook(bookEntity);
            e.setId(createId("Books", 1));
            retEntitySet.getEntities().add(e);
        }
        return retEntitySet;
    }

    private EntityCollection getRelateBooks(EdmEntityType edmEntityType, List<UriParameter> keyParams) {
        EntityCollection retEntitySet = new EntityCollection();
        for(BookEntity bookEntity : bookJpaRepository.findByCategory(
                categoryJpaRepository.findById(Long.valueOf(keyParams.get(0).getText()))
                        .orElseThrow(() -> new EntityNotFoundException("없음"))
        )){
            Entity e = jpaBookToODataBook(bookEntity);
            e.setId(createId("Books", 1));
            retEntitySet.getEntities().add(e);
        }
        return retEntitySet;
    }

    private EntityCollection getCategories(){
        EntityCollection retEntitySet = new EntityCollection();
        for(CategoryEntity categoryEntity : categoryJpaRepository.findAll()){
             Entity e = jpaCategoryToOdataCategory(categoryEntity);
            e.setId(createId("Categories", 1));
            retEntitySet.getEntities().add(e);
        }
        return retEntitySet;
    }

    private Entity getBook(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException{

        BookEntity bookEntity = bookJpaRepository.findById(Long.valueOf(keyParams.get(0).getText()))
                .orElseThrow(() -> new EntityNotFoundException("없음"));
        Entity e = jpaBookToODataBook(bookEntity);
        e.setId(createId("Books", 1));
        return e;
    }

    private Entity getCategory(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException{

        CategoryEntity categoryEntity = categoryJpaRepository.findById(Long.valueOf(keyParams.get(0).getText()))
                .orElseThrow(() -> new EntityNotFoundException("없음"));
        Entity e = jpaCategoryToOdataCategory(categoryEntity);
        e.setId(createId("Books", 1));
        return e;
    }

    private Entity jpaCategoryToOdataCategory(CategoryEntity categoryEntity) {
        return new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, categoryEntity.getId()))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, categoryEntity.getName()));
    }

    private Entity jpaBookToODataBook(BookEntity bookEntity) {
        return new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, bookEntity.getId()))
                .addProperty(new Property(null, "Title", ValueType.PRIMITIVE, bookEntity.getTitle()))
                .addProperty(new Property(null, "Page", ValueType.PRIMITIVE, bookEntity.getPage()))
                .addProperty(new Property(null, "Category", ValueType.PRIMITIVE, bookEntity.getCategory().getName()));
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
