package com.example.olingo.processor;

import com.example.olingo.persistence.Storage;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;

import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.*;
import org.apache.olingo.server.api.uri.queryoption.*;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class BookCollectionProcessor implements EntityCollectionProcessor {
    private OData oData;
    private ServiceMetadata serviceMetadata;
    private final Storage storage;
    public BookCollectionProcessor(Storage storage) {
        this.storage = storage;
    }
    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.oData = oData;
        this.serviceMetadata = serviceMetadata;
    }
    //엔티티 컬렉션에 대한 HTTP GET 연산으로 OData 서비스가 호출될 때 이 readEntityCollection(...) 메서드가 호출된다
    //readEntityCollection(...) 메서드는 백엔드(예: 데이터베이스)에서 데이터를 "읽기"하고 OData 서비스를 호출하는 사용자에게 데이터를 전달하는 데 사용됩니다.
    @Override
    public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        //요청된 EntitySet을 (구문 분석된 서비스 URI의 표현인) uriInfo 객체에서 검색합니다.
        List<UriResource> resourcesPaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcesPaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        //relatedEntityCollection 확인을 위한 segmentCount
        int segmentCount = resourcesPaths.size();

        EntityCollection entityCollection = null;
        if (segmentCount == 1){
            //요청된 엔티티셋 이름에 대한 데이터를 백엔드에서 가져옵니다.
            entityCollection = storage.readEntitySetData(edmEntitySet);
        } else if (segmentCount == 2) {
            UriResourceEntitySet uriResourceEntitySet1 = (UriResourceEntitySet) resourcesPaths.get(0);
            EdmEntitySet edmEntitySet1 = uriResourceEntitySet1.getEntitySet();

            UriResource lastSegment = resourcesPaths.get(1);
            UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
            EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();

            String navPropName = edmNavigationProperty.getName();
            EdmBindingTarget edmBindingTarget = edmEntitySet.getRelatedBindingTarget(navPropName);
            if (edmBindingTarget == null) {
                throw new ODataApplicationException("Not supported.",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }

            if (edmBindingTarget instanceof EdmEntitySet) {
                edmEntitySet = (EdmEntitySet) edmBindingTarget;
            } else {
                throw new ODataApplicationException("Not supported.",
                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
            }


            List<UriParameter> keyPredicates = uriResourceEntitySet1.getKeyPredicates();
            entityCollection = storage.readRelateEntitySetData(edmEntitySet1, keyPredicates);
        }

        //사용자가 지정한 쿼리 옵션에 따라 결과 집합을 수정합니다.
        List<Entity> entityList = entityCollection.getEntities();
        EntityCollection returnEntityCollection = new EntityCollection();


        //count handler 원래 엔티티 수를 반환하고, $top 및 $skip을 무시합니다.
        CountOption countOption = uriInfo.getCountOption();
        if (countOption != null) {
            boolean isCount = countOption.getValue();
            if(isCount){
                returnEntityCollection.setCount(entityList.size());
            }
        }

        //apply $orderby
        OrderByOption orderByOption = uriInfo.getOrderByOption();
        if (orderByOption != null) {
            List<OrderByItem> orderItemList = orderByOption.getOrders();
            final OrderByItem orderByItem = orderItemList.get(0);
            Expression expression = orderByItem.getExpression();
            if(expression instanceof Member){
                UriInfoResource resourcePath = ((Member)expression).getResourcePath();
                UriResource uriResource = resourcePath.getUriResourceParts().get(0);
                if (uriResource instanceof UriResourcePrimitiveProperty) {
                    EdmProperty edmProperty = ((UriResourcePrimitiveProperty)uriResource).getProperty();
                    final String sortPropertyName = edmProperty.getName();

                    Collections.sort(entityList, new Comparator<Entity>() {

                        public int compare(Entity entity1, Entity entity2) {
                            int compareResult = 0;

                            if(sortPropertyName.equals("ID")){
                                Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
                                Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();

                                compareResult = integer1.compareTo(integer2);
                            } else if (sortPropertyName.equals("Page")) {
                                Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
                                Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();

                                compareResult = integer1.compareTo(integer2);
                            } else{
                                String propertyValue1 = (String) entity1.getProperty(sortPropertyName).getValue();
                                String propertyValue2 = (String) entity2.getProperty(sortPropertyName).getValue();

                                compareResult = propertyValue1.compareTo(propertyValue2);
                            }
                            //desc
                            if(orderByItem.isDescending()){
                                return - compareResult;
                            }

                            return compareResult;
                        }
                    });
                }
            }
        }

        //skip handler
        SkipOption skipOption = uriInfo.getSkipOption();
        if (skipOption != null) {
            int skipNumber = skipOption.getValue();
            if (skipNumber >= 0) {
                if(skipNumber <= entityList.size()) {
                    entityList = entityList.subList(skipNumber, entityList.size());
                } else {
                    entityList.clear();
                }
            } else {
                throw new ODataApplicationException("Invalid value for $skip", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }

        //top handler
        TopOption topOption = uriInfo.getTopOption();
        if (topOption != null) {
            int topNumber = topOption.getValue();
            if (topNumber >= 0) {
                if(topNumber <= entityList.size()) {
                    entityList = entityList.subList(0, topNumber);
                }
            } else {
                throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }

        for(Entity entity : entityList){
            returnEntityCollection.getEntities().add(entity);
        }

        //select
        SelectOption selectOption = uriInfo.getSelectOption();

        //요청된 형식(JSON)에 따라 직렬화
        ODataSerializer serializer = oData.createSerializer(responseFormat);

        //콘텐츠를 직렬화합니다: 엔티티셋 객체에서 입력 스트림으로 변환합니다.
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        String selectList = oData.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption);

        ContextURL contextURL = ContextURL.with()
                .entitySet(edmEntitySet)
                .selectList(selectList)
                .build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts =
                EntityCollectionSerializerOptions.with()
                        .id(id)
                        .contextURL(contextURL)
                        .select(selectOption)
                        .count(countOption)
                        .build();
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, returnEntityCollection, opts);

        //응답 개체 구성: 본문, 헤더 및 상태 코드 설정
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}
