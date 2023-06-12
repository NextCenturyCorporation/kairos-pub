package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.*;
import com.ncc.kairos.moirai.clotho.resources.ApplicationConstants;
import com.ncc.kairos.moirai.clotho.utilities.GraphTraversal;
import com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

import javax.validation.constraints.NotNull;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;

import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.ValidationUtils.*;

public class KsfModelValidation extends KsfValidator {

    private static final String INDEXED_ARRAY_FORMAT = "'%s'.%s[%d]";

    private JsonLdRepresentation jRep;
    private List<Instance> instances;
    private boolean isTask1;
    private String documentId;
    private final List<String> provenanceIds = new ArrayList<>();
    private final List<String> instanceIds = new ArrayList<>();

    // Structures to store instantiation-specific objects
    private List<String> entityIds;
    private List<String> eventIds;
    private List<String> participantIds;
    private List<String> valuesIds;
    private List<String> relationIds;
    private List<String> idsWithQnodes;
    private List<Filler> fillerObjs;
    private final HashMap<String, List<String>> idMap = new HashMap<>(); // maps KE type (incl. Provenance) to list of 5-digit IDs

    public KsfModelValidation() {
        jRep = null;
    }

    public KsfModelValidation(JsonLdRepresentation newRep) {
        if (newRep == null) {
            throw new IllegalArgumentException("Model cannot be null.");
        }
        setModel(newRep);
    }

