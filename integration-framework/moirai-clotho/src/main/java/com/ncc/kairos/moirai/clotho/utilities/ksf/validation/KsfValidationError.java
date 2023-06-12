package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import java.util.*;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.FATAL;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.ERROR;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.WARNING;

public final class KsfValidationError {

    private KsfValidationError() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    static String constructMissingValueMessage(String objectType, String objectId, String missingKeyword) {
        return constructMissingValueMessage(ERROR, objectType, objectId, missingKeyword);
    }

    static String constructMissingValueMessage(String errorType, String objectType, String objectId, String missingKeyword) {
        return errorType.equals(ERROR) ?
                String.format("%s: Required keyword '%s' is missing/empty from %s object '%s'.", errorType, missingKeyword, objectType, objectId)
                : String.format("%s: No '%s' keyword found in %s object '%s'.", errorType, missingKeyword, objectType, objectId);
    }

    static String constructEmptyTemporalMessage(String temporalId) {
        return String.format("%s: Temporal object '%s' must contain at least one of: %s, %s, %s, %s, %s, or %s.",
                ERROR, temporalId, DURATION, EARLIEST_START_TIME, LATEST_END_TIME, EARLIEST_END_TIME, LATEST_END_TIME, ABSOLUTE_TIME);
    }

    static String constructDuplicateTemporalMessage(String eventId, String keyword) {
        return String.format("%s: temporal object on event '%s' has duplicate '%s' keywords.", ERROR, eventId, keyword);
    }

    static String constructContradictoryTemporalMessage(String eventId, String keyword1, String keyword2) {
        return String.format("%s: temporal object on event '%s' has a(n) %s that comes after its %s.",
                WARNING, eventId, keyword1, keyword2);
    }

    static String constructOutlinkTemporalMismatchMessage(String eventId, String keyword1, String keyword2) {
        return String.format("%s: temporal object on event '%s' has an %s that comes after its outlinked event's %s.",
                WARNING, eventId, keyword1, keyword2);
    }

    static String constructMalformedTemporalMessage(String eventId, String keyword1) {
        return String.format("%s: temporal object on event '%s' has a malformed %s.", ERROR, eventId, keyword1);
    }

    static String constructBothKeywordsMissingMessage(String errorType, String objectType, String objectId, String keyword1, String keyword2) {
        return String.format("%s: At least one of %s or %s must be present in %s '%s'.", errorType, keyword1, keyword2, objectType, objectId);
    }

    static String constructBothKeywordsMissingMessage(String objectType, String objectId, String keyword1, String keyword2) {
        return constructBothKeywordsMissingMessage(ERROR, objectType, objectId, keyword1, keyword2);
    }

    static String constructBothKeywordsPresentMessage(String objectType, String keyword1, String keyword2, String objectId) {
        return String.format("%s: Cannot have both %s and %s be present in %s '%s'.", ERROR, keyword1, keyword2, objectType, objectId);
    }

    static String constructExactlyOneRequiredMessage(String objectType, String objectId, String keyword1, String keyword2) {
        return String.format("%s: Exactly one of %s or %s must be present in %s '%s'.", ERROR, keyword1, keyword2, objectType, objectId);
    }

    static String constructAllRequiredMessage(String objectType, String objectId, List<String> keywords) {
        return String.format("%s: If one of %s is present, then all must be present in %s '%s'.", ERROR, keywords, objectType, objectId);
    }

    static String constructIdMissingKeywordMessage(String objectType, String objectId, String keyword, String invalidId, String missingKeyword) {
        return String.format("%s: %s object '%s' specifies a(n) %s @id '%s' with a missing/empty %s keyword.",
                ERROR, objectType, objectId, keyword, invalidId, missingKeyword);
    }

    static String constructHasKeywordButNotOtherKeywordMessage(String objectType, String objectId, String hasKeyword, String missingKeyword) {
        return String.format("%s: %s '%s' has %s but no %s.", ERROR, objectType, objectId, hasKeyword, missingKeyword);
    }

    static String constructTooManyValuesMessage(String objectType, String objectId, String keyword) {
        return constructTooManyValuesMessage(ERROR, objectType, objectId, keyword, 1);
    }

    static String constructTooManyValuesMessage(String errorLevel, String objectType, String objectId, String keyword, int maxAllowed) {
        return String.format("%s: The %s '%s' has more than %d '%s' value(s).", errorLevel, objectType, objectId, maxAllowed, keyword);
    }

    static String constructDuplicateParticipantMessage(String objectId, String roleName, String entity) {
        return String.format("%s: The %s '%s' has two %s with the same %s (%s) and %s (%s).", ERROR, EVENT, objectId,
                PARTICIPANTS, ROLE_NAME, roleName, ENTITY, entity);
    }

    static String constructNamingViolationMessage(String errorType, String objectType, String objectId, String keyword, String invalidId, String violationType) {
        return String.format("%s: naming convention violation (%s) in %s object with invalid %s: '%s' in top-level object '%s'.",
                errorType, violationType, objectType, keyword, invalidId, objectId);
    }

    static String constructNamingViolationMessage(String objectType, String objectId, String keyword, String invalidId, String violationType) {
        return constructNamingViolationMessage(ERROR, objectType, objectId, keyword, invalidId, violationType);
    }

    static String constructUnsupportedKeywordMessage(String objectType, String objectId, String keyword) {
        return String.format("%s: %s '%s' has an unsupported keyword '%s'.", ERROR, objectType, objectId, keyword);
    }

    static String constructUnsupportedXOROutlinksMessage(String childId, String parentId) {
        return String.format("%s: event '%s' specifies %s, but parent event '%s' has %s %s.",
                ERROR, childId, OUTLINKS, parentId, XOR_GATE, CHILDREN_GATE);
    }

    static String constructInvalidValueMessage(String objectType, String objectId, String keyword, Object invalidValue) {
        return constructInvalidValueMessage(ERROR, objectType, objectId, keyword, invalidValue);
    }

    static String constructInvalidValueMessage(String errorType, String objectType, String objectId, String keyword, Object invalidValue) {
        return String.format("%s: %s object '%s' has an invalid %s value '%s'.", errorType, objectType, objectId, keyword,
                invalidValue == null ? "" : invalidValue.toString());
    }

    static String constructDuplicateIdsMessage(String objectType, String objectId, String parentType, Set<String> duplicateIds) {
        return String.format("%s: The following @id(s): %s appear multiple times in %s for the %s object '%s'.",
                FATAL, duplicateIds.toString(), objectType, parentType, objectId);
    }

    static String constructInvalidModalityValuesMessage(String objectType, String objectId, List<String> invalidValues) {
        return String.format("%s: %s object '%s' has invalid modality value(s) %s.", ERROR, objectType, objectId, invalidValues.toString());
    }

    public static String constructInvalidMediaTypeMessage(String provenanceId, String mediaType) {
        return String.format("%s: mediaType '%s' cannot be categorized as text/image/audio/video on provenance object: '%s'.",
                ERROR, mediaType, provenanceId);
    }

    static String constructInvalidMediaSubFieldMessage(String errorType, String provenanceId, String mediaCategory, String invalidField) {
        return String.format("%s: required keyword '%s' is missing/empty/malformed in %s provenance object '%s'.",
                errorType, invalidField, mediaCategory, provenanceId);
    }

    static String constructInvalidProvenanceTimesMessage(String provenanceId, String mediaCategory, float startTime, float endTime) {
        return (endTime <= startTime ?
                String.format("%s: %s provenance object '%s' has invalid provenance times (%s %f <= %s %f).",
                        ERROR, mediaCategory, provenanceId, END_TIME, endTime, START_TIME, startTime) :
                String.format("%s: %s provenance object '%s' has excessive duration (%fs); only the first %ds will be assessed.",
                        WARNING, mediaCategory, provenanceId, (endTime - startTime), PROVENANCE_MAX_DURATION));
    }

    static String constructProvenanceTa1refMessage(String objectType, String objectId, String keywordName) {
        return String.format("%s: %s%s object '%s' has ta1ref=none but %s is missing.", ERROR,
                objectType.equals(EVENT) ? "primitive " : "", objectType, objectId, keywordName);
    }

