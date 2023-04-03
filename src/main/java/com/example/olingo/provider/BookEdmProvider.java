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
    //CsdlEntityType을 모델링하기 위해 다음과 같은 메타데이터를 제공해야 합니다.
    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        CsdlEntityType entityType = null;
        if(entityTypeName.equals(ConstModel.ET_BOOK_FQN)){
            entityType = getBookEntityType();
        }else if (entityTypeName.equals(ConstModel.ET_CATEGORY_FQN)) {
            entityType = getCategoryEntityType();
        }
        return entityType;
    }


    //엔티티 집합은 OData 서비스를 사용하여 데이터를 요청할 때 중요한 리소스입니다.
    //이 예제에서는 제품 목록을 제공할 것으로 예상되는 다음 URL을 호출합니다
    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName){
        CsdlEntitySet entitySet = null;
        if (entityContainer.equals(ConstModel.CONTAINER)){
            if (entitySetName.equals(ConstModel.ES_BOOKS_NAME)) {
                entitySet = getBookEntitySet();
            } else if (entitySetName.equals(ConstModel.ES_CATEGORIES_NAME)) {
                entitySet = getCategoryEntitySet();
            }
        }
        return entitySet;
    }


    //데이터를 제공하기 위해 OData 서비스에는 엔티티셋을 담는 엔티티컨테이너가 필요합니다.
    //이 예제에서는 엔티티셋이 하나뿐이므로 엔티티컨테이너를 하나 생성하고 엔티티셋을 설정합니다.
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
    //OData 서비스 모델에는 여러 스키마가 있을 수 있지만,
    //대부분의 경우 스키마는 하나만 있을 것입니다.
    //따라서 이 예제에서는 스키마 목록을 만들고 여기에 새 CsdlSchema 개체를 하나 추가합니다.
    //스키마는 모든 요소를 고유하게 식별하는 역할을 하는 네임스페이스로 구성됩니다.
    //그런 다음 요소가 스키마에 추가됩니다.
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

    private static CsdlEntityType getCategoryEntityType() {
        CsdlEntityType entityType;
        // create EntityType properties
        CsdlProperty id = new CsdlProperty().setName("ID")
                .setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty name = new CsdlProperty().setName("Name")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        // create PropertyRef for Key element
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("ID");

        // navigation property: one-to-many
        CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Books")
                .setType(ConstModel.ET_BOOK_FQN).setCollection(true).setPartner("Category");
        List<CsdlNavigationProperty> navPropList = new ArrayList<>();
        navPropList.add(navProp);

        // configure EntityType
        entityType = new CsdlEntityType();
        entityType.setName(ConstModel.ET_CATEGORY_NAME);
        entityType.setProperties(Arrays.asList(id, name));
        entityType.setKey(Arrays.asList(propertyRef));
        entityType.setNavigationProperties(navPropList);
        return entityType;
    }

    private static CsdlEntityType getBookEntityType() {
        CsdlEntityType entityType;
        CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
        CsdlProperty title = new CsdlProperty().setName("Title").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty page = new CsdlProperty().setName("Page").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty category = new CsdlProperty().setName("Category").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        //키 요소에 대한 PropertyRef 생성
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();
        propertyRef.setName("ID");

        //nav
        CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Category")
                .setType(ConstModel.ET_CATEGORY_FQN).setNullable(false).setPartner("Books");
        List<CsdlNavigationProperty> navPropList = new ArrayList<>();
        navPropList.add(navProp);

        //config EntityType
        entityType = new CsdlEntityType();
        entityType.setName(ConstModel.ET_BOOK_NAME);
        entityType.setProperties(Arrays.asList(id, title , page, category));
        entityType.setKey(Arrays.asList(propertyRef));
        entityType.setNavigationProperties(navPropList);
        return entityType;
    }

    private static CsdlEntitySet getCategoryEntitySet() {
        CsdlEntitySet entitySet;
        entitySet = new CsdlEntitySet();
        entitySet.setName(ConstModel.ES_CATEGORIES_NAME);
        entitySet.setType(ConstModel.ET_CATEGORY_FQN);

        // navigation
        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
        navPropBinding.setTarget("Books"); // the target entity set, where the navigation property points to
        navPropBinding.setPath("Books"); // the path from entity type to navigation property
        List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
        navPropBindingList.add(navPropBinding);
        entitySet.setNavigationPropertyBindings(navPropBindingList);
        return entitySet;
    }

    private static CsdlEntitySet getBookEntitySet() {
        CsdlEntitySet entitySet;
        entitySet = new CsdlEntitySet();
        entitySet.setName(ConstModel.ES_BOOKS_NAME);
        entitySet.setType(ConstModel.ET_BOOK_FQN);
        // navigation
        CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
        navPropBinding.setTarget("Categories"); // the target entity set, where the navigation property points to
        navPropBinding.setPath("Category"); // the path from entity type to navigation property
        List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();
        navPropBindingList.add(navPropBinding);
        entitySet.setNavigationPropertyBindings(navPropBindingList);
        return entitySet;
    }
}