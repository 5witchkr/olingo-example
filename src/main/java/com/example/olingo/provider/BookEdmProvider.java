package com.example.olingo.provider;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class BookEdmProvider extends CsdlAbstractEdmProvider {
    //CsdlEntityType을 모델링하기 위해 메타데이터를 제공
    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        CsdlEntityType entityType = null;
        if(entityTypeName.equals(ConstModel.ET_BOOK_FQN)){
            CsdlEntityType entityType1;
            CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
            CsdlProperty title = new CsdlProperty().setName("Title").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty page = new CsdlProperty().setName("Page").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty category = new CsdlProperty().setName("CategoryName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            //키 요소에 대한 PropertyRef 생성
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            //nav
            CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Category")
                    .setType(ConstModel.ET_CATEGORY_FQN).setNullable(false).setPartner("Books");
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
            navPropList.add(navProp);

            //config EntityType
            entityType1 = new CsdlEntityType();
            entityType1.setName(ConstModel.ET_BOOK_NAME);
            entityType1.setProperties(Arrays.asList(id, title , page, category));
            entityType1.setKey(Arrays.asList(propertyRef));
            entityType1.setNavigationProperties(navPropList);
            entityType = entityType1;
        }else if (entityTypeName.equals(ConstModel.ET_CATEGORY_FQN)) {
            CsdlEntityType entityType1;
            // create EntityType properties
            CsdlProperty id = new CsdlProperty().setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty().setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create PropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // navigation property
            CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Books")
                    .setType(ConstModel.ET_BOOK_FQN).setCollection(true).setPartner("Category");
            List<CsdlNavigationProperty> navPropList = new ArrayList<>();
            navPropList.add(navProp);

            // configure EntityType
            entityType1 = new CsdlEntityType();
            entityType1.setName(ConstModel.ET_CATEGORY_NAME);
            entityType1.setProperties(Arrays.asList(id, name));
            entityType1.setKey(Arrays.asList(propertyRef));
            entityType1.setNavigationProperties(navPropList);
            entityType = entityType1;
        }
        return entityType;
    }


    //엔티티Set은 OData 서비스를 사용하여 데이터를 요청할 때 중요한 리소스입니다.
    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName){
        CsdlEntitySet entitySet = null;
        if (entityContainer.equals(ConstModel.CONTAINER)){
            if (entitySetName.equals(ConstModel.ES_BOOKS_NAME)) {
                CsdlEntitySet entitySet1;
                entitySet1 = new CsdlEntitySet();
                entitySet1.setName(ConstModel.ES_BOOKS_NAME);
                entitySet1.setType(ConstModel.ET_BOOK_FQN);
                // navigation
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget("Categories");
                navPropBinding.setPath("Category");
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
                navPropBindingList.add(navPropBinding);
                entitySet1.setNavigationPropertyBindings(navPropBindingList);
                entitySet = entitySet1;
            } else if (entitySetName.equals(ConstModel.ES_CATEGORIES_NAME)) {
                CsdlEntitySet entitySet1;
                entitySet1 = new CsdlEntitySet();
                entitySet1.setName(ConstModel.ES_CATEGORIES_NAME);
                entitySet1.setType(ConstModel.ET_CATEGORY_FQN);

                // navigation
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget("Books");
                navPropBinding.setPath("Books");
                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
                navPropBindingList.add(navPropBinding);
                entitySet1.setNavigationPropertyBindings(navPropBindingList);
                entitySet = entitySet1;
            }
        }
        return entitySet;
    }


    //데이터를 제공하기 위해 OData 서비스에는 엔티티셋을 담는 엔티티컨테이너가 필요합니다.
    @Override
    public CsdlEntityContainer getEntityContainer(){
        //create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(ConstModel.CONTAINER, ConstModel.ES_BOOKS_NAME));
        entitySets.add(getEntitySet(ConstModel.CONTAINER, ConstModel.ES_CATEGORIES_NAME));

        //create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(ConstModel.CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);
        return entityContainer;
    }


    //이제 이 모든 요소를 CsdlSchema에 넣어야 합니다.
    //스키마 목록을 만들고 여기에 새 CsdlSchema 개체를 하나 추가합니다.
    //스키마는 모든 요소를 고유하게 식별하는 역할을 하는 네임스페이스로 구성됩니다.
    @Override
    public List<CsdlSchema> getSchemas(){
        //create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(ConstModel.NAMESPACE);

        //add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ConstModel.ET_BOOK_FQN));
        entityTypes.add(getEntityType(ConstModel.ET_CATEGORY_FQN));
        schema.setEntityTypes(entityTypes);

        //addEntityContainer
        schema.setEntityContainer(getEntityContainer());

        //finally
        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);
        return schemas;
    }

    //문서를 출력합니다.
    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName){
        if (entityContainerName == null || entityContainerName.equals(ConstModel.CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(ConstModel.CONTAINER);
            return entityContainerInfo;
        }
        return null;
    }

}