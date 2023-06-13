package com.ncc.kairos.moirai.zeus.api;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * ApuUtil class auto generated from swagger code gen.
 * For this project we only generate the api and model and ignore everything else.
 * This file falls under 'everything else' so we copy it over here so we don't need to generate it again.
 * @author vince charming
 */
@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
public final class ApiUtil {

    // Private null constructor
    private ApiUtil() {

    }

    public static void setExampleResponse(NativeWebRequest req, String contentType, String example) {
        try {
            HttpServletResponse res = req.getNativeResponse(HttpServletResponse.class);
            res.setCharacterEncoding("UTF-8");
            res.addHeader("Content-Type", contentType);
            res.getWriter().print(example);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkForValidId(String id) {
        return !StringUtils.isEmpty(id) && !id.equals("-1");
    }
}