    static String constructMismatchedKeywordCountMessage(String objectType, String objectId, String... keywords) {
        return String.format("%s: mismatch between the number of %s values in %s object '%s'.",
                ERROR, Arrays.toString(keywords), objectType, objectId);
    }

    static String constructInvalidIdMessage(String errorType, String objectType, String objectId, String keyword, String invalidId) {
        return String.format("%s: %s object '%s' specifies a(n) %s keyword with invalid @id '%s'.",
                errorType, objectType, objectId, keyword, invalidId);
    }

    static String constructInvalidIdMessage(String objectType, String objectId, String keyword, String invalidId) {
        return constructInvalidIdMessage(ERROR, objectType, objectId, keyword, invalidId);
    }

    static String constructInvalidURIMessage(String objectType, String objectId, String keyword, String invalidURI) {
        return String.format("%s: %s object '%s' specifies a(n) %s keyword with invalid URI '%s'.",
                ERROR, objectType, objectId, keyword, invalidURI);
    }

    static String constructCircularTemporalRelationMessage(String objectId) {
        return String.format("%s: Circular temporal relation in relation '%s'.", ERROR, objectId);
    }

    static String constructCircularOutlinksMessage(String instanceId) {
        return String.format("%s: Circular reference(s) in %s in instance '%s'.", FATAL, OUTLINKS, instanceId);
    }

    static String constructSameValueMessage(String errorType, String objectType, String objectId, String keyword1, String keyword2) {
        return String.format("%s: %s object '%s' specifies a %s that is the same value as its %s",
                errorType, objectType, objectId, keyword1, keyword2);
    }

    static String constructNotTransitiveReductionMessage(String instanceId) {
        return String.format("%s: The graph formed by the %s in instance '%s' is not the transitive reduction.", ERROR, OUTLINKS, instanceId);
    }

    static String constructRunawayChildMessage(String parentId, String childId) {
        return String.format("%s: %s object '%s' specifies a %s id of '%s' that does not specify the %s as its %s.",
                FATAL, EVENT, parentId, SUBGROUP_EVENTS, childId, EVENT, PARENT);
    }

    static String constructOrphanedChildMessage(String childId, String parentId) {
        return String.format("%s: %s object '%s' specifies a %s id of '%s' that does not specify the %s in its %s.",
                FATAL, EVENT, childId, PARENT, parentId, EVENT, SUBGROUP_EVENTS);
    }

    static String constructNonSiblingOutlinkMessage(String objectId, String outlinkId) {
        return String.format("%s: %s object '%s' specifies an %s id of '%s' that is not a sibling of the event.",
                ERROR, EVENT, objectId, OUTLINKS, outlinkId);
    }

    static String constructExactlyOneTopLevelEventsMessage(String instanceId) {
        return String.format("%s: %s object '%s' must specify exactly one event with isTopLevel set to true.",
                ERROR, INSTANCES, instanceId);
    }

    static String constructChildIsTopLevelMessage(String eventId, String invalidId) {
        return String.format("%s: Event object '%s' specifies a %s keyword containing an event '%s' whose %s is set to true.",
                ERROR, eventId, SUBGROUP_EVENTS, invalidId, IS_TOP_LEVEL);
    }

    static String constructPrimitivesAtRootMessage(String instanceId, int count) {
        return String.format("%s: Instance '%s' contains %d primitive events that are direct children of the root/topLevel event.",
                WARNING, instanceId, count);
    }

    static String constructMismatchedMediaTypeForIDMessage(String provenanceID, String childID, String mediaType) {
        return String.format("%s: Mismatched mediaType '%s' for childID '%s' in provenance '%s'.",
                WARNING, mediaType, childID, provenanceID);
    }

    static String constructMultipleXorProvenanceMessage(String eventId) {
        return String.format("%s: Provenance is provided for more than one child from parent event '%s' with %s %s.",
                WARNING, eventId, XOR_GATE, CHILDREN_GATE);
    }

    static String constructEmptyStringIgnoredWarning(String objectType, String objectId, String keywordName) {
        return String.format("%s: Empty %s string ignored in %s '%s'.", WARNING, keywordName, objectType, objectId);
    }
}
