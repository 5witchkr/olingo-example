package com.example.olingo.controller;


import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@RestController
@RequestMapping(ODataController.URI)
public class ODataController {

    protected static final String URI = "/OData";

    private final EntityCollectionProcessor bookCollectionProcessor;
    private final EntityProcessor bookProcessor;
    private final CsdlEdmProvider bookEdmProvider;

    public ODataController(EntityCollectionProcessor bookCollectionProcessor, EntityProcessor bookProcessor, CsdlEdmProvider bookEdmProvider) {
        this.bookCollectionProcessor = bookCollectionProcessor;
        this.bookProcessor = bookProcessor;
        this.bookEdmProvider = bookEdmProvider;
    }

    @RequestMapping(value = "*")
    public void process1(HttpServletRequest request, HttpServletResponse response) {
        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(bookEdmProvider,
                new ArrayList<>());
        ODataHttpHandler handler = odata.createHandler(edm);
        handler.register(bookCollectionProcessor);
        handler.register(bookProcessor);
        handler.process(new HttpServletRequestWrapper(request) {
            @Override
            public String getServletPath() {
                return ODataController.URI;
            }
        }, response);
    }
    @RequestMapping(value = "/*/*")
    public void process2(HttpServletRequest request, HttpServletResponse response) {
        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(bookEdmProvider,
                new ArrayList<>());
        ODataHttpHandler handler = odata.createHandler(edm);
        handler.register(bookCollectionProcessor);
        handler.register(bookProcessor);
        handler.process(new HttpServletRequestWrapper(request) {
            @Override
            public String getServletPath() {
                return ODataController.URI;
            }
        }, response);
    }
}
