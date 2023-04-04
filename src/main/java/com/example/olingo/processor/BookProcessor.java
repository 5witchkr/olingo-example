package com.example.olingo.processor;

import com.example.olingo.persistence.Storage;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Component
public class BookProcessor implements EntityProcessor {
    private OData oData;
    private ServiceMetadata serviceMetadata;
    private final Storage storage;

    public BookProcessor(Storage storage) {
        this.storage = storage;
    }
    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.oData = oData;
        this.serviceMetadata = serviceMetadata;
    }
    @Override
    public void readEntity(ODataRequest request, ODataResponse response,
                           UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, SerializerException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Entity entity = storage.readEntityData(edmEntitySet, keyPredicates);

        //serialize
        EdmEntityType entityType = edmEntitySet.getEntityType();

        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = oData.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(serviceMetadata, entityType, entity, options);
        InputStream entityStream = serializerResult.getContent();

        //response object
        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
        UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        EdmEntitySet edmEntitySet = uriResource.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.oData.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        Entity createEntity = storage.createEntityData(edmEntitySet, requestEntity);

        ContextURL contextURL = ContextURL.with().entitySet(edmEntitySet).build();

        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextURL).build();

        ODataSerializer serializer = this.oData.createSerializer(requestFormat);
        SerializerResult serializerResponse = serializer.entity(serviceMetadata, edmEntityType, createEntity, options);

        response.setContent(serializerResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
                             ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.oData.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

        HttpMethod httpMethod = request.getMethod();
        storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);

        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
            throws ODataApplicationException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        storage.deleteEntityData(edmEntitySet, keyPredicates);

        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
}
