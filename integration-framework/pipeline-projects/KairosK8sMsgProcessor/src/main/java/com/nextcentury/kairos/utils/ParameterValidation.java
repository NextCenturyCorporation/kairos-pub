package com.nextcentury.kairos.utils;

import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import java.io.OutputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ParameterValidation {
    private static final Logger logger = LogManager.getLogger(ParameterValidation.class);

    public static boolean checkParamaterValidityHRF(HttpExchange httpExchange, Map<String, String> params) throws IOException {
        boolean isValid = true;
        if ( StringUtils.isBlank(params.get("performername"))) {
            isValid = false;
            logger.error("Missing performername body parameter");
            httpExchange.sendResponseHeaders(404, 0);
        }
        else if ( StringUtils.isBlank(params.get("runId"))) {
            isValid = false;
            logger.error("Missing runId body parameter");
            httpExchange.sendResponseHeaders(404, 0);
        }
        else {
            logger.info("passed HRF Request validation.");
            httpExchange.sendResponseHeaders(200, 0);   
        }
        OutputStream os = httpExchange.getResponseBody();
        os.write("Missing Body Parameters".getBytes());

        return isValid;
    }
}