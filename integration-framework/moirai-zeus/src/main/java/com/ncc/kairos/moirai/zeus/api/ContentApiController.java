package com.ncc.kairos.moirai.zeus.api;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.DropdownDao;
import com.ncc.kairos.moirai.zeus.services.ContentServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.annotations.ApiParam;

@Controller
@RequestMapping("${openapi.moiraiZeus.base-path:}")
public class ContentApiController implements ContentApi {

    @Autowired
    private ContentServices service;

    public  ResponseEntity<List<DropdownDao>> retrieveDropdownByKey
    (@ApiParam(value = "Contents to retrieve by key",required=true) @PathVariable("key") String key, 
    @ApiParam(value = "Provides a Select one option",required=true) @PathVariable("selectOne") Boolean selectOne) {
        List<DropdownDao> returnList = service.getDropdownsByKey(key, selectOne);
        return new ResponseEntity<>(returnList, HttpStatus.OK);
    }
    
}
