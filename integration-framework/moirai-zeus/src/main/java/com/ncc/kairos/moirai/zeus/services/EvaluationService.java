package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.EvaluationDatasetRepository;
import com.ncc.kairos.moirai.zeus.dao.EvaluationRepository;
import com.ncc.kairos.moirai.zeus.dao.ExperimentRepository;
import com.ncc.kairos.moirai.zeus.model.Dataset;
import com.ncc.kairos.moirai.zeus.model.Evaluation;
import com.ncc.kairos.moirai.zeus.model.Experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing evaluations.
 *
 * @author ryan scott
 * @version 0.1
 */
@Service
public class EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationDatasetRepository evaluationDatasetRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    public List<Evaluation> getEvaluations() {
        return evaluationRepository.findAll();
    }

    public boolean hasMatch(String id) {
        Evaluation match = getEvaluation(id);
        return (match != null);
    }

    public boolean hasMatch(Evaluation toMatch) {
        Evaluation match = getEvaluation(toMatch);
        return (match != null);
    }

    public Evaluation getEvaluation(String key) {
        Optional<Evaluation> opt = this.evaluationRepository.findById(key);
        if (!opt.isPresent()) {
            opt = this.evaluationRepository.findByName(key);
        }
        return opt.isPresent() ? opt.get() : null;
    }

    public Evaluation getEvaluation(Evaluation toMatch) {
        Optional<Evaluation> opt = this.evaluationRepository.findById(toMatch.getId());
        if (!opt.isPresent()) {
            opt = this.evaluationRepository.findByName(toMatch.getName());
        }
        return opt.isPresent() ? opt.get() : null;
    }

    public Evaluation newEvaluation(Evaluation evaluation) {
        evaluation.setId(null);
        evaluation.setCreationDate(OffsetDateTime.now());
        return saveEvaluation(evaluation);
    }

    public Evaluation updateEvaluation(Evaluation evaluation) {
        deleteEvaluation(evaluation);
        return saveEvaluation(evaluation);
    }

    public void deleteEvaluation(String id) {
        if (hasMatch(id)) {
            deleteEvaluation(getEvaluation(id));
        }
    }

    public void deleteEvaluation(Evaluation evaluation) {
        Evaluation existingEvaluation = getEvaluation(evaluation);

        if (existingEvaluation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Evaluation not found with id " + evaluation.getId());
        }

        List<Dataset> savedDatasets = evaluation.getDatasets();
        evaluationRepository.delete(existingEvaluation);
        savedDatasets.forEach(dataset -> evaluationDatasetRepository.delete(dataset));

    }

    private Evaluation saveEvaluation(Evaluation evaluation) {
        if (evaluation.getDatasets() == null) {
            evaluation.setDatasets(new ArrayList<Dataset>());
        }
        
        List<Dataset> savedDatasets = evaluation.getDatasets().stream()
                .map(dataset -> dataset.id(null))
                .map(dataset -> evaluationDatasetRepository.save(dataset))
                .collect(Collectors.toList());
        evaluation.setDatasets(savedDatasets);

        if (evaluation.getExperiments() == null) {
            evaluation.setExperiments(new ArrayList<Experiment>());
        }

        List<Experiment> savedExperiments = evaluation.getExperiments().stream()
                .map(experiment -> experiment.id(null))
                .map(experiment -> experimentRepository.save(experiment))
                .collect(Collectors.toList());
            evaluation.setExperiments(savedExperiments);
        return this.evaluationRepository.save(evaluation);
    }
}
