package com.nextcentury.kairos.performer.hrf;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.logging.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.io.OutputStream;

import org.apache.commons.fileupload.FileItem;

import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.nextcentury.kairos.utils.ParameterValidation;

public abstract class AHandler implements HttpHandler {
    private static final Logger logger = LogManager.getLogger(AHandler.class);

    @Override
    public void handle(final HttpExchange t) throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();
        for(Entry<String, List<String>> header : t.getRequestHeaders().entrySet()) {
            logger.info(header.getKey() + ": " + header.getValue().get(0));
        }
        DiskFileItemFactory d = new DiskFileItemFactory();      

        try {
            ServletFileUpload up = new ServletFileUpload(d);
            List<FileItem> result = up.parseRequest(new RequestContext() {

                @Override
                public String getCharacterEncoding() {
                    return "UTF-8";
                }

                @Override
                public int getContentLength() {
                    return 0;
                }

                @Override
                public String getContentType() {
                    return t.getRequestHeaders().getFirst("Content-type");
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return t.getRequestBody();
                }

                public Map<String,String> getParameters() {
                    return parameters;
                }

            });
            t.getResponseHeaders().add("Content-type", "text/plain");
            
            List<FileItem> parsedFiles = new ArrayList<>();
            // Get all parameters and files written
            for(FileItem fi : result) {
                if (fi.isFormField()){
                    parameters.put(fi.getFieldName(), fi.getString());
                    logger.info("Parameters: " + fi.getFieldName() + "= " + fi.getString());
                } else {
                    logger.info("File-Item: " + fi.getFieldName() + " = " + fi.getName());
                    parsedFiles.add(fi);
                }
            }
             // Parameter validation here
            boolean passedValidation = ParameterValidation.checkParamaterValidityHRF(t, parameters);
            // upload
            if (passedValidation) {
                for(FileItem file : parsedFiles) {
                    handle(t, file, parameters);
                }
            }
            
            // os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }            
    }

    public abstract void handle(HttpExchange httpExchange, FileItem file, Map<String, String> parameters) throws IOException;

}