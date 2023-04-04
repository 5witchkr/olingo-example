package com.example.olingo.processor;

import com.example.olingo.persistence.Storage;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.springframework.stereotype.Component;

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

        //todo relatedEntityCollection 확인을 위한 segmentCount
        int segmentCount = resourcesPaths.size();

        EntityCollection entityCollection = null;
        if (segmentCount == 1){
            //요청된 엔티티셋 이름에 대한 데이터를 백엔드에서 가져옵니다.
            entityCollection = storage.readEntitySetData(edmEntitySet);
        } else if (segmentCount == 2) {
            UriResource firstSegment = resourcesPaths.get(0);
            UriResource lastSegment = resourcesPaths.get(1);

            UriResourceEntitySet uriResourceEntitySet1 = (UriResourceEntitySet) resourcesPaths.get(0);

            EdmEntitySet edmEntitySet1 = uriResourceEntitySet1.getEntitySet();

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

        //skip handler
        SkipOption skipOption = uriInfo.getSkipOption();
        if (skipOption != null) {
            int skipNumber = skipOption.getValue();
            if (skipNumber >= 0) {
                if(skipNumber <= entityList.size()) {
                    entityList = entityList.subList(skipNumber, entityList.size());
                } else {
                    // The client skipped all entities
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
                }  // else the client has requested more entities than available => return what we have
            } else {
                throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }
        }

        // after applying the query options, create EntityCollection based on the reduced list
        for(Entity entity : entityList){
            returnEntityCollection.getEntities().add(entity);
        }

        //select
        SelectOption selectOption = uriInfo.getSelectOption();

        //요청된 형식(JSON)에 따라 직렬화기를 생성합니다.
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