    @Override
    public void setModel(Object model) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null.");
        } else if (model instanceof JsonLdRepresentation) {
            setModel((JsonLdRepresentation) model); // NOSONAR, because it wants us to remove the cast, which creates an infinite loop
        } else {
            throw new UnsupportedOperationException(model.getClass() + " models not supported.");
        }
    }

    private void setModel(JsonLdRepresentation model) {
        jRep = model;
        instanceIds.clear();
        provenanceIds.clear();
        idMap.clear();
        clearInstantiationSpecificStructures();
        isTA2 = jRep.getTa2() == null || jRep.getTa2();
        isTask1 = jRep.getTask2() == null || !jRep.getTask2();

        if (isTA2) {
            instances = jRep.getInstances();
            idMap.put(INSTANCES, new ArrayList<>());
            idMap.put(PROVENANCE, new ArrayList<>());
        } else {
            // Extract TA1 info into a single Instance.
            instances = new ArrayList<>();
            Instance schemaLib = new Instance();
            schemaLib.setEvents(jRep.getEvents());
            schemaLib.setEntities(jRep.getEntities());
            schemaLib.setRelations(jRep.getRelations());
            instances.add(schemaLib);
        }
    }

    /**
     * Return whether this validator is processing TA2 input.
     *
     * @return True if this validator is processing TA2 input
     */
    @Override
    public boolean isTA2() {
        return isTA2;
    }

    // Reset instantiation-specific data in-between processing different schemas of the same submission
    private void clearInstantiationSpecificStructures() {
        entityIds = new ArrayList<>();
        eventIds = new ArrayList<>();
        participantIds = new ArrayList<>();
        valuesIds = new ArrayList<>();
        relationIds = new ArrayList<>();
        idsWithQnodes = new ArrayList<>();
        fillerObjs = new ArrayList<>();
        idMap.put(EVENTS, new ArrayList<>());
        idMap.put(ENTITIES, new ArrayList<>());
        idMap.put(RELATIONS, new ArrayList<>());
        idMap.put(VALUES, new ArrayList<>());
        idMap.put(PARTICIPANTS, new ArrayList<>());
    }

    /**
     * Validate SDF input.
     * Fatal if any required elements are missing: @context, @id, sdfVersion, and version.
     * Error if document ID does not follow naming convention.
     * Fatal if sdfVersion does not equal {SDF_VERSION}.
     * (TA2): Error if additional required elements are missing: ceID, instances, provenanceData (Task1 only), task2.
     * (TA2): Error if events, entities, or relations is present.
     * (TA2): Error if instances contains more than 2 elements.
     * (TA2): Error if @id does not follow naming convention.
     * (TA2): Fatal if there are any duplicate 5-digit IDs within the @ids of the array of instances objects.
     * @return a List of Strings, one for each validation error or warning
     */
    @Override
    public List<String> validate() {
        List<String> ksfValidationErrorsToReturn = new ArrayList<>();

        documentId = jRep.getAtId();
        // Error if any required elements are missing: @id
        if (StringUtils.isAllEmpty(documentId)) {
            documentId = "<no @id>";
            ksfValidationErrorsToReturn.add(KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, JSON_LD_ID));
        } else {
            // Error if document ID does not follow naming convention.
            ksfValidationErrorsToReturn.addAll(validateIdNaming(SUBMISSIONS, documentId, JSON_LD_ID, List.of(documentId)));
        }
        // Error if any required elements are missing: sdfVersion
        final String sdfVersion = jRep.getSdfVersion();
        if (StringUtils.isAllEmpty(sdfVersion)) {
            ksfValidationErrorsToReturn.add(KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION));
        } else if (!sdfVersion.equals(ApplicationConstants.SDF_VERSION_VALUE)) {
            // Error if sdfVersion does not equal {SDF_VERSION}.
            ksfValidationErrorsToReturn.add(KsfValidationError.constructInvalidValueMessage(FATAL, DOCUMENT, documentId, SDF_VERSION, sdfVersion));
        }
        // Error if any required elements are missing: version
        if (StringUtils.isAllEmpty(jRep.getVersion())) {
            ksfValidationErrorsToReturn.add(KsfValidationError.constructMissingValueMessage(DOCUMENT, documentId, VERSION));
        }

        // Validate TA2 document-level issues.
        if (isTA2) {
            ksfValidationErrorsToReturn.addAll(validateTA2document());
        }

        // Validate each instantiation (ctor created an instantiation out of TA1 input)
        if (instances != null) {
            int instanceNum = 0;
            for (Instance instance : instances) {
                ksfValidationErrorsToReturn.addAll(validateInstantiation(instance, instanceNum++));
            }
            if (isTA2) {
                // (TA2): Error if @id does not follow naming convention.
                ksfValidationErrorsToReturn.addAll(validateIdNaming(INSTANCES, documentId, JSON_LD_ID, instanceIds));
                // (TA2): Fatal if there are any duplicate 5-digit IDs within the @ids of the array of instances objects.
                Set<String> duplicateIds = getDuplicateStrings(idMap.get(INSTANCES));
                if (!duplicateIds.isEmpty()) {
                    ksfValidationErrorsToReturn.add(KsfValidationError.constructDuplicateIdsMessage(INSTANCES, documentId, DOCUMENT, duplicateIds));
                }
                if (isTask1) {
                    // Error if provenanceID does not follow naming convention.
                    ksfValidationErrorsToReturn.addAll(validateIdNaming(PROVENANCE, documentId, PROVENANCE_ID, provenanceIds));

                    // Fatal if there are any duplicate 5-digit IDs within the provenanceID values in the array of provenanceData objects.
                    Set<String> duplicateProvenanceIds = getDuplicateStrings(idMap.get(PROVENANCE));

                    if (!duplicateProvenanceIds.isEmpty()) {
                        ksfValidationErrorsToReturn.add(KsfValidationError.constructDuplicateIdsMessage(PROVENANCE_DATA, documentId, DOCUMENT, duplicateProvenanceIds));
                    }
                }
            }
        }

        return ksfValidationErrorsToReturn;
    }

    // Validate TA2 document-level issues.
    private List<String> validateTA2document() {
        List<String> documentErrors = new ArrayList<>();

        // (TA2): Error if additional required elements are missing: ceID, instances, provenanceData (Task1 only), task2.
        if (StringUtils.isAllEmpty(jRep.getCeID())) {
            documentErrors.add(KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, CE_ID));
        }
        if (isNullOrEmptyList(instances)) {
            documentErrors.add(KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, INSTANCES));
        } else if (instances.size() > MAX_INSTANCES) {
            // (TA2): Error if instances contains more than 2 elements.
            documentErrors.add(KsfValidationError.constructTooManyValuesMessage(ERROR, DOCUMENT, documentId, INSTANCES, MAX_INSTANCES));
        }
        if (jRep.getTask2() == null) {
            documentErrors.add(KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, TASK_2));
        }
        if (isTask1) {
            if (isNullOrEmptyList(jRep.getProvenanceData())) {
                documentErrors.add(KsfValidationError.constructMissingValueMessage(FATAL, DOCUMENT, documentId, PROVENANCE_DATA));
            } else {
                documentErrors.addAll(validateProvenanceData(jRep.getProvenanceData()));
            }
        }

        // (TA2): Error if events, entities, or relations is present.
        if (isNonEmptyList(jRep.getEvents())) {
            documentErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, EVENTS));
        }
        if (isNonEmptyList(jRep.getEntities())) {
            documentErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, ENTITIES));
        }
        if (isNonEmptyList(jRep.getRelations())) {
            documentErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(DOCUMENT, documentId, RELATIONS));
        }

        return documentErrors;
    }

    private List<String> validateTa1RefNaming(String errorType, String objectType, String objectId, String candidateRef) {
        List<String> ta1RefErrors = new ArrayList<>();
        if (NONE.equals(candidateRef)) {
            return ta1RefErrors;
        }

        // Convert objectType to its mnemonic
        final String mnemonic;
        switch (objectType) {
            case INSTANCES:
                mnemonic = "SC";
                break;
            case EVENTS:
                mnemonic = "EV";
                break;
            case RELATIONS:
                mnemonic = "RE";
                break;
            default:
                mnemonic = null;
        }

        // ta1refs come through as <KE_MNEMONIC><5-digit-num><anything>
        if (candidateRef.length() < 2 || mnemonic == null || !candidateRef.startsWith(mnemonic)) {
            ta1RefErrors.add(KsfValidationError.constructNamingViolationMessage(errorType, objectType, objectId, TA1_REF, candidateRef, "object type"));
        } else {
            String restOfString = candidateRef.substring(2);
            String idStr = "<could not extract>";
            if (restOfString.length() >= UNIQUE_ID_NUMDIGITS) {
                idStr = restOfString.substring(0, UNIQUE_ID_NUMDIGITS);
            }
            if (!idStr.matches(String.format("\\d{%d}", UNIQUE_ID_NUMDIGITS))) {
                ta1RefErrors.add(KsfValidationError.constructNamingViolationMessage(errorType, objectType, objectId, TA1_REF, candidateRef, "5-digit int"));
            }
        }
        return ta1RefErrors;
    }

    private List<String> validateIdNaming(String objectType, String objectId, String keyword, List<String> candidateIds) {
        List<String> idValueErrors = validateURIs(objectType, objectId, keyword, candidateIds);

        if (!idValueErrors.isEmpty()) { // first check for invalid IRIs
            return idValueErrors;
        }

        for (String curId : candidateIds) {
            if (KAIROS_NULL_UNEXPANDED.equals(curId)) {
                continue;
            }
            // @ids come through unexpanded as <performer-prefix>/<OBJECT_TYPE>/<5-digit-num>/<anything>
            int keTypeStartIndex = curId.toLowerCase().indexOf("/" + objectType.toLowerCase() + "/");
            if (keTypeStartIndex < 0) {
                idValueErrors.add(KsfValidationError.constructNamingViolationMessage(objectType, objectId, keyword, curId, "object type"));
            } else if (!SUBMISSIONS.equals(objectType)) {
                String restOfString = curId.substring(keTypeStartIndex + objectType.length() + 2); // 2 for the slashes
                String idStr = "<could not extract>";
                if (restOfString.length() > UNIQUE_ID_NUMDIGITS) {
                    idStr = restOfString.substring(0, UNIQUE_ID_NUMDIGITS + 1); // includes what should be trailing slash
                } else if (restOfString.length() == UNIQUE_ID_NUMDIGITS) {
                    idStr = restOfString.substring(0, UNIQUE_ID_NUMDIGITS) + "/"; // Add trailing slash if it's missing for ease of regex
                }

                if (!idStr.matches(String.format("\\d{%d}/", UNIQUE_ID_NUMDIGITS))) {
                    idValueErrors.add(KsfValidationError.constructNamingViolationMessage(objectType, objectId, keyword, curId, "5-digit int"));
                } else if (!keyword.equals(TA1_REF)) {
                    List<String> idList = idMap.get(objectType);
                    idList.add(idStr.substring(0, UNIQUE_ID_NUMDIGITS)); // remove trailing slash
                }
            }
        }
        return idValueErrors;
    }

    private List<String> validateURIs(String objectType, String objectId, String keyword, String candidateURI) {
        if (candidateURI == null) {
            return new ArrayList<>();
        }
        return validateURIs(objectType, objectId, keyword, List.of(candidateURI));
    }

    private List<String> validateURIs(String objectType, String objectId, String keyword, List<String> candidateURIs) {
        List<String> idValueErrors = new ArrayList<>();
        if (candidateURIs == null) {
            return idValueErrors;
        }

        for (String curId : candidateURIs) {
            if (KAIROS_NULL_UNEXPANDED.equals(curId)) {
                continue;
            }
            try {
                new URI(curId);
            } catch (URISyntaxException error) {
                idValueErrors.add(KsfValidationError.constructInvalidURIMessage(objectType,
                        keyword.equals(JSON_LD_ID) && !objectType.equals(INSTANCES) ? curId : objectId, keyword, curId));
            }
        }
        return idValueErrors;
    }

    /**
     * Fatal if both entities and events are empty or missing.
     * Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying participants objects.
     * Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying relations objects.
     * Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying values objects.
     * Error if @ids do not follow naming convention.
     * (TA2): Error if any required elements are missing: @id, confidence, name, ta1ref (Fatal).
     * (TA2): Fatal if ta1ref is "none".
     * @param instance a TA1 schema library or a TA2 instantiation thereof
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateInstantiation(Instance instance, int instanceNum) {
        List<String> instantiationErrors = new ArrayList<>();
        clearInstantiationSpecificStructures();
        String instantiationId = isTA2 ? instance.getAtId() : documentId;
        if (isTA2) {
            // (TA2): Error if any required elements are missing: @id.
            if (StringUtils.isBlank(instantiationId)) {
                instantiationId = String.format("instance[%d]", instanceNum);
                instantiationErrors.add(KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, JSON_LD_ID));
            } else {
                instanceIds.add(instantiationId);
            }
            instantiationErrors.addAll(validateTA2instance(instance, instantiationId));
        }

        // Fatal if both entities and events are empty or missing.
        if (isNullOrEmptyList(instance.getEvents()) && isNullOrEmptyList(instance.getEntities())) {
            instantiationErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(FATAL, (isTA2 ? INSTANCES : DOCUMENT),
                    instantiationId, EVENTS, ENTITIES));
        } else {
            // Validate events, entities, and their dependencies
            instantiationErrors.addAll(validateEntities(instance.getEntities(), instantiationId));
            instantiationErrors.addAll(validateEvents(instance.getEvents(), instantiationId));

            // Error if @ids do not follow naming convention.
            instantiationErrors.addAll(validateIdNaming(PARTICIPANTS, instantiationId, JSON_LD_ID, participantIds));

            // Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying participants objects.
            Set<String> duplicateIds = getDuplicateStrings(idMap.get(PARTICIPANTS));
            if (!duplicateIds.isEmpty()) {
                instantiationErrors.add(KsfValidationError.constructDuplicateIdsMessage(PARTICIPANTS,
                        instantiationId, isTA2 ? INSTANCES : DOCUMENT, duplicateIds));
            }
        }

        // Validate relations
        instantiationErrors.addAll(validateRelations(instance.getRelations(), instantiationId));
        // Error if @ids do not follow naming convention.
        instantiationErrors.addAll(validateIdNaming(RELATIONS, instantiationId, JSON_LD_ID, relationIds));
        // Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying relations objects.
        Set<String> duplicateIds = getDuplicateStrings(idMap.get(RELATIONS));
        if (!duplicateIds.isEmpty()) {
            instantiationErrors.add(KsfValidationError.constructDuplicateIdsMessage(RELATIONS,
                    instantiationId, isTA2 ? INSTANCES : DOCUMENT, duplicateIds));
        }

        if (isTA2) {
            // Error if @ids do not follow naming convention.
            instantiationErrors.addAll(validateIdNaming(VALUES, instantiationId, JSON_LD_ID, valuesIds));

            // Fatal if there are any duplicate 5-digit IDs within the @ids of all underlying values objects.
            duplicateIds = getDuplicateStrings(idMap.get(VALUES));
            if (!duplicateIds.isEmpty()) {
                instantiationErrors.add(KsfValidationError.constructDuplicateIdsMessage(VALUES,
                        instantiationId, INSTANCES, duplicateIds));
            }

            // Validate predictions and fillers (requires that events, entities, and relations have been processed)
            instantiationErrors.addAll(validatePredictions(instance.getEvents()));
            instantiationErrors.addAll(validateFillerEntities(instance.getEvents(), instance.getEntities()));
        }

        return instantiationErrors;
    }

    // Validate TA2 instance-level issues.
    private List<String> validateTA2instance(Instance instance, String instantiationId) {
        List<String> instanceErrors = new ArrayList<>();

        // (TA2): Error if any required elements are missing: name.
        if (StringUtils.isBlank(instance.getName())) {
            instanceErrors.add(KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, NAME));
        }
        // (TA2): Fatal if any required elements are missing: ta1ref.
        String ta1ref = instance.getTa1ref();
        if (StringUtils.isBlank(ta1ref)) {
            instanceErrors.add(KsfValidationError.constructMissingValueMessage(FATAL, INSTANCES, instantiationId, TA1_REF));
        } else if (ta1ref.equals(NONE)) {
            // (TA2): Error if ta1ref is "none".
            instanceErrors.add(KsfValidationError.constructInvalidValueMessage(FATAL, INSTANCES, instantiationId, TA1_REF, ta1ref));
        } else {
            instanceErrors.addAll(validateTa1RefNaming(FATAL, INSTANCES, instantiationId, ta1ref));
        }
        // (TA2): Error if any required elements are missing: confidence.
        Float confidence = isNullOrEmptyList(instance.getConfidence()) ? null : instance.getConfidence().get(0);
        if (confidence == null) {
            instanceErrors.add(KsfValidationError.constructMissingValueMessage(INSTANCES, instantiationId, CONFIDENCE));
        } else if (!KsfFieldValuesValidation.isValidConfidenceValue(confidence)) {
            // Error if confidence is not a float between 0 and 1.0.
            instanceErrors.add(KsfValidationError.constructInvalidValueMessage(INSTANCES, instantiationId, CONFIDENCE, confidence));
        }

        return instanceErrors;
    }

    /**
     * Error if any required elements are missing: @id.
     * Error if @ids do not follow naming convention.
     * Fatal if there are any duplicate 5-digit IDs within the @ids of the array of entities objects.
     */
    private List<String> validateEntities(List<SchemaEntity> entities, String instantiationId) {
        List<String> entityErrors = new ArrayList<>();

        if (isNullOrEmptyList(entities)) {
            return entityErrors; // allow null for now; can be required for ta1/ta2, task1/task2
        }

        int numEntities = 0;
        for (SchemaEntity curEntity : entities) {
            String entityIdentifier = curEntity.getAtId();
            // Require @id
            if (StringUtils.isEmpty(entityIdentifier)) {
                entityIdentifier = String.format("'%s'.entities[%d]", instantiationId, numEntities);
                entityErrors.add(KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, JSON_LD_ID));
            } else {
                entityIds.add(entityIdentifier);
                if (isNonEmptyList(curEntity.getWdNode())) {
                    idsWithQnodes.add(entityIdentifier);
                }
            }
            numEntities++;
            entityErrors.addAll(validateEntity(curEntity, entityIdentifier));

            if (isTA2) {
                entityErrors.addAll(validateTA2Entity(curEntity, entityIdentifier));
            } else {
                entityErrors.addAll(validateTA1Entity(curEntity, entityIdentifier));
            }
        } // entity

        // Error if @ids do not follow naming convention.
        entityErrors.addAll(validateIdNaming(ENTITIES, instantiationId, JSON_LD_ID, entityIds));
        // Fatal if there are any duplicate 5-digit IDs within the @ids of the array of entities objects.
        Set<String> duplicateIds = getDuplicateStrings(idMap.get(ENTITIES));
        if (!duplicateIds.isEmpty()) {
            entityErrors.add(KsfValidationError.constructDuplicateIdsMessage(ENTITY,
                    instantiationId, isTA2 ? INSTANCES : DOCUMENT, duplicateIds));
        }

        return entityErrors;
    }

    /**
     * Error if any required elements are missing: name.
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Error if certain keywords contain invalid URIs.
     * Warning that empty wd_label/ta2wd_label values are ignored.
     */
    private List<String> validateEntity(SchemaEntity curEntity, String entityIdentifier) {
        List<String> entityErrors = new ArrayList<>();
        // Require name
        if (StringUtils.isBlank(curEntity.getName())) {
            entityErrors.add(KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, NAME));
        }

        // Warning that empty wd_label/ta2wd_label values are ignored.
        if (removeEmptyValues(curEntity.getWdLabel())) {
            entityErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(ENTITY, entityIdentifier, WD_LABEL));
        }
        if (removeEmptyValues(curEntity.getTa2wdLabel())) {
            entityErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(ENTITY, entityIdentifier, TA2WD_LABEL));
        }

        // Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(curEntity.getWdNode(), curEntity.getWdLabel(), curEntity.getWdDescription())) {
            entityErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(ENTITY, entityIdentifier,
                    WD_NODE, WD_LABEL, WD_DESCRIPTION));
        }

        // Error if certain keywords contain invalid URIs.
        entityErrors.addAll(validateURIs(ENTITY, entityIdentifier, WD_NODE, curEntity.getWdNode()));
        entityErrors.addAll(validateURIs(ENTITY, entityIdentifier, TA2WD_NODE, curEntity.getTa2wdNode()));
        entityErrors.addAll(validateURIs(ENTITY, entityIdentifier, REFERENCE, curEntity.getReference()));

        return entityErrors;
    }

    /**
     * (TA1): Error if centrality (when present) is not a float between 0 and 1.0.
     * (TA1): Error if any required elements are missing: wd_node, wd_label, and wd_description.
     * (TA1): Error if ta2wd_node, ta2wd_label, or ta2wd_description is present.
     */
    private List<String> validateTA1Entity(SchemaEntity curEntity, String entityIdentifier) {
        List<String> ta1EntityErrors = new ArrayList<>();

        Float centralityVal = curEntity.getCentrality();
        // (TA1): Error if centrality (when present) is not a float between 0 and 1.0.
        if (centralityVal != null && !KsfFieldValuesValidation.isValidCentralityValue(centralityVal)) {
            ta1EntityErrors.add(KsfValidationError.constructInvalidValueMessage(ENTITY, entityIdentifier, CENTRALITY, centralityVal));
        }

        // (TA1): Error if any required elements are missing: wd_node, wd_label, and wd_description.
        if (isNullOrEmptyList(curEntity.getWdLabel())) {
            ta1EntityErrors.add(KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_LABEL));
        }
        if (isNullOrEmptyList(curEntity.getWdNode())) {
            ta1EntityErrors.add(KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_NODE));
        }
        if (isNullOrEmptyList(curEntity.getWdDescription())) {
            ta1EntityErrors.add(KsfValidationError.constructMissingValueMessage(ENTITY, entityIdentifier, WD_DESCRIPTION));
        }
        // (TA1): Error if ta2wd_node, ta2wd_label, or ta2wd_description is present.
        if (isNonEmptyList(curEntity.getTa2wdNode())) {
            ta1EntityErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_NODE));
        }
        if (isNonEmptyList(curEntity.getTa2wdLabel())) {
            ta1EntityErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_LABEL));
        }
        if (isNonEmptyList(curEntity.getTa2wdDescription())) {
            ta1EntityErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(ENTITY, entityIdentifier, TA2WD_DESCRIPTION));
        }

        return ta1EntityErrors;
    }

    /**
     * (TA2): Error if both wd_node and ta2wd_node are present.
     * (TA2): Error if both wd_node and ta2wd_node are missing.
     * (TA2): Error if both wd_label and ta2wd_label are missing.
     * (TA2): Error if both wd_description and ta2wd_description are missing.
     * (TA2): Error if there is a mismatch between the number of ta2wd_node, ta2wd_label, and ta2wd_description values.
     */
    private List<String> validateTA2Entity(SchemaEntity entity, String entityIdentifier) {
        List<String> entityErrors = new ArrayList<>();

        boolean hasQnode = isNonEmptyList(entity.getWdNode());
        boolean hasTA2qnode = isNonEmptyList(entity.getTa2wdNode());
        // (TA2): Error if both wd_node and ta2wd_node are present.
        if (hasQnode && hasTA2qnode) {
            entityErrors.add(KsfValidationError.constructBothKeywordsPresentMessage(ENTITY, WD_NODE, TA2WD_NODE, entityIdentifier));
        } else if (!hasQnode && !hasTA2qnode) {
            // (TA2): Error if both wd_node and ta2wd_node are missing.
            entityErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_NODE, TA2WD_NODE));
        }

        // (TA2): Error if both wd_label and ta2wd_label are missing.
        if (isNullOrEmptyList(entity.getWdLabel()) && isNullOrEmptyList(entity.getTa2wdLabel())) {
            entityErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_LABEL, TA2WD_LABEL));
        }

        // (TA2): Error if both wd_description and ta2wd_description are missing.
        if (isNullOrEmptyList(entity.getWdDescription()) && isNullOrEmptyList(entity.getTa2wdDescription())) {
            entityErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(ENTITY, entityIdentifier, WD_DESCRIPTION, TA2WD_DESCRIPTION));
        }

        // (TA2): Error if there is a mismatch between the number of ta2wd_node, ta2wd_label, and ta2wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(entity.getTa2wdNode(), entity.getTa2wdLabel(), entity.getTa2wdDescription())) {
            entityErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(ENTITY, entityIdentifier,
                    TA2WD_NODE, TA2WD_LABEL, TA2WD_DESCRIPTION));
        }

        return entityErrors;
    }

    /**
     * Fatal if there are any duplicate 5-digit IDs within the @ids of the array of events objects.
     * Error if @ids do not follow naming convention.
     * @param events an array of events
     * @param instantiationId a String identifier for the instantiation containing the events
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateEvents(List<SchemaEvent> events, String instantiationId) {
        List<String> eventErrors = new ArrayList<>();

        if (isNullOrEmptyList(events)) {
            return eventErrors;
        }

        // This needs to be done prior to event validations because of dependencies with other checks
        for (SchemaEvent curEvent : events) {
            String eventId = curEvent.getAtId();
            if (!StringUtils.isEmpty(eventId)) {
                eventIds.add(eventId);
                if (isNonEmptyList(curEvent.getWdNode())) {
                    idsWithQnodes.add(eventId);
                }
            }
        }

        // Error if @ids do not follow naming convention.
        eventErrors.addAll(validateIdNaming(EVENTS, instantiationId, JSON_LD_ID, eventIds));
        // Fatal if there are any duplicate 5-digit IDs within the @ids of the array of events objects.
        Set<String> duplicateIds = getDuplicateStrings(idMap.get(EVENTS));
        if (!duplicateIds.isEmpty()) {
            eventErrors.add(KsfValidationError.constructDuplicateIdsMessage(EVENTS,
                    instantiationId, isTA2 ? INSTANCES : DOCUMENT, duplicateIds));
        }

        // Validate each event
        for (int i = 0; i < events.size(); i++) {
            eventErrors.addAll(validateEvent(events.get(i), i, instantiationId));
        }

        if (isTA2 && duplicateIds.isEmpty()) {
            boolean foundIssues = false;
            // Validate outlinks graph if there are no duplicate event IDs (otherwise don't bother)
            foundIssues = eventErrors.addAll(validateOutlinkGraph(events, instantiationId));
            // Validate TA2 hierarchy for structural consistency
            foundIssues = eventErrors.addAll(validateTA2Hierarchy(events, instantiationId)) || foundIssues;
            if (!foundIssues) { // bypass outlink-temporal conflicts if there are outlink or hierarchy issues
                eventErrors.addAll(checkOutlinkTemporalConflicts(events, instantiationId));
            }
        }

        return eventErrors;
    }

    // Warn if temporal values contradict outlinks:
    // Event A outlinks to Event B, but A.earliestStartTime > B.latestEndTime
    // Event A outlinks to Event B, but A.absoluteTime > B.latestEndTime
    private List<String> checkOutlinkTemporalConflicts(List<SchemaEvent> events, String instantiationId) {
        List<String> conflictErrors = new ArrayList<>();

        int eventIndex = 0;
        for (SchemaEvent event : events) {
            String eventIndexStr = String.format(INDEXED_ARRAY_FORMAT, instantiationId, EVENT, eventIndex++);
            String eventIdentifier = getFirstNonBlankString(Arrays.asList(event.getAtId(), event.getName(), eventIndexStr));
            if (isNullOrEmptyList(event.getOutlinks()) || isNullOrEmptyList(event.getTemporal())) {
                continue;
            }

            String eventEarliestStartTime = null;
            String eventAbsoluteTime = null;
            for (Temporal curTemporal : event.getTemporal()) {
                if (!StringUtils.isBlank(curTemporal.getEarliestStartTime())) {
                    eventEarliestStartTime = curTemporal.getEarliestStartTime();
                }
                if (!StringUtils.isBlank(curTemporal.getAbsoluteTime())) {
                    eventAbsoluteTime = curTemporal.getEarliestStartTime();
                }
            }

            String outlinkLatestEndTime = null;
            for (String outlink : event.getOutlinks()) {
                SchemaEvent outlinkedEvent = getEventById(events, outlink);
                if (outlinkedEvent != null && isNonEmptyList(outlinkedEvent.getTemporal())) {
                    for (Temporal curTemporal : outlinkedEvent.getTemporal()) {
                        if (!StringUtils.isBlank(curTemporal.getLatestEndTime())) {
                            outlinkLatestEndTime = curTemporal.getLatestEndTime();
                        }
                    }

                    // Finally, check for the conflicts
                    conflictErrors.addAll(checkTemporalConflict(eventEarliestStartTime, eventAbsoluteTime, outlinkLatestEndTime, eventIdentifier));
                }
            }
        }

        return conflictErrors;
    }

    // Warn if temporal values contradict outlinks.
    private List<String> checkTemporalConflict(String eventEarliestStartTime, String eventAbsoluteTime, String outlinkLatestEndTime, String eventId) {
        List<String> conflictErrors = new ArrayList<>();

        // Make sure we have valid times before proceeding.
        LocalDateTime earliestStartTime = safeParseLocalDateTime(eventEarliestStartTime);
        LocalDateTime latestEndTime = safeParseLocalDateTime(outlinkLatestEndTime);
        LocalDateTime absoluteTime = safeParseLocalDateTime(eventAbsoluteTime);

        if (isValidLocalDateTime(latestEndTime)) {
            // Event A outlinks to Event B, but A.earliestStartTime > B.latestEndTime
            if (isValidLocalDateTime(earliestStartTime) && earliestStartTime.compareTo(latestEndTime) > 0) {
                conflictErrors.add(KsfValidationError.constructOutlinkTemporalMismatchMessage(eventId, EARLIEST_START_TIME, LATEST_END_TIME));
            }
            // Event A outlinks to Event B, but A.absoluteTime > B.latestEndTime
            if (isValidLocalDateTime(absoluteTime) && absoluteTime.compareTo(latestEndTime) > 0) {
                conflictErrors.add(KsfValidationError.constructOutlinkTemporalMismatchMessage(eventId, ABSOLUTE_TIME, LATEST_END_TIME));
            }
        }

        return conflictErrors;
    }

    /**
     * Error if there is no event with isTopLevel set to "true", or if more than one event has isTopLevel set to "true".
     * Fatal if there is an inconsistency between parent and subgroup_events keywords.
     * Fatal if an event specifies itself as its parent.
     * Error if subgroup_events specifies the @id of an event with isTopLevel set to "true".
     * Error if outlinks specifies a non-sibling event (i.e., an event with a different parent).
     * Error if outlinks is specified for an event whose parent event specifies a children_gate of XOR.
     * Warning if provenance is provided for more than one child event from a parent with XOR children_gate.
     * Warning if hierarchy contains multiple primitive events at the root level of the hierarchy.
     * @param events all events in the instantiation
     * @param instantiationId an identifier for the instantiation
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTA2Hierarchy(List<SchemaEvent> events, String instantiationId) {
        List<String> hierarchyErrors = new ArrayList<>();

        int numTopLevelEvents = 0;
        int eventIndex = 0;
        for (SchemaEvent event : events) {
            numTopLevelEvents += processEventHierarchy(events, event, eventIndex++, hierarchyErrors, instantiationId);
        } // for events

        // Error if there is no event with isTopLevel set to "true", or if more than one event has isTopLevel set to "true".
        if (numTopLevelEvents != 1) {
            hierarchyErrors.add(KsfValidationError.constructExactlyOneTopLevelEventsMessage(instantiationId));
            return hierarchyErrors;
        }
        // Warning if hierarchy contains multiple primitive events at the root level of the hierarchy.
        int numPrimitivesAtRoot = 0;
        for (SchemaEvent event : events) {
            if (isNullOrEmptyList(event.getSubgroupEvents())) {
                String parentId = safeGetString(event.getParent());
                SchemaEvent parentEvent = getEventById(events, parentId);
                if (parentEvent != null && parentEvent.getIsTopLevel() != null && parentEvent.getIsTopLevel()) {
                    numPrimitivesAtRoot++;
                }
            }
        }
        if (numPrimitivesAtRoot >= MAX_TOP_LEVEL_PRIMITIVES) {
            hierarchyErrors.add(KsfValidationError.constructPrimitivesAtRootMessage(instantiationId, numPrimitivesAtRoot));
        }

        return hierarchyErrors;
    }

    /*
     * Fatal if there is an inconsistency between parent and subgroup_events keywords.
     * Error if outlinks specifies a non-sibling event (i.e., an event with a different parent).
     * Error if outlinks is specified for an event whose parent event specifies a children_gate of XOR.
     */
    private int processEventHierarchy(List<SchemaEvent> events, SchemaEvent event, int eventIndex, List<String> hierarchyErrors,
            String instantiationId) {

        List<String> children = safeGetStrings(event.getSubgroupEvents());
        String eventIndexStr = String.format(INDEXED_ARRAY_FORMAT, instantiationId, EVENT, eventIndex);
        String eventId = safeGetString(event.getAtId());
        String eventIdentifier = getFirstNonBlankString(Arrays.asList(eventId, event.getName(), eventIndexStr));
        String parentId = safeGetString(event.getParent());
        boolean isTopLevel = event.getIsTopLevel() != null && event.getIsTopLevel();
        boolean foundProvenance = false;
        for (String childId : children) {
            SchemaEvent child = getEventById(events, childId);
            if (child != null) {
                foundProvenance = processChildHierarchy(event, eventId, eventIdentifier, child, childId, foundProvenance, hierarchyErrors);
            }
        } // for children

        // Fatal if there is an inconsistency between parent and subgroup_events keywords. (check #2)
        SchemaEvent parentEvent = getEventById(events, parentId);
        if (parentEvent != null) {
            List<String> parentChildren = safeGetStrings(parentEvent.getSubgroupEvents());
            if (!parentChildren.contains(eventId)) {
                hierarchyErrors.add(KsfValidationError.constructOrphanedChildMessage(eventIdentifier, parentId));
            }
        }

        List<String> outlinks = safeGetStrings(event.getOutlinks());
        for (String outlinkId : outlinks) {
            SchemaEvent outlinkEvent = getEventById(events, outlinkId);
            if (outlinkEvent == null) {
                continue; // invalid outlink
            }
            // Error if outlinks specifies a non-sibling event (i.e., an event with a different parent).
            String outlinkParentId = safeGetString(outlinkEvent.getParent());
            if (!outlinkParentId.equals(parentId)) {
                hierarchyErrors.add(KsfValidationError.constructNonSiblingOutlinkMessage(eventIdentifier, outlinkId));
            }
        } // for outlinks

        // Error if outlinks is specified for an event whose parent event specifies a children_gate of XOR.
        if (!outlinks.isEmpty() && (parentEvent != null && XOR_GATE.equalsIgnoreCase(parentEvent.getChildrenGate()))) {
            hierarchyErrors.add(KsfValidationError.constructUnsupportedXOROutlinksMessage(eventId, parentId));
        }
        return isTopLevel ? 1 : 0;
    }

    /*
     * Fatal if there is an inconsistency between parent and subgroup_events keywords.
     * Fatal if an event specifies itself as its parent.
     * Error if subgroup_events specifies the @id of an event with isTopLevel set to "true".
     * Warning if provenance is provided for more than one child event from a parent with XOR children_gate.
     */
    private boolean processChildHierarchy(SchemaEvent event, String eventId, String eventIdentifier, SchemaEvent child,
            String childId, boolean foundProvenance, List<String> hierarchyErrors) {
        // Fatal if an event specifies itself as its parent.
        if (childId.equals(eventId)) {
            hierarchyErrors.add(KsfValidationError.constructSameValueMessage(FATAL, EVENT, eventId, PARENT, JSON_LD_ID));
            return foundProvenance;
        }

        // Error if subgroup_events specifies the @id of an event with isTopLevel set to "true".
        boolean childIsTopLevel = child.getIsTopLevel() != null && child.getIsTopLevel();
        if (childIsTopLevel) {
            hierarchyErrors.add(KsfValidationError.constructChildIsTopLevelMessage(eventIdentifier, childId));
        }
        // Fatal if there is an inconsistency between parent and subgroup_events keywords. (check #1)
        String childParentId = safeGetString(child.getParent());
        if (!childParentId.equals(eventId)) {
            hierarchyErrors.add(KsfValidationError.constructRunawayChildMessage(eventIdentifier, childId));
        }
        // Warning if provenance is provided for more than one child event from a parent with XOR children_gate.
        if (XOR_GATE.equalsIgnoreCase(event.getChildrenGate()) && (!safeGetFirstString(child.getProvenance()).isBlank())) {
            if (foundProvenance) {
                hierarchyErrors.add(KsfValidationError.constructMultipleXorProvenanceMessage(event.getAtId()));
            }
            foundProvenance = true;
        }

        return foundProvenance;
    }

    /**
     * Error if any required elements are missing: @id, description, and name.
     * Error if children_gate (when present) does not match the controlled vocabulary: and, or, or xor (case-insensitive).
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Error if confidence (when present) is not a float between 0 and 1.0.
     * Error if both subgroup_events and participants are missing.
     * Error if modality (when present) does not match the controlled vocabulary: generic, hedged, irrealis, or negated.
     * Error if certain keywords contain invalid URIs.
     * Warning that empty wd_label/ta2wd_label values are ignored.
     * (TA1): Error if instanceOf does not match the @id of an events object.
     * (TA1): Error if children is present but children_gate is missing.
     * (TA2): Error if subgroup_events is present but children_gate is missing.
     * (TA1): Error if both children and wd_node are missing.
     * (TA2): Error if both subgroup_events is missing and either wd_node is missing, or ta2wd_node is missing and ta1ref is "none".
     * @param event a SchemaEvent to be validated
     * @param eventIndex the index in the events array of the specified event
     * @param instantiationId a String identifier for the instantiation containing the events
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateEvent(SchemaEvent event, int eventIndex, String instantiationId) {
        List<String> eventErrors = new ArrayList<>();

        String eventAtId = safeGetString(event.getAtId());
        String eventName = safeGetString(event.getName());
        String eventIndexStr = String.format(INDEXED_ARRAY_FORMAT, instantiationId, EVENT, eventIndex);
        String eventIdentifier = getFirstNonBlankString(Arrays.asList(eventAtId, eventName, eventIndexStr));

        // Error if any required elements are missing: @id, description, and name.
        if (eventAtId.isEmpty()) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, JSON_LD_ID));
        }
        if (safeGetString(event.getDescription()).isBlank()) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, DESCRIPTION));
        }
        if (eventName.isBlank()) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, NAME));
        }

        // Error if confidence (when present) is not a float between 0 and 1.0.
        if (isNonEmptyList(event.getConfidence())) {
            for (Float confidence : event.getConfidence()) {
                if (!KsfFieldValuesValidation.isValidConfidenceValue(confidence)) {
                    eventErrors.add(KsfValidationError.constructInvalidValueMessage(EVENT, eventAtId, CONFIDENCE, confidence));
                }
            }
        }

        // Error if modality (when present) does not match the controlled vocabulary: generic, hedged, irrealis, or negated.
        eventErrors.addAll(KsfFieldValuesValidation.validateModalityValues(EVENT, event.getModality(), eventIdentifier));

        // Warning that empty wd_label/ta2wd_label values are ignored.
        if (removeEmptyValues(event.getWdLabel())) {
            eventErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(EVENT, eventIdentifier, WD_LABEL));
        }
        if (removeEmptyValues(event.getTa2wdLabel())) {
            eventErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(EVENT, eventIdentifier, TA2WD_LABEL));
        }

        // Error if certain keywords contain invalid URIs
        eventErrors.addAll(validateURIs(EVENT, eventIdentifier, WD_NODE, event.getWdNode()));
        eventErrors.addAll(validateURIs(EVENT, eventIdentifier, TA2WD_NODE, event.getTa2wdNode()));
        eventErrors.addAll(validateURIs(EVENT, eventIdentifier, REFERENCE, event.getReference()));
        eventErrors.addAll(validateURIs(EVENT, eventIdentifier, SUBGROUP_EVENTS, event.getSubgroupEvents()));
        eventErrors.addAll(validateURIs(EVENT, eventIdentifier, PARENT, event.getParent()));

        boolean hasQnode = isNonEmptyList(event.getWdNode());
        boolean hasTA2Qnode = isNonEmptyList(event.getTa2wdNode());
        boolean hasChildren = isTA2 ? isNonEmptyList(event.getSubgroupEvents()) : isNonEmptyList(event.getChildren());
        boolean hasChildrenGate = !StringUtils.isEmpty(event.getChildrenGate());

        // Error if both subgroup_events and participants are missing.
        if (!hasChildren && isNullOrEmptyList(event.getParticipants())) {
            eventErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, PARTICIPANTS,
                    isTA2 ? SUBGROUP_EVENTS : CHILDREN));
        }

        // (TA2): Error if both subgroup_events is missing and either wd_node is missing, or ta2wd_node is missing and ta1ref is "none".
        if (isTA2 && !hasChildren) {
            if (NONE.equals(event.getTa1ref())) {
                if (!hasTA2Qnode) {
                    eventErrors.add(KsfValidationError.constructBothKeywordsMissingMessage("source-only event", eventIdentifier, TA2WD_NODE, SUBGROUP_EVENTS));
                }
            } else if (!hasQnode) {
                eventErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, WD_NODE, SUBGROUP_EVENTS));
            }
        }

        // Error if children/subgroup_events is present but children_gate is missing.
        if (hasChildren && !hasChildrenGate) {
            eventErrors.add(KsfValidationError.constructHasKeywordButNotOtherKeywordMessage(EVENT, eventIdentifier,
                    isTA2 ? SUBGROUP_EVENTS : CHILDREN, CHILDREN_GATE));
        }

        // Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(event.getWdNode(), event.getWdLabel(), event.getWdDescription())) {
            eventErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier,
                    WD_NODE, WD_LABEL, WD_DESCRIPTION));
        }

        // Error if children_gate (when present) does not match the controlled vocabulary: and, or, or xor (case-insensitive).
        if (hasChildrenGate) {
            eventErrors.addAll(KsfFieldValuesValidation.validateChildrenGateValues(event.getChildrenGate(), eventIdentifier));
        }

        // Validate event's children, participants, and relations (if any)
        if (hasChildren) {
            eventErrors.addAll(isTA2 ?
                    validateTA2Children(event, eventIdentifier) : validateTA1Children(event, eventIdentifier));
        }
        eventErrors.addAll(validateParticipants(event, eventIdentifier));
        eventErrors.addAll(validateRelations(event.getRelations(), eventIdentifier));

        if (isTA2) {
            eventErrors.addAll(validateTA2Event(event, eventIdentifier));
        } else {
            // (TA1) Error if instanceOf does not match the @id of an events object.
            String instanceOf = safeGetString(event.getInstanceOf());
            if (!instanceOf.isEmpty() && !eventIds.contains(instanceOf)) {
                eventErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, INSTANCE_OF, instanceOf));
            }

            // (TA1): Error if both children and wd_node are missing.
            if (!hasChildren && !hasQnode) {
                eventErrors.add(KsfValidationError.constructBothKeywordsMissingMessage(EVENT, eventIdentifier, WD_NODE, CHILDREN));
            }
        }

        return eventErrors;
    }

    /**
     * Error if subgroup_events does not match the @id of an events object.
     * Warning if subgroup_events contains more than 7 elements.
     * @param event a SchemaEvent whose children (subgroup_events) are to be validated
     * @param eventIdentifier a String identifier for the event
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTA2Children(SchemaEvent event, String eventIdentifier) {
        List<String> childErrors = new ArrayList<>();
        List<String> children = safeGetStrings(event.getSubgroupEvents());

        // Error if subgroup_events does not match the @id of an events object.
        for (String childId : children) {
            if (!eventIds.contains(childId)) {
                childErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, SUBGROUP_EVENTS, childId));
            }
        }

        // Warning if subgroup_events contains more than 7 elements.
        if (children.size() > MAX_CHILDREN) {
            childErrors.add(KsfValidationError.constructTooManyValuesMessage(WARNING, EVENT, eventIdentifier,
                    SUBGROUP_EVENTS, MAX_CHILDREN));
        }
        return childErrors;
    }

    /**
     * Error if the graph expressed by outlinks is not the transitive reduction of the event order graph.
     * Fatal if the graph expressed by outlinks contains any cycles (e.g. circular references).
     * @param events all events in the instantiation
     * @param instantiationId an identifier for the instantiation
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateOutlinkGraph(List<SchemaEvent> events, String instantiationId) {
        List<String> outlinkGraphErrors = new ArrayList<>();

        // Error if the graph expressed by outlinks contains any cycles (e.g. circular references).
        if (GraphTraversal.getHasCycles(events, eventIds)) {
            outlinkGraphErrors.add(KsfValidationError.constructCircularOutlinksMessage(instantiationId));
        } else if (!GraphTraversal.isTransitiveReduction(events, eventIds)) {
            // Error if the graph expressed by outlinks is not the transitive reduction of the event order graph.
            outlinkGraphErrors.add(KsfValidationError.constructNotTransitiveReductionMessage(instantiationId));
        }

        return outlinkGraphErrors;
    }

    /**
     * Error if any required elements are missing: child.
     * Error if child does not match the @id of an events object.
     * Error if importance (when present) is not a float between 0 and 1.0.
     * @param event a SchemaEvent whose children are to be validated
     * @param eventIdentifier a String identifier for the event
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTA1Children(SchemaEvent event, String eventIdentifier) {
        List<String> childErrors = new ArrayList<>();

        List<Child> children = event.getChildren();
        if (isNullOrEmptyList(children)) {
            return childErrors;
        }

        int numChildren = 0;
        for (Child childObj : children) {
            String objectIdentifier = eventIdentifier + "[" + numChildren + "]";
            // Error if any required elements are missing: child.
            String child = childObj.getChild();
            if (StringUtils.isBlank(child)) {
                childErrors.add(KsfValidationError.constructMissingValueMessage(CHILD, objectIdentifier, CHILD));
            } else {
                // Error if child does not match the @id of an events object.
                if (!eventIds.contains(child)) {
                    childErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, CHILD, child));
                }
            }

            // Error if importance (when present) is not a float between 0 and 1.0.
            Float importanceVal = childObj.getImportance();
            if (importanceVal != null && !KsfFieldValuesValidation.isValidImportanceValue(importanceVal)) {
                childErrors.add(KsfValidationError.constructInvalidValueMessage(CHILD, objectIdentifier, IMPORTANCE, importanceVal));
            }
            numChildren++;
        }

        return childErrors;
    }

    /**
     * Error if any required elements are missing: @id, entity, roleName.
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Error if two participants share the same values for both roleName and entity.
     * Error if entity does not match the @id of an entities or events object, unless it is set to kairos:NULL (TA2).
     * Error if certain keywords contain invalid URIs.
     * Warning that empty wd_label values are ignored.
     * (TA2): Error if entity is not "kairos:NULL" and the @id pointed to by entity does not have a qnode.
     * @param event a SchemaEvent whose participants are to be validated
     * @param eventIdentifier a String identifier for the event
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateParticipants(SchemaEvent event, String eventIdentifier) {
        List<String> participantErrors = new ArrayList<>();

        List<Participant> participants = event.getParticipants();
        if (isNullOrEmptyList(participants)) {
            return participantErrors;
        }

        int numParticipants = 0;
        HashSet<String> flaggedRoles = new HashSet<>(); // avoid duplicate errors in roleName/entity checks
        for (Participant participant : participants) {
            String participantIdentifier;

            // Error if any required elements are missing: @id
            String participantAtId = participant.getAtId();
            if (StringUtils.isEmpty(participantAtId)) {
                participantIdentifier = String.format(INDEXED_ARRAY_FORMAT, eventIdentifier, PARTICIPANT, numParticipants);
                participantErrors.add(KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, JSON_LD_ID));
            } else {
                participantIdentifier = participantAtId;
                participantIds.add(participantAtId); // For checking duplicate participant @ids at the instantiation level
            }
            numParticipants++;

            String entity = participant.getEntity();
            participantErrors.addAll(validateParticipantAttributes(participant, participantIdentifier, entity));

            // Error if two participants share the same values for both roleName and entity.
            participantErrors.addAll(validateParticipantRoles(participant, participants, entity, eventIdentifier, flaggedRoles));

            if (isTA2) {
                participantErrors.addAll(validateFillers(participant.getValues(), participantIdentifier));
                participantErrors.addAll(validateTA2Participant(entity, participantIdentifier));
            } else {
                // Error if entity does not match the @id of an entities or events object.
                if (!StringUtils.isBlank(entity) && !entityIds.contains(entity) && !eventIds.contains(entity)) {
                    participantErrors.add(KsfValidationError.constructInvalidIdMessage(PARTICIPANT, participantIdentifier, ENTITY, entity));
                }
            }
        } // each participant

        return participantErrors;
    }


    /**
     * Error if entity does not match the @id of an entities or events object, unless it is set to kairos:NULL (TA2).
     * (TA2): Error if entity is not "kairos:NULL" and the @id pointed to by entity does not have a qnode.
     */
    private List<String> validateTA2Participant(String entity, String participantIdentifier) {
        List<String> participantErrors = new ArrayList<>();

        if (!StringUtils.isBlank(entity) && !KAIROS_NULL_UNEXPANDED.equals(entity)) {
            // Error if entity does not match the @id of an entities or events object, unless it is set to kairos:NULL (TA2).
            if (!entityIds.contains(entity) && !eventIds.contains(entity)) {
                participantErrors.add(KsfValidationError.constructInvalidIdMessage(PARTICIPANT, participantIdentifier, ENTITY, entity));
            }
            // (TA2): Error if entity is not "kairos:NULL" and the @id pointed to by entity does not have a qnode.
            if (!idsWithQnodes.contains(entity)) {
                participantErrors.add(KsfValidationError.constructIdMissingKeywordMessage(PARTICIPANT, participantIdentifier, ENTITY, entity, WD_NODE));
            }
        }

        return participantErrors;
    }

    /**
     * Error if any required elements are missing: entity, roleName.
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Error if certain keywords contain invalid URIs.
     * Warning that empty wd_label values are ignored.
     */
    private List<String> validateParticipantAttributes(Participant participant, String participantIdentifier, String entity) {
        List<String> participantErrors = new ArrayList<>();

        // Error if any required elements are missing: entity.
        if (StringUtils.isBlank(entity)) {
            participantErrors.add(KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, ENTITY));
        }

        // Error if any required elements are missing: roleName.
        if (StringUtils.isBlank(participant.getRoleName())) {
            participantErrors.add(KsfValidationError.constructMissingValueMessage(PARTICIPANTS, participantIdentifier, ROLE_NAME));
        }

        // Warning that empty wd_label values are ignored.
        if (removeEmptyValues(participant.getWdLabel())) {
            participantErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(PARTICIPANT, participantIdentifier, WD_LABEL));
        }

        // Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(participant.getWdNode(), participant.getWdLabel(), participant.getWdDescription())) {
            participantErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(PARTICIPANT, participantIdentifier,
                    WD_NODE, WD_LABEL, WD_DESCRIPTION));
        }

        // Error if certain keywords contain invalid URIs
        participantErrors.addAll(validateURIs(PARTICIPANT, participantIdentifier, REFERENCE, participant.getReference()));
        participantErrors.addAll(validateURIs(PARTICIPANT, participantIdentifier, WD_NODE, participant.getWdNode()));

        return participantErrors;
    }

    /*
     * Error if two participants share the same values for both roleName and entity.
     * NOTE: Currently only reports one infraction per roleName, even if there are multiple duplicate entities.
     */
    private List<String> validateParticipantRoles(Participant participant, List<Participant> participants, String entity,
            String eventIdentifier, HashSet<String> flaggedRoles) {

        List<String> participantErrors = new ArrayList<>();
        for (Participant participant2 : participants) {
            if (participant != participant2 && !StringUtils.isBlank(entity) &&
                    entity.equalsIgnoreCase(participant2.getEntity())) {
                // Same entity, so check roleName
                String roleName = participant.getRoleName();
                if (!StringUtils.isBlank(roleName) && !flaggedRoles.contains(roleName) &&
                        roleName.equalsIgnoreCase(participant2.getRoleName())) {
                    participantErrors.add(KsfValidationError.constructDuplicateParticipantMessage(
                            eventIdentifier, roleName, entity));
                    flaggedRoles.add(roleName);
                }
            }
        }

        return participantErrors;
    }

    /**
     * (TA2): Error if any required elements are missing: @id, confidence (Task 1), provenance, ta2entity.
     * (TA2): Error if confidence is not a float between 0 and 1.0.
     * (TA2): Error if modality (when present) does not match the controlled vocabulary: hedged or negated.
     * (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values.
     * (TA2 Task 1): Error if provenance does not match the provenanceID in a provenanceData object.
     * @param fillers a List of `Filler`s for the participant specified by `participantIdentifier`
     * @param participantIdentifier a String identifier for the participant to whom `fillers` belongs
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateFillers(List<Filler> fillers, String participantIdentifier) {
        List<String> fillerErrors = new ArrayList<>();

        if (isNullOrEmptyList(fillers)) {
            return fillerErrors;
        }

        int numFillers = 0;
        for (Filler filler : fillers) {
            String fillerIdentifier;
            // Error if any required elements are missing: @id
            String fillerAtId = filler.getAtId();
            if (StringUtils.isEmpty(fillerAtId)) {
                fillerIdentifier = String.format(INDEXED_ARRAY_FORMAT, participantIdentifier, VALUES, numFillers);
                fillerErrors.add(KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, JSON_LD_ID));
            } else {
                fillerIdentifier = fillerAtId;
                valuesIds.add(fillerAtId); // For checking duplicate values @ids at the instantiation level
            }
            numFillers++;

            List<String> provenanceList = safeGetStrings(filler.getProvenance());
            // (TA2): Error if any required elements are missing: provenance, ta2entity.
            if (isNullOrEmptyList(provenanceList)) {
                fillerErrors.add(KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, PROVENANCE));
            }
            if (StringUtils.isBlank(filler.getTa2entity())) {
                fillerErrors.add(KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, TA2ENTITY));
            }

            // (TA2): Error if any required elements are missing: confidence (Task 1)
            if (isNullOrEmptyList(filler.getConfidence())) {
                if (isTask1) {
                    fillerErrors.add(KsfValidationError.constructMissingValueMessage(VALUES, fillerIdentifier, CONFIDENCE));
                }
            } else {
                // Error if confidence is not a float between 0 and 1.0.
                for (Float confidence : filler.getConfidence()) {
                    if (!KsfFieldValuesValidation.isValidConfidenceValue(confidence)) {
                        fillerErrors.add(KsfValidationError.constructInvalidValueMessage(VALUES, fillerIdentifier, CONFIDENCE, confidence));
                    }
                }
            }

            // (TA2): Error if modality (when present) does not match the controlled vocabulary: hedged or negated.
            fillerErrors.addAll(KsfFieldValuesValidation.validateModalityValues(VALUES, filler.getModality(), fillerIdentifier));

            // (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values.
            // (TA2 Task 1): Error if provenance does not match the provenanceID in a provenanceData object.
            if (isTask1) {
                fillerErrors.addAll(KsfFieldValuesValidation.validateProvenanceValues(provenanceList, filler.getConfidence(),
                        provenanceIds, VALUES, fillerIdentifier, ERROR));
            } else {
                // Error if provenance is an invalid URI
                fillerErrors.addAll(validateURIs(VALUES, fillerIdentifier, PROVENANCE, provenanceList));
            }

            // Store for validating ta2entity later
            fillerObjs.add(filler);
        } // each filler

        return fillerErrors;
    }

    /**
     * (TA2): Error if ta2entity does not match the @id of an entities or events object.
     * (TA2): Error if the @id pointed to by ta2entity does not have a ta2wd_node.
     * Precondition: Entities, events, and relations have been processed
     * @param events a List of `SchemaEvent`s for an instantiation
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateFillerEntities(List<SchemaEvent> events, List<SchemaEntity> entities) {
        List<String> fillerErrors = new ArrayList<>();

        for (Filler filler : fillerObjs) {
            String ta2entity = filler.getTa2entity();
            if (StringUtils.isBlank(ta2entity)) {
                continue; // ta2entity was bad in the first place, so skip it
            }
            boolean found = false;
            boolean hasQnode = false;
            if (entities != null) {
                for (SchemaEntity entity : entities) {
                    if (ta2entity.equals(entity.getAtId())) {
                        found = true;
                        if (isNonEmptyList(entity.getTa2wdNode())) {
                            hasQnode = true;
                            break; // ta2entity links to an entity with a ta2qnode, so exit
                        }
                    }
                }
            }
            if (!found && isNonEmptyList(events)) {
                for (SchemaEvent event : events) {
                    if (ta2entity.equals(event.getAtId())) {
                        found = true;
                        if (isNonEmptyList(event.getTa2wdNode())) {
                            hasQnode = true;
                            break; // ta2entity links to an event with a ta2qnode, so exit
                        }
                    }
                }
            }

            if (!found) {
                // (TA2): Error if ta2entity does not match the @id of an entities or events object.
                fillerErrors.add(KsfValidationError.constructInvalidIdMessage(VALUES, filler.getAtId(), TA2ENTITY, ta2entity));
            } else if (!hasQnode) {
                // (TA2): Error if the @id pointed to by ta2entity does not have a ta2wd_node.
                fillerErrors.add(KsfValidationError.constructIdMissingKeywordMessage(VALUES, filler.getAtId(), TA2ENTITY, ta2entity, TA2WD_NODE));
            }
        } // each filler

        return fillerErrors;
    }

    /**
     * (TA2): Error if any required elements are missing: confidence (Task 1), parent, ta1ref.
     * (TA2): Error ta1ref is "none" but both subgroup_events and provenance are missing
     * (TA2): Error if ta1ref does not follow naming convention.
     * (TA2): Error if parent does not match the @id of an events object, unless isTopLevel is true and parent is "kairos:NULL".
     * (TA2): Error if outlink does not match the @id of an events object.
     * (TA2): Error if subgroup_events is missing and provenance and predictionProvenance are both missing or both present.
     * (TA2): Error if children is present.
     * (TA2): Error if provenance is present but no values are present in participants.
     * (TA2): Error if there is a mismatch between the number of ta2wd_node, ta2wd_label, and ta2wd_description values.
     * (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values.
     * (TA2 Task 1): Error if provenance does not match the provenanceID in a provenanceData object.
     * (TA2 Task 2): Error if predictionProvenance is present but confidence is missing.
     * @param event a SchemaEvent to be validated
     * @param eventIdentifier a String identifier for the event
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTA2Event(SchemaEvent event, String eventIdentifier) {
        List<String> eventErrors = new ArrayList<>();

        // (TA2): Error if any required elements are missing: parent.
        String parentId = safeGetString(event.getParent());
        if (StringUtils.isEmpty(parentId)) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, PARENT));
        } else {
            // (TA2): Error if parent does not match the @id of an events object, unless isTopLevel is true and parent is "kairos:NULL".
            if (!eventIds.contains(parentId) &&
                    !(event.getIsTopLevel() != null && event.getIsTopLevel() && parentId.equals(KAIROS_NULL_UNEXPANDED))) {
                eventErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, PARENT, parentId));
            }
        }

        // (TA2): Error if any required elements are missing: confidence (Task 1).
        if (isTask1 && isNullOrEmptyList(event.getConfidence())) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, CONFIDENCE));
        }

        // (TA2): Error if any required elements are missing: ta1ref.
        String ta1ref = safeGetString(event.getTa1ref());
        if (StringUtils.isEmpty(ta1ref)) {
            eventErrors.add(KsfValidationError.constructMissingValueMessage(EVENT, eventIdentifier, TA1_REF));
        } else {
            // (TA2): Error if ta1ref does not follow naming convention.
            eventErrors.addAll(validateTa1RefNaming(ERROR, EVENTS, eventIdentifier, ta1ref));
        }

        // (TA2): Error if subgroup_events is missing and provenance and predictionProvenance are both missing or both present.
        boolean isInstantiated = isNonEmptyList(event.getProvenance());
        boolean isPredicted = isNonEmptyList(event.getPredictionProvenance());
        boolean isPrimitive = isNullOrEmptyList(event.getSubgroupEvents());
        if (isPrimitive && ((isInstantiated && isPredicted) || (!isInstantiated && !isPredicted))) {
            eventErrors.add(KsfValidationError.constructExactlyOneRequiredMessage("primitive " + EVENT,
                    eventIdentifier, PROVENANCE, PREDICTION_PROVENANCE));
        }

        // (TA2): Error if there is a mismatch between the number of ta2wd_node, ta2wd_label, and ta2wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(event.getTa2wdNode(), event.getTa2wdLabel(), event.getTa2wdDescription())) {
            eventErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(EVENT, eventIdentifier,
                    TA2WD_NODE, TA2WD_LABEL, TA2WD_DESCRIPTION));
        }

        // (TA2): Error if children is present.
        if (isNonEmptyList(event.getChildren())) {
            eventErrors.add(KsfValidationError.constructUnsupportedKeywordMessage(EVENT, eventIdentifier, CHILDREN));
        }

        // (TA2): Error ta1ref is "none" but both subgroup_events and provenance are missing.
        if (isPrimitive && !isInstantiated && NONE.equals(ta1ref)) {
            eventErrors.add(KsfValidationError.constructProvenanceTa1refMessage(EVENT, eventIdentifier, PROVENANCE));
        }

        // (TA2): Error if outlink does not match the @id of an events object.
        List<String> outlinks = safeGetStrings(event.getOutlinks());
        for (String outlink : outlinks) {
            if (!eventIds.contains(outlink)) {
                eventErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, OUTLINKS, outlink));
            }
        }

        if (isInstantiated) {
            // (TA2): Error if provenance is present but no values are present in participants.
            boolean foundFiller = false;
            List<Participant> participants = event.getParticipants();
            if (isNonEmptyList(participants)) {
                for (Iterator<Participant> pIter = participants.iterator(); !foundFiller && pIter.hasNext();) {
                    Participant participant = pIter.next();
                    if (isNonEmptyList(participant.getValues())) {
                        foundFiller = true;
                    }
                }
                if (!foundFiller) {
                    eventErrors.add(KsfValidationError.constructMissingValueMessage("instantiated " + EVENT, eventIdentifier, VALUES));
                }
            }

            if (isTask1) {
                // (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values.
                // (TA2 Task 1): Error if provenance does not match the provenanceID in a provenanceData object.
                eventErrors.addAll(KsfFieldValuesValidation.validateProvenanceValues(event.getProvenance(),
                        event.getConfidence(), provenanceIds, EVENT, eventIdentifier, ERROR));
            } else {
                // Error if provenance is an invalid URI
                eventErrors.addAll(validateURIs(EVENT, eventIdentifier, PROVENANCE, event.getProvenance()));
            }
        }

        // (TA2 Task 2): Error if predictionProvenance is present but confidence is missing.
        if (!isTask1 && isPredicted && isNullOrEmptyList(event.getConfidence())) {
            eventErrors.add(KsfValidationError.constructHasKeywordButNotOtherKeywordMessage(EVENT,
                    eventIdentifier, PREDICTION_PROVENANCE, CONFIDENCE));
        }

        // Validate temporals
        eventErrors.addAll(validateTemporals(event.getTemporal(), eventIdentifier));

        return eventErrors;
    }

    /**
     * (TA1) Error if centrality (when present) is not a float between 0 and 1.0.
     * Error if any required elements are missing: @id, relationObject, relationSubject, wd_node, wd_label, and wd_description.
     * Error if wd_node contains more than one element.
     * Error if wd_node is from the temporal subset and relationSubject matches any relationObject.
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Error if relationObject or relationSubject does not match an @id of an element in the entities or events arrays.
     * Error if modality (when present) does not match the controlled vocabulary: hedged or negated.
     * Error if certain keywords contain invalid URIs.
     * Warning that empty wd_label values are ignored.
     * Warning if optional name element is missing.
     * @param relations an array of relations
     * @param ownerId a String identifier for the "owner" of the relations-- typically an event id or instantiation id
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateRelations(List<Relation> relations, String ownerId) {
        List<String> relationErrors = new ArrayList<>();

        if (isNullOrEmptyList(relations)) {
            return relationErrors;
        }

        int numRelations = 0;
        for (Relation curRelation : relations) {
            String relationIdentifier = curRelation.getAtId();
            // Error if any required elements are missing: @id
            if (StringUtils.isEmpty(relationIdentifier)) {
                relationIdentifier = String.format("'%s'.relation[%d]", ownerId, numRelations);
                relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, JSON_LD_ID));
            } else {
                // For checking duplicate relation @ids at the instantiation level
                relationIds.add(relationIdentifier);
            }
            numRelations++;

            // Error if any required elements are missing: relationSubject
            String relationSubject = safeGetString(curRelation.getRelationSubject());
            if (relationSubject.isBlank()) {
                relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_SUBJECT));
            } else {
                // Error if relationSubject does not match an @id of an element in the entities or events arrays.
                relationErrors.addAll(validateRelationArgumentIds(RELATION_SUBJECT, Collections.singletonList(relationSubject), relationIdentifier));
            }

            // Error if any required elements are missing: relationObject
            List<String> relationObjects = safeGetStrings(curRelation.getRelationObject());
            if (relationObjects.isEmpty()) {
                relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, RELATION_OBJECT));
            } else {
                // Error if relationObject does not match an @id of an element in the entities or events arrays.
                relationErrors.addAll(validateRelationArgumentIds(RELATION_OBJECT, relationObjects, relationIdentifier));
            }

            // Validate Wikidata-related fields
            relationErrors.addAll(validateRelationWikidata(curRelation, relationIdentifier, relationSubject, relationObjects));

            // Error if certain keywords contain invalid URIs
            relationErrors.addAll(validateURIs(RELATION, relationIdentifier, KairosSchemaFormatConstants.WD_NODE, curRelation.getWdNode()));
            relationErrors.addAll(validateURIs(RELATION, relationIdentifier, REFERENCE, curRelation.getReference()));

            // Error if modality (when present) does not match the controlled vocabulary: generic, hedged, irrealis, or negated.
            relationErrors.addAll(KsfFieldValuesValidation.validateModalityValues(RELATION, curRelation.getModality(), relationIdentifier));

            // Warning if optional name element is missing
            if (StringUtils.isBlank(curRelation.getName())) {
                relationErrors.add(KsfValidationError.constructMissingValueMessage(WARNING, RELATIONS, relationIdentifier, NAME));
            }

            if (isTA2) {
                relationErrors.addAll(validateTA2Relation(curRelation, relationIdentifier));
            } else {
                // (TA1) Error if centrality (when present) is not a float between 0 and 1.0.
                Float centralityVal = curRelation.getCentrality();
                if (centralityVal != null && !KsfFieldValuesValidation.isValidCentralityValue(centralityVal)) {
                    relationErrors.add(KsfValidationError.constructInvalidValueMessage(RELATION, relationIdentifier, CENTRALITY, centralityVal));
                }
            }
        } // each relation

        return relationErrors;
    }

    /*
     * Error if any required elements are missing: wd_node, wd_label, and wd_description.
     * Error if wd_node contains more than one element.
     * Error if wd_node is from the temporal subset and relationSubject matches any relationObject.
     * Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
     * Warning that empty wd_label values are ignored.
     */
    private List<String> validateRelationWikidata(Relation curRelation, String relationIdentifier, String relationSubject,
            List<String> relationObjects) {

        List<String> relationErrors = new ArrayList<>();
        @NotNull List<String> wdNode = curRelation.getWdNode();
        @NotNull List<String> wdLabel = curRelation.getWdLabel();
        @NotNull List<String> wdDescription = curRelation.getWdDescription();

        boolean isTemporal = TEMPORAL_SUBSET_IDS.contains(safeGetFirstString(wdNode));
        // Error if any required elements are missing: wd_node
        if (isNullOrEmptyList(wdNode)) {
            relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, KairosSchemaFormatConstants.WD_NODE));
        } else {
            if (wdNode.size() > 1) {
                // Error if wd_node contains more than one element.
                relationErrors.add(KsfValidationError.constructTooManyValuesMessage(RELATION, relationIdentifier, WD_NODE));
            }
            if (isTemporal) {
                // Error if wd_node is from the temporal subset and relationSubject matches any relationObject.
                for (String relationObject : relationObjects) {
                    if (relationObject.equals(relationSubject)) {
                        relationErrors.add(KsfValidationError.constructCircularTemporalRelationMessage(relationIdentifier));
                    }
                }
            }
        }

        // Error if any required elements are missing: wd_label
        if (isNullOrEmptyList(wdLabel)) {
            relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_LABEL));
        }

        // Error if any required elements are missing: wd_description
        if (isNullOrEmptyList(wdDescription)) {
            relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, WD_DESCRIPTION));
        }

        // Warning that empty wd_label values are ignored.
        if (removeEmptyValues(wdLabel)) {
            relationErrors.add(KsfValidationError.constructEmptyStringIgnoredWarning(RELATIONS, relationIdentifier, WD_LABEL));
        }

        // Error if there is a mismatch between the number of wd_node, wd_label, and wd_description values.
        if (KsfFieldValuesValidation.isMismatchedListFieldCount(wdNode, wdLabel, wdDescription)) {
            relationErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(RELATIONS, relationIdentifier, WD_NODE, WD_LABEL, WD_DESCRIPTION));
        }

        return relationErrors;
    }

    private List<String> validateRelationArgumentIds(String relationType, List<String> relationArguments, String ownerIdentifier) {
        List<String> errorMsgsToReturn = new ArrayList<>();
        for (String relationArgument : relationArguments) {
            if (!entityIds.contains(relationArgument) && !eventIds.contains(relationArgument)) {
                errorMsgsToReturn.add(KsfValidationError.constructInvalidIdMessage(RELATION, ownerIdentifier, relationType, relationArgument));
            }
        }
        return errorMsgsToReturn;
    }

    /**
     * (TA2): Error if any required elements are missing: ta1ref.
     * (TA2): Error if one of relationObject_prov, relationProvenance, relationSubject_prov, and confidence
     *        (Task 1) is present, but they aren't all present.
     * (TA2): Error if wd_node is "wd:Q79030196" or "Q79030196" (i.e., the before relation).
     * (TA2): Error if ta1ref is "none", but relationObject_prov, relationProvenance, relationSubject_prov,
     *        or confidence (Task 1) is missing.
     * (TA2): Error if ta1ref does not follow naming convention.
     * (TA2): Error if confidence is not a float between 0 and 1.0.
     * (TA2): Error if relationObject is an array of more than one element.
     * (TA2 Task 1): Error if there is a mismatch between the number of confidence and relationProvenance values.
     * (TA2 Task 1): Error if relationObject_prov, relationProvenance, or relationSubject_prov does not match the provenance in a provenanceData object.
     * @param relation a single relation to validate
     * @param relationIdentifier a String to identify the relation in error messages
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTA2Relation(Relation relation, String relationIdentifier) {
        List<String> relationErrors = new ArrayList<>();

        boolean hasProvenance = isNonEmptyList(relation.getRelationProvenance());
        boolean hasSubjectProv = isNonEmptyList(relation.getRelationSubjectProv());
        boolean hasObjectProv = isNonEmptyList(relation.getRelationObjectProv());
        boolean hasConfidence = isNonEmptyList(relation.getConfidence());
        int numPresent = 0;
        if (hasProvenance) {
            numPresent++;
        }
        if (hasSubjectProv) {
            numPresent++;
        }
        if (hasObjectProv) {
            numPresent++;
        }

        if (isTask1) {
            relationErrors.addAll(
                    validateTA2Task1Relation(relation, relationIdentifier, numPresent, hasProvenance, hasSubjectProv, hasObjectProv, hasConfidence));
        } else {
            // (TA2): Error if one of relationObject_prov, relationProvenance, relationSubject_prov,
            //        and confidence (Task 1) is present, but they aren't all present.
            if (numPresent != 0 && numPresent != 3) {
                relationErrors.add(KsfValidationError.constructAllRequiredMessage(RELATION,
                        relationIdentifier, Arrays.asList(RELATION_PROVENANCE, RELATION_OBJECT_PROV, RELATION_SUBJECT_PROV)));
            }

            // Error if provenance is an invalid URI
            // NOTE:  This is checked in Task 1 by ensuring they match a provenanceData object).
            relationErrors.addAll(validateURIs(RELATION, relationIdentifier, RELATION_PROVENANCE, relation.getRelationProvenance()));
            relationErrors.addAll(validateURIs(RELATION, relationIdentifier, RELATION_SUBJECT_PROV, relation.getRelationSubjectProv()));
            relationErrors.addAll(validateURIs(RELATION, relationIdentifier, RELATION_OBJECT_PROV, relation.getRelationObjectProv()));
        } // Task 2

        // (TA2): Error if wd_node is "wd:Q79030196" or "Q79030196" (i.e., the before relation).
        String wdNode = safeGetFirstString(relation.getWdNode());
        if (wdNode.equalsIgnoreCase(WIKI_EVENT_PREFIX + BEFORE_QID) || wdNode.equalsIgnoreCase(BEFORE_QID) || wdNode.equalsIgnoreCase(WIKI_EVENT_IRI + BEFORE_QID)) {
            relationErrors.add(KsfValidationError.constructInvalidValueMessage(RELATION, relationIdentifier, WD_NODE, wdNode));
        }

        // (TA2): Error if relationObject is an array of more than one element
        if (isNonEmptyList(relation.getRelationObject()) && relation.getRelationObject().size() > 1) {
            relationErrors.add(KsfValidationError.constructTooManyValuesMessage(RELATION, relationIdentifier, RELATION_OBJECT));
        }

        String ta1ref = safeGetString(relation.getTa1ref());
        if (ta1ref.isBlank()) {
            // (TA2): Error if any required elements are missing: ta1ref.
            relationErrors.add(KsfValidationError.constructMissingValueMessage(RELATIONS, relationIdentifier, TA1_REF));
        } else {
            // (TA2): Error if ta1ref is "none", but relationObject_prov, relationProvenance, relationSubject_prov,
            // or confidence (Task 1) is missing.
            if (ta1ref.equals(NONE)) {
                if (!hasProvenance) {
                    relationErrors.add(KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, RELATION_PROVENANCE));
                }
                if (!hasSubjectProv) {
                    relationErrors.add(KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, RELATION_SUBJECT_PROV));
                }
                if (!hasObjectProv) {
                    relationErrors.add(KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, RELATION_OBJECT_PROV));
                }
                if (isTask1 && !hasConfidence) {
                    relationErrors.add(KsfValidationError.constructProvenanceTa1refMessage(RELATION, relationIdentifier, CONFIDENCE));
                }
            } else {
                // (TA2): Error if ta1ref does not follow naming convention.
                relationErrors.addAll(validateTa1RefNaming(ERROR, RELATIONS, relationIdentifier, ta1ref));
            }
        }

        return relationErrors;
    }

    private List<String> validateTA2Task1Relation(Relation relation, String relationIdentifier, int numPresent,
            boolean hasProvenance, boolean hasSubjectProv, boolean hasObjectProv, boolean hasConfidence) {
        List<String> relationErrors = new ArrayList<>();

        // (TA2): Error if one of relationObject_prov, relationProvenance, relationSubject_prov,
        //        and confidence (Task 1) is present, but they aren't all present.
        numPresent += hasConfidence ? 1 : 0;
        if (numPresent != 0 && numPresent != 4) {
            relationErrors.add(KsfValidationError.constructAllRequiredMessage(RELATION,
                    relationIdentifier, Arrays.asList(RELATION_PROVENANCE, RELATION_OBJECT_PROV, RELATION_SUBJECT_PROV, CONFIDENCE)));
        }

        // (TA2): Error if confidence is not a float between 0 and 1.0.
        List<Float> confidenceValues = relation.getConfidence();
        if (hasConfidence) {
            for (Float confidence : confidenceValues) {
                if (!KsfFieldValuesValidation.isValidConfidenceValue(confidence)) {
                    relationErrors.add(KsfValidationError.constructInvalidValueMessage(RELATION, relationIdentifier, CONFIDENCE, confidence));
                }
            }
        }

        // (TA2 Task 1): Error if relationObject_prov, relationProvenance, or relationSubject_prov does not match the provenance in a provenanceData object.
        if (hasProvenance) {
            for (String provenance : relation.getRelationProvenance()) {
                if (!KsfFieldValuesValidation.isValidProvenanceId(provenance, provenanceIds)) {
                    relationErrors.add(KsfValidationError.constructInvalidIdMessage(RELATIONS, relationIdentifier, RELATION_PROVENANCE, provenance));
                }
            }
        }
        if (hasSubjectProv) {
            for (String provenance : relation.getRelationSubjectProv()) {
                if (!KsfFieldValuesValidation.isValidProvenanceId(provenance, provenanceIds)) {
                    relationErrors.add(KsfValidationError.constructInvalidIdMessage(RELATIONS, relationIdentifier, RELATION_SUBJECT_PROV, provenance));
                }
            }
        }
        if (hasObjectProv) {
            for (String provenance : relation.getRelationObjectProv()) {
                if (!KsfFieldValuesValidation.isValidProvenanceId(provenance, provenanceIds)) {
                    relationErrors.add(KsfValidationError.constructInvalidIdMessage(RELATIONS, relationIdentifier, RELATION_OBJECT_PROV, provenance));
                }
            }
        }

        // (TA2 Task 1): Error if there is a mismatch between the number of confidence and relationProvenance values.
        if (hasConfidence && hasProvenance &&
                relation.getConfidence().size() > 1 && // a single confidence value can apply to multiple provenance values
                KsfFieldValuesValidation.isMismatchedListFieldCount(confidenceValues, relation.getRelationProvenance())) {
            relationErrors.add(KsfValidationError.constructMismatchedKeywordCountMessage(RELATION, relationIdentifier, CONFIDENCE, RELATION_PROVENANCE));
        }

        return relationErrors;
    }

    /**
     * Error if predictionProvenance does not match the @id of an entity, event, or relation object.
     * @param events All events for the instantiation specified by `[`instantiationId`]`
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validatePredictions(List<SchemaEvent> events) {
        List<String> predictionErrors = new ArrayList<>();

        if (isNullOrEmptyList(events)) {
            return predictionErrors;
        }

        for (SchemaEvent event : events) {
            if (isNullOrEmptyList(event.getPredictionProvenance())) {
                continue; // No predictions, so skip
            }
            String eventIdentifier = getFirstNonBlankString(Arrays.asList(event.getAtId(), event.getName(), "<UNKNOWN>"));
            // Error if predictionProvenance does not match the @id of an entity, event, or relation object.
            List<String> predictionProvenance = safeGetStrings(event.getPredictionProvenance());
            for (String provId : predictionProvenance) {
                if (!eventIds.contains(provId) && !entityIds.contains(provId) && !relationIds.contains(provId)) {
                    predictionErrors.add(KsfValidationError.constructInvalidIdMessage(EVENT, eventIdentifier, PREDICTION_PROVENANCE, provId));
                }
            } // each predictionProvenance
        } // each event

        return predictionErrors;
    }

    /**
     * (TA2): Error if none of the temporal keywords is present: duration, earliestStartTime, latestStartTime,
     *        earliestEndTime, latestEndTime, and absoluteTime.
     * (TA2): Error if a temporal keyword appears more than once in the list of temporal objects.
     * (TA2): Error if confidence is not a float between 0 and 1.0.
     * (TA2 Task 1): Error if any required elements are missing: confidence
     * (TA2 Task 1): Warning if provenance does not match the provenance in a provenanceData object.
     * (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values (if present).
     * @param temporals a list of temporal objects
     * @param eventIdentifier identifier for the event
     * @return a list of validation error strings for the specified temporalList
     */
    private List<String> validateTemporals(List<Temporal> temporals, String eventIdentifier) {
        List<String> temporalErrors = new ArrayList<>();

        if (isNullOrEmptyList(temporals)) {
            return temporalErrors;
        }

        int numTemporals = 0;
        String eventDuration = null;
        String eventEST = null;
        String eventEET = null;
        String eventLST = null;
        String eventLET = null;
        String eventAbsTime = null;
        for (Temporal curTemporal : temporals) {
            String temporalIdentifier = String.format(INDEXED_ARRAY_FORMAT, eventIdentifier, TEMPORAL, numTemporals++);
            // (TA2 Task 1): Error if any required elements are missing: confidence
            List<Float> confidenceValues = curTemporal.getConfidence();
            if (isNullOrEmptyList(confidenceValues)) {
                if (isTask1) {
                    temporalErrors.add(KsfValidationError.constructMissingValueMessage(TEMPORAL, temporalIdentifier, CONFIDENCE));
                }
            } else {
                // (TA2): Error if confidence is not a float between 0 and 1.0.
                for (Float confidence : confidenceValues) {
                    if (!KsfFieldValuesValidation.isValidConfidenceValue(confidence)) {
                        temporalErrors.add(KsfValidationError.constructInvalidValueMessage(TEMPORAL, temporalIdentifier, CONFIDENCE, confidence));
                    }
                }
            }

            // (TA2): Error if a temporal keyword appears more than once in the list of temporal objects.
            boolean foundTemporal = false;
            if (!StringUtils.isBlank(curTemporal.getDuration())) {
                foundTemporal = true;
                if (eventDuration != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, DURATION));
                } else {
                    eventDuration = curTemporal.getDuration();
                }
            }
            if (!StringUtils.isBlank(curTemporal.getEarliestStartTime())) {
                foundTemporal = true;
                if (eventEST != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, EARLIEST_START_TIME));
                } else {
                    eventEST = curTemporal.getEarliestStartTime();
                }
            }
            if (!StringUtils.isBlank(curTemporal.getLatestStartTime())) {
                foundTemporal = true;
                if (eventLST != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, LATEST_START_TIME));
                } else {
                    eventLST = curTemporal.getLatestStartTime();
                }
            }
            if (!StringUtils.isBlank(curTemporal.getEarliestEndTime())) {
                foundTemporal = true;
                if (eventEET != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, EARLIEST_END_TIME));
                } else {
                    eventEET = curTemporal.getEarliestEndTime();
                }
            }
            if (!StringUtils.isBlank(curTemporal.getLatestEndTime())) {
                foundTemporal = true;
                if (eventLET != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, LATEST_END_TIME));
                } else {
                    eventLET = curTemporal.getLatestEndTime();
                }
            }
            if (!StringUtils.isBlank(curTemporal.getAbsoluteTime())) {
                foundTemporal = true;
                if (eventAbsTime != null) {
                    temporalErrors.add(KsfValidationError.constructDuplicateTemporalMessage(eventIdentifier, ABSOLUTE_TIME));
                } else {
                    eventAbsTime = curTemporal.getAbsoluteTime();
                }
            }
            // (TA2): Error if none of the following keywords is present: duration, earliestStartTime,
            //        latestStartTime, earliestEndTime, latestEndTime, and absoluteTime.
            if (!foundTemporal) {
                temporalErrors.add(KsfValidationError.constructEmptyTemporalMessage(temporalIdentifier));
            }

            // (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values (if present).
            // (TA2 Task 1): Warning if provenance does not match the provenance in a provenanceData object.
            if (isTask1) {
                temporalErrors.addAll(KsfFieldValuesValidation.validateProvenanceValues(curTemporal.getProvenance(),
                        curTemporal.getConfidence(), provenanceIds, TEMPORAL, temporalIdentifier, WARNING));
            } else {
                // Error if provenance is an invalid URI
                temporalErrors.addAll(validateURIs(TEMPORAL, temporalIdentifier, PROVENANCE, curTemporal.getProvenance()));
            }
        } // each temporal

        temporalErrors.addAll(validateTemporalTimes(eventEST, eventEET, eventLST, eventLET, eventAbsTime, eventIdentifier));

        return temporalErrors;
    }

    /**
     * (TA2) Error if the value of a temporal keyword cannot be parsed.
     * (TA2) Warning if contradictory temporal keyword values are detected.
     * @param earliestStart the extracted earliest start date of the event, nor NULL if none was provided 
     * @param earliestEnd the extracted earliest end date of the event, nor NULL if none was provided
     * @param latestStart the extracted latest start date of the event, nor NULL if none was provided
     * @param latestEnd the extracted latest end date of the event, nor NULL if none was provided
     * @param absTime the extracted absolute time of the event, nor NULL if none was provided
     * @param eventId an identifier for the event
     * @return a List of Strings, one for each validation error or warning
     */
    private List<String> validateTemporalTimes(String earliestStart, String earliestEnd, String latestStart,
            String latestEnd, String absTime, String eventId) {
        List<String> temporalTimesErrors = new ArrayList<>();

        // (TA2) Error if the value of a temporal keyword cannot be parsed.
        LocalDateTime earliestStartTime = safeParseLocalDateTime(earliestStart);
        LocalDateTime latestStartTime = safeParseLocalDateTime(latestStart);
        LocalDateTime earliestEndTime = safeParseLocalDateTime(earliestEnd);
        LocalDateTime latestEndTime = safeParseLocalDateTime(latestEnd);
        LocalDateTime absoluteTime = safeParseLocalDateTime(absTime);

        // safeParseLocalDateTime() will return null only if there's an error parsing the time.
        if (earliestStartTime == null) {
            temporalTimesErrors.add(KsfValidationError.constructMalformedTemporalMessage(eventId, EARLIEST_START_TIME));
        }
        if (latestStartTime == null) {
            temporalTimesErrors.add(KsfValidationError.constructMalformedTemporalMessage(eventId, LATEST_START_TIME));
        }
        if (earliestEndTime == null) {
            temporalTimesErrors.add(KsfValidationError.constructMalformedTemporalMessage(eventId, EARLIEST_END_TIME));
        }
        if (latestEndTime == null) {
            temporalTimesErrors.add(KsfValidationError.constructMalformedTemporalMessage(eventId, LATEST_END_TIME));
        }
        if (absoluteTime == null) {
            temporalTimesErrors.add(KsfValidationError.constructMalformedTemporalMessage(eventId, ABSOLUTE_TIME));
        }

        // (TA2) Warning if contradictory temporal keyword values are detected.
        // Verify: earliestStart <= latestStart
        if (isValidLocalDateTime(earliestStartTime) && isValidLocalDateTime(latestStartTime) &&
            earliestStartTime.compareTo(latestStartTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, EARLIEST_START_TIME, LATEST_START_TIME));
        }
        // Verify: earliestStart <= earliestEnd
        if (isValidLocalDateTime(earliestStartTime) && isValidLocalDateTime(earliestEndTime) &&
            earliestStartTime.compareTo(earliestEndTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, EARLIEST_START_TIME, EARLIEST_END_TIME));
        }
        // Verify: earliestEnd <= latestEnd
        if (isValidLocalDateTime(earliestEndTime) && isValidLocalDateTime(latestEndTime) &&
            earliestEndTime.compareTo(latestEndTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, EARLIEST_END_TIME, LATEST_END_TIME));
        }
        // Verify: latestStart <= latestEnd
        if (isValidLocalDateTime(latestStartTime) && isValidLocalDateTime(latestEndTime) &&
            latestStartTime.compareTo(latestEndTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, LATEST_START_TIME, LATEST_END_TIME));
        }
        // Verify: earliestStart <= absoluteTime
        if (isValidLocalDateTime(earliestStartTime) && isValidLocalDateTime(absoluteTime) &&
            earliestStartTime.compareTo(absoluteTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, EARLIEST_START_TIME, ABSOLUTE_TIME));
        }
        // Verify: absoluteTime <= latestEnd
        if (isValidLocalDateTime(absoluteTime) && isValidLocalDateTime(latestEndTime) &&
            absoluteTime.compareTo(latestEndTime) > 0) {  // NOSONAR because isValidLocalDateTime checks for null
            temporalTimesErrors.add(KsfValidationError.constructContradictoryTemporalMessage(eventId, ABSOLUTE_TIME, LATEST_END_TIME));
        }

        return temporalTimesErrors;
    }

    /**
     * Error if any required elements are missing: provenance, mediaType, childID, parentIDs.
     * Warning if childID does not match a childID from the current document corpus.
     * Warning if any parentIDs do not match a parentID from the current document corpus.
     * Warning if mediaType does not match the media type of the corresponding childID from the current document corpus.
     * @param provenanceData a list of provenance data
     * @return a list of validation error strings for the specified provenance data
     */
    private List<String> validateProvenanceData(List<Provenance> provenanceData) {
        List<String> provenanceErrors = new ArrayList<>();
        if (isNullOrEmptyList(provenanceData)) {
            return provenanceErrors; // This error has already been reported
        }

        int numProvenance = 0;
        for (Provenance curProvenance : provenanceData) {
            String curProvenanceId;
            if (curProvenance != null) {
                // Error if any required elements are missing: provenance, mediaType, childID, parentIDs.
                curProvenanceId = curProvenance.getProvenanceID();
                if (StringUtils.isAllEmpty(curProvenanceId)) {
                    curProvenanceId = String.format("%s.%s[%d]", DOCUMENT, PROVENANCE_DATA, numProvenance);
                    provenanceErrors.add(KsfValidationError.constructMissingValueMessage(PROVENANCE_DATA, curProvenanceId, PROVENANCE_ID));
                } else {
                    provenanceIds.add(curProvenanceId); // Add to the document-level provenance-ids
                }
                numProvenance++;
                String mediaType = safeGetString(curProvenance.getMediaType());
                if (mediaType.isBlank()) {
                    provenanceErrors.add(KsfValidationError.constructMissingValueMessage(PROVENANCE_DATA, curProvenanceId, MEDIA_TYPE));
                } else {
                    provenanceErrors.addAll(KsfFieldValuesValidation.validateMediaSubfields(curProvenance, curProvenanceId));
                }
                String childID = safeGetString(curProvenance.getChildID());
                if (childID.isBlank()) {
                    provenanceErrors.add(KsfValidationError.constructMissingValueMessage(PROVENANCE_DATA, curProvenanceId, CHILD_ID));
                } else {
                    boolean validID = KsfFieldValuesValidation.isValidChildId(childID);
                    if (validID) {
                        // Warning if mediaType does not match the media type of the corresponding childID from the current document corpus.
                        if (!mediaType.isBlank()) {
                            provenanceErrors.addAll(KsfFieldValuesValidation.validateMediaTypeForId(childID, mediaType, curProvenanceId));
                        }
                    } else {
                        // Warning if childID does not match a childID from the current document corpus.
                        provenanceErrors.add(KsfValidationError.constructInvalidValueMessage(WARNING, PROVENANCE_DATA, curProvenanceId, CHILD_ID, childID));
                    }
                }
                List<String> parentIDs = safeGetStrings(curProvenance.getParentIDs());
                boolean malformedParentIDs = ValidationUtils.removeEmptyValues(curProvenance.getParentIDs());
                if (parentIDs.isEmpty()) {
                    provenanceErrors.add(KsfValidationError.constructMissingValueMessage(PROVENANCE_DATA, curProvenanceId, PARENT_IDs));
                } else if (malformedParentIDs) {
                    provenanceErrors.add(KsfValidationError.constructInvalidMediaSubFieldMessage(WARNING, curProvenanceId, mediaType, PARENT_IDs));
                } else {
                    for (String parentID : parentIDs) {
                        if (!KsfFieldValuesValidation.isValidParentId(parentID)) {
                            provenanceErrors.add(KsfValidationError.constructInvalidValueMessage(WARNING, PROVENANCE_DATA, curProvenanceId, PARENT_IDs, parentID));
                        }
                    }
                }
            }
        }

        return provenanceErrors;
    }

}
