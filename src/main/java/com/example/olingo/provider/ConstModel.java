package com.example.olingo.provider;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

public final class ConstModel {
    //service Namespace
    public static final String NAMESPACE = "OData.Demo";

    //EDM Container
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    //Entity Types Names
    public static final String ET_BOOK_NAME = "Book";
    public static final String ET_CATEGORY_NAME = "Category";
    public static final FullQualifiedName ET_BOOK_FQN = new FullQualifiedName(NAMESPACE, ET_BOOK_NAME);
    public static final FullQualifiedName ET_CATEGORY_FQN = new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

    //Entity Set Names
    public static final String ES_BOOKS_NAME ="Books";
    public static final String ES_CATEGORIES_NAME = "Categories";

}
