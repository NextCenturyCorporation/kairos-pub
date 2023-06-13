package com.ncc.kairos.moirai.zeus.api;

import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.services.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
@Controller
@RequestMapping("${openapi.moiraiZeus.base-path:}")
public class SupportController implements SupportApi {
    @Autowired
    private SupportService supportService;

    @Override
    public ResponseEntity<Faq> retrieveFaq(String id) {
        Faq faq = supportService.retrieveFaq(id);
        if(faq == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ with Id not found");
        }
        return new ResponseEntity<>(faq, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<FaqCategory>> retrieveFaqCategories() {
        return new ResponseEntity<>(supportService.retrieveFaqCategories(), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> deleteFaq(String id) {
        supportService.deleteFaq(id);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_DELETED), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> submitFaqCategories(@Valid List<FaqCategory> faqCategory) {
        supportService.submitFaqCategories(faqCategory);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_ADDED), HttpStatus.OK);

    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> deleteFaqCategory(String id) {
        supportService.deleteFaqCategory(id);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_DELETED), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<FaqCategory> retrieveFaqCategory(String id) {
        FaqCategory faqCategory = supportService.retrieveFaqCategory(id);
        if(faqCategory == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ category with Id not found");
        }
        return new ResponseEntity<>(faqCategory, HttpStatus.OK);

    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> updateFaqById(String id, Faq faq) {
        supportService.updateFaqById(id, faq);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_MODIFIED), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> updateFaqCategoryById(String id, FaqCategory faqCategory) {
        supportService.updateFaqCategoryById(id, faqCategory);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_MODIFIED), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> deleteFaqsByIdList(@NotNull @Valid List<String> idList) {
        supportService.deleteFaqByIdList(idList);
        return new ResponseEntity<>(new StringResponse().value(Constants.FAQ_DELETED), HttpStatus.OK);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleError(ConstraintViolationException constraintViolationException) {
        return new ResponseEntity<>(new StringResponse().value(constraintViolationException.getMessage()), HttpStatus.BAD_REQUEST);
    }
}