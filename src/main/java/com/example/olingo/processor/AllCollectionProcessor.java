//package com.example.olingo.processor;
//
//import com.example.olingo.persistence.Storage;
//import java.io.InputStream;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Locale;
//
//import org.apache.olingo.commons.api.data.ContextURL;
//import org.apache.olingo.commons.api.data.Entity;
//import org.apache.olingo.commons.api.data.EntityCollection;
//import org.apache.olingo.commons.api.edm.EdmEntitySet;
//import org.apache.olingo.commons.api.edm.EdmEntityType;
//import org.apache.olingo.commons.api.edm.EdmProperty;
//import org.apache.olingo.commons.api.format.ContentType;
//import org.apache.olingo.commons.api.http.HttpHeader;
//import org.apache.olingo.commons.api.http.HttpStatusCode;
//import org.apache.olingo.server.api.OData;
//import org.apache.olingo.server.api.ODataApplicationException;
//import org.apache.olingo.server.api.ODataRequest;
//import org.apache.olingo.server.api.ODataResponse;
//import org.apache.olingo.server.api.ServiceMetadata;
//import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
//import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
//import org.apache.olingo.server.api.serializer.ODataSerializer;
//import org.apache.olingo.server.api.serializer.SerializerException;
//import org.apache.olingo.server.api.serializer.SerializerResult;
//import org.apache.olingo.server.api.uri.UriInfo;
//import org.apache.olingo.server.api.uri.UriInfoResource;
//import org.apache.olingo.server.api.uri.UriParameter;
//import org.apache.olingo.server.api.uri.UriResource;
//import org.apache.olingo.server.api.uri.UriResourceEntitySet;
//import org.apache.olingo.server.api.uri.UriResourceFunction;
//import org.apache.olingo.server.api.uri.UriResourceNavigation;
//import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
//import org.apache.olingo.server.api.uri.queryoption.CountOption;
//import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
//import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
//import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
//import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
//import org.apache.olingo.server.api.uri.queryoption.SelectOption;
//import org.apache.olingo.server.api.uri.queryoption.SkipOption;
//import org.apache.olingo.server.api.uri.queryoption.TopOption;
//import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
//import org.apache.olingo.server.api.uri.queryoption.expression.Member;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AllCollectionProcessor implements EntityCollectionProcessor {
//    private OData odata;
//    private ServiceMetadata serviceMetadata;
//    private Storage storage;
//
//    public AllCollectionProcessor(Storage storage) {
//        this.storage = storage;
//    }
//
//    public void init(OData odata, ServiceMetadata serviceMetadata) {
//        this.odata = odata;
//        this.serviceMetadata = serviceMetadata;
//    }
//
//    public void readEntityCollection(ODataRequest request, ODataResponse response,
//                                     UriInfo uriInfo, ContentType responseFormat)
//            throws ODataApplicationException, SerializerException {
//
//        final UriResource firstResourceSegment = uriInfo.getUriResourceParts().get(0);
//
//        if(firstResourceSegment instanceof UriResourceEntitySet) {
//            readEntityCollectionInternal(request, response, uriInfo, responseFormat);
//        } else if(firstResourceSegment instanceof UriResourceFunction) {
//            readFunctionImportCollection(request, response, uriInfo, responseFormat);
//        } else {
//            throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
//                    Locale.ENGLISH);
//        }
//    }
//
//    private void readFunctionImportCollection(final ODataRequest request, final ODataResponse response,
//                                              final UriInfo uriInfo, final ContentType responseFormat) throws ODataApplicationException, SerializerException {
//
//        // 1st step: Analyze the URI and fetch the entity collection returned by the function import
//        // Function Imports are always the first segment of the resource path
//        final UriResource firstSegment = uriInfo.getUriResourceParts().get(0);
//
//        if(!(firstSegment instanceof UriResourceFunction)) {
//            throw new ODataApplicationException("Not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),
//                    Locale.ENGLISH);
//        }
//
//        final UriResourceFunction uriResourceFunction = (UriResourceFunction) firstSegment;
//
//
//        // 2nd step: Serialize the response entity
//        final EdmEntityType edmEntityType = (EdmEntityType) uriResourceFunction.getFunction().getReturnType().getType();
//        final ContextURL contextURL = ContextURL.with().asCollection().type(edmEntityType).build();
//        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextURL).build();
//        final ODataSerializer serializer = odata.createSerializer(responseFormat);
//
//
//        // 3rd configure the response object
//
//        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
//        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
//    }
//
//    private void readEntityCollectionInternal(ODataRequest request, ODataResponse response, UriInfo uriInfo,
//                                              ContentType responseFormat) throws ODataApplicationException, SerializerException {
//
//        // Read the collection or process ONE navigation property
//        EdmEntitySet edmEntitySet = null; // we'll need this to build the ContextURL
//        EntityCollection entityCollection = null; // we'll need this to set the response body
//    // 1st retrieve the requested EntitySet from the uriInfo (representation of the parsed URI)
//    List<UriResource> resourceParts = uriInfo.getUriResourceParts();
//    int segmentCount = resourceParts.size();
//
//    UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
//    if (!(uriResource instanceof UriResourceEntitySet)) {
//        throw new ODataApplicationException("Only EntitySet is supported",
//                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
//    }
//
//    UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
//    EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();
//
//    if (segmentCount == 1) { // this is the case for: DemoService/DemoService.svc/Categories
//        edmEntitySet = startEdmEntitySet; // the response body is built from the first (and only) entitySet
//
//        // 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
//        entityCollection = storage.readEntitySetData(startEdmEntitySet);
//    } else if (segmentCount == 2) { // in case of navigation: DemoService.svc/Categories(3)/Products
//
//        UriResource lastSegment = resourceParts.get(1); // in our example we don't support more complex URIs
//        if (lastSegment instanceof UriResourceNavigation) {
//            // from Categories(1) to Products
//
//            // 2nd: fetch the data from backend
//            // first fetch the entity where the first segment of the URI points to
//            List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
//            // e.g. for Categories(3)/Products we have to find the single entity: Category with ID 3
//            Entity sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);
//            // error handling for e.g. DemoService.svc/Categories(99)/Products
//            if (sourceEntity == null) {
//                throw new ODataApplicationException("Entity not found.",
//                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
//            }
//            // then fetch the entity collection where the entity navigates to
//            // note: we don't need to check uriResourceNavigation.isCollection(),
//            // because we are the EntityCollectionProcessor
//            entityCollection = storage.readRelateEntitySetData(edmEntitySet , keyPredicates);
//        }
//    } else { // this would be the case for e.g. Products(1)/Category/Products
//        throw new ODataApplicationException("Not supported",
//                HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
//    }
//    List<Entity> modifiedEntityList = entityCollection.getEntities();
//    EntityCollection modifiedEntityCollection = new EntityCollection();
//
//    // 3rd: Apply system query option
//    // The system query options have to be applied in a defined order
//    // 3.1.) $filter
//
//    // 3.2.) $orderby
//    modifiedEntityList = applyOrderQueryOption(modifiedEntityList, uriInfo.getOrderByOption());
//    // 3.3.) $count
//    modifiedEntityList = applyCountQueryOption(modifiedEntityCollection, modifiedEntityList, uriInfo.getCountOption());
//    // 3.4.) $skip
//    modifiedEntityList = applySkipQueryOption(modifiedEntityList, uriInfo.getSkipOption());
//    // 3.5.) $top
//    modifiedEntityList = applyTopQueryOption(modifiedEntityList, uriInfo.getTopOption());
//    // 3.6.) Server driven paging (not part of this tutorial)
//    // 3.7.) $expand
//    // Nested system query options are not implemented
//    validateNestedExpxandSystemQueryOptions(uriInfo.getExpandOption());
//    // 3.8.) $select
//    SelectOption selectOption = uriInfo.getSelectOption();
//
//    // Set the (may) modified entityList to the new entity collection
//    modifiedEntityCollection.getEntities().addAll(modifiedEntityList);
//
//    // 4th: create a serializer based on the requested format (json)
//    ODataSerializer serializer = odata.createSerializer(responseFormat);
//
//    // we need the property names of the $select, in order to build the context URL
//    EdmEntityType edmEntityType = edmEntitySet.getEntityType();
//    String selectList = odata.createUriHelper()
//            .buildContextURLSelectList(edmEntityType, uriInfo.getExpandOption(), selectOption);
//    ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();
//
//    // adding the selectOption to the serializerOpts will actually tell the lib to do the job
//    final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
//    EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
//            .contextURL(contextUrl)
//            .count(uriInfo.getCountOption())
//            .select(selectOption)
//            .expand(uriInfo.getExpandOption())
//            .id(id)
//            .build();
//
//    // and serialize the content: transform from the EntitySet object to InputStream
//    SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType,
//            modifiedEntityCollection, opts);
//    InputStream serializedContent = serializerResult.getContent();
//
//    // 5th: configure the response object: set the body, headers and status code
//    response.setContent(serializedContent);
//    response.setStatusCode(HttpStatusCode.OK.getStatusCode());
//    response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
//}
//
//    private List<Entity> applyTopQueryOption(List<Entity> entityList, TopOption topOption)
//            throws ODataApplicationException {
//
//        if (topOption != null) {
//            int topNumber = topOption.getValue();
//            if (topNumber >= 0) {
//                if(topNumber <= entityList.size()) {
//                    entityList = entityList.subList(0, topNumber);
//                }  // else the client has requested more entities than available => return what we have
//            } else {
//                throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(),
//                        Locale.ROOT);
//            }
//        }
//
//        return entityList;
//    }
//
//    private List<Entity> applySkipQueryOption(List<Entity> entityList, SkipOption skipOption)
//            throws ODataApplicationException {
//
//        if (skipOption != null) {
//            int skipNumber = skipOption.getValue();
//            if (skipNumber >= 0) {
//                if(skipNumber <= entityList.size()) {
//                    entityList = entityList.subList(skipNumber, entityList.size());
//                } else {
//                    // The client skipped all entities
//                    entityList.clear();
//                }
//            } else {
//                throw new ODataApplicationException("Invalid value for $skip", HttpStatusCode.BAD_REQUEST.getStatusCode(),
//                        Locale.ROOT);
//            }
//        }
//
//        return entityList;
//    }
//
//    private List<Entity> applyCountQueryOption(EntityCollection entityCollection, List<Entity> modifiedEntityList,
//                                               CountOption countOption) {
//
//        // handle $count: always return the original number of entities, without considering $top and $skip
//        if (countOption != null) {
//            boolean isCount = countOption.getValue();
//            if (isCount) {
//                entityCollection.setCount(modifiedEntityList.size());
//            }
//        }
//
//        return modifiedEntityList;
//    }
//
//    private List<Entity> applyOrderQueryOption(List<Entity> entityList, OrderByOption orderByOption) {
//
//        if (orderByOption != null) {
//            List<OrderByItem> orderItemList = orderByOption.getOrders();
//            final OrderByItem orderByItem = orderItemList.get(0); // in our example we support only one
//            Expression expression = orderByItem.getExpression();
//            if (expression instanceof Member) {
//                UriInfoResource resourcePath = ((Member) expression).getResourcePath();
//                UriResource uriResource = resourcePath.getUriResourceParts().get(0);
//                if (uriResource instanceof UriResourcePrimitiveProperty) {
//                    EdmProperty edmProperty = ((UriResourcePrimitiveProperty) uriResource).getProperty();
//                    final String sortPropertyName = edmProperty.getName();
//
//                    // do the sorting for the list of entities
//                    Collections.sort(entityList, new Comparator<Entity>() {
//
//                        // we delegate the sorting to the native sorter of Integer and String
//                        public int compare(Entity entity1, Entity entity2) {
//                            int compareResult = 0;
//
//                            if (sortPropertyName.equals("ID")) {
//                                Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
//                                Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();
//
//                                compareResult = integer1.compareTo(integer2);
//                            } else {
//                                String propertyValue1 = (String) entity1.getProperty(sortPropertyName).getValue();
//                                String propertyValue2 = (String) entity2.getProperty(sortPropertyName).getValue();
//
//                                compareResult = propertyValue1.compareTo(propertyValue2);
//                            }
//
//                            // if 'desc' is specified in the URI, change the order of the list
//                            if (orderByItem.isDescending()) {
//                                return -compareResult; // just convert the result to negative value to change the order
//                            }
//
//                            return compareResult;
//                        }
//                    });
//                }
//            }
//        }
//
//        return entityList;
//    }
//
//    private void validateNestedExpxandSystemQueryOptions(final ExpandOption expandOption)
//            throws ODataApplicationException {
//        if(expandOption == null) {
//            return;
//        }
//
//        for(final ExpandItem item : expandOption.getExpandItems()) {
//            if(    item.getCountOption() != null
//                    || item.getFilterOption() != null
//                    || item.getLevelsOption() != null
//                    || item.getOrderByOption() != null
//                    || item.getSearchOption() != null
//                    || item.getSelectOption() != null
//                    || item.getSkipOption() != null
//                    || item.getTopOption() != null) {
//
//                throw new ODataApplicationException("Nested expand system query options are not implemented",
//                        HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(),Locale.ENGLISH);
//            }
//        }
//    }
//}
