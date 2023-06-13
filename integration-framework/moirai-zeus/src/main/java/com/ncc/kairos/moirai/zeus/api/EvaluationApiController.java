package com.ncc.kairos.moirai.zeus.api;

import io.swagger.annotations.ApiParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;

import com.ncc.kairos.moirai.zeus.model.Evaluation;
import com.ncc.kairos.moirai.zeus.model.StringResponse;
import com.ncc.kairos.moirai.zeus.services.EvaluationService;

import java.util.List;
import java.util.Optional;

/**
 * Controller Implementation of the EvaluationApi that is autogenerated by
 * swagger-codegen.
 *
 * @author Ryan Scott
 */
@Controller
@RequestMapping("${openapi.moiraiZeus.base-path:}")
public class EvaluationApiController implements EvaluationApi {

    private final NativeWebRequest request;

    @Autowired
    public EvaluationApiController(NativeWebRequest request) {
        this.request = request;
    }

    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(this.request);
    }

    @Autowired
    private EvaluationService evaluationService;

    @Override
    public ResponseEntity<List<Evaluation>> getEvaluations() {
        return new ResponseEntity<>(evaluationService.getEvaluations(), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> newEvaluation(
            @ApiParam(value = "Evaluation to save") @Valid @RequestBody Evaluation evaluation) {
        if (evaluationService.hasMatch(evaluation)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Evaluation already exists with id " + evaluation.getId());
        }

        evaluationService.newEvaluation(evaluation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> updateEvaluation(
            @ApiParam(value = "Evaluation to save") @Valid @RequestBody Evaluation evaluation) {

        if (evaluation.getId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evaluation missing id");
        }

        if (!evaluationService.hasMatch(evaluation)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Evaluation not found with id " + evaluation.getId());
        }

        evaluationService.updateEvaluation(evaluation);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> deleteEvaluation(
            @ApiParam(value = "evaluation id", required = true) @PathVariable("id") String id) {
        if (id.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id must be set");
        }

        if (!evaluationService.hasMatch(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Evaluation not found with id " + id);
        }

        evaluationService.deleteEvaluation(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
