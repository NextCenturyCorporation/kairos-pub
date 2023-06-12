package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.Provenance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ncc.kairos.moirai.clotho.resources.KairosSchemaFormatConstants.*;
import static com.ncc.kairos.moirai.clotho.utilities.ksf.validation.KsfValidator.ERROR;

public final class KsfFieldValuesValidation {

    private KsfFieldValuesValidation() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static List<String> validateModalityValues(String objectType, List<String> modalities, String objectId) {
        List<String> invalidModalityError = new ArrayList<>();
        List<String> invalidModalityValues = new ArrayList<>();
        List<String> validEventModalities = Arrays.asList(GENERIC, HEDGED, IRREALIS, NEGATED);
        List<String> validParticipantModalities = Arrays.asList(GENERIC, HEDGED, NEGATED);
        List<String> validRelationModalities = Arrays.asList(HEDGED, NEGATED);

        if (ValidationUtils.isNullOrEmptyList(modalities)) {
            return invalidModalityError;
        }

        for (String curModality : modalities) {
            if (curModality != null) {
                if ((objectType.equals(EVENT)) && !validEventModalities.contains(curModality)) {
                    invalidModalityValues.add(curModality);
                }
                if (objectType.equals((RELATION)) && !validRelationModalities.contains(curModality)) {
                    invalidModalityValues.add(curModality);
                }
                if (objectType.equals(VALUES) && !validParticipantModalities.contains(curModality)) {
                    invalidModalityValues.add(curModality);
                }
            }
        }

        if (!invalidModalityValues.isEmpty()) {
            invalidModalityError.add(KsfValidationError.constructInvalidModalityValuesMessage(objectType, objectId, invalidModalityValues));
        }
        return invalidModalityError;
    }

    public static List<String> validateChildrenGateValues(String childrenGateVal, String eventIdentifier) {
        List<String> invalidOutlinkError = new ArrayList<>();
        List<String> validChildrenGateValues = Arrays.asList(AND_GATE, OR_GATE, XOR_GATE);

        if (!validChildrenGateValues.contains(childrenGateVal.toUpperCase())) {
            invalidOutlinkError.add(KsfValidationError.constructInvalidValueMessage(EVENT, eventIdentifier, CHILDREN_GATE, childrenGateVal));
        }

        return invalidOutlinkError;
    }

    public static boolean isValidCentralityValue(Float centralityValue) {
        return isValidConfidenceValue(centralityValue);
    }

    public static boolean isValidImportanceValue(Float importanceValue) {
        return isValidConfidenceValue(importanceValue);
    }

    public static boolean isValidConfidenceValue(Float confidenceValStr) {
        return (confidenceValStr != null && confidenceValStr >= 0f && confidenceValStr <= 1f);
    }

    public static List<String> validateProvenanceValues(List<String> provenance, List<Float> confidence, List<String> validProvenanceIds,
                                                        String objectType, String objectId, String errorLevel) {
        List<String> errorsToReturn = new ArrayList<>();
        List<String> provenanceList = ValidationUtils.safeGetStrings(provenance);
        // (TA2 Task 1): Error if there is a mismatch between the number of confidence and provenance values (if present).
        if (
                (confidence == null || confidence.size() != 1) && // a single confidence value can apply to multiple provenance values
                (KsfFieldValuesValidation.isMismatchedListFieldCount(confidence, provenanceList)) && // mismatched counts
                (!(provenanceList.isEmpty() && objectType.equals(TEMPORAL))) // provenance optional for temporals
           ) {
            errorsToReturn.add(KsfValidationError.constructMismatchedKeywordCountMessage(objectType, objectId, CONFIDENCE, PROVENANCE));
        }

        // (TA2 Task 1): Error/Warning if provenance does not match the provenanceID in a provenanceData object.
        for (String provenanceId : provenanceList) {
            if (!KsfFieldValuesValidation.isValidProvenanceId(provenanceId, validProvenanceIds)) {
                errorsToReturn.add(KsfValidationError.constructInvalidIdMessage(errorLevel, objectType, objectId, PROVENANCE, provenanceId));
            }
        }

        return errorsToReturn;
    }

    public static boolean isValidProvenanceId(String provenanceIdToValidate, List<String> validProvenanceIds) {
        return validProvenanceIds.contains(provenanceIdToValidate);
    }

    /**
     * Validate media subfields.  Note that at present, we only return one error per invocation.
     * Error if text media types lack offset or length, or if offset < 0 or length <= 1.
     * Error if mediaType cannot be categorized as text, image, audio, or video.
     * Error if image media types lack parentIDs or boundingBox with exactly four values.
     * Error if audio media types lack parentIDs, startTime, or endTime.
     * Error if video media types lack parentIDs or boundingBox with exactly four values.
     * Error if video media types lack startTime or endTime AND keyframes.
     * @param provenanceObj a provenance object to validate
     * @param provenanceId an identifier for the specified provenance object
     * @return a list of validation violation strings for the specified provenance object
     */
    public static List<String> validateMediaSubfields(Provenance provenanceObj, String provenanceId) {
        String mediaType = provenanceObj.getMediaType();
        List<String> errorsToReturn = new ArrayList<>();
        String errorLevel = ERROR;

        // Violation if mediaType cannot be categorized as text, image, audio, or video.
        if (KsfProvenanceData.TEXT_MEDIA_TYPES.contains(mediaType)) {
            mediaType = TEXTUAL;
        } else if (KsfProvenanceData.IMAGE_MEDIA_TYPES.contains(mediaType)) {
            mediaType = IMAGE;
        } else if (KsfProvenanceData.AUDIO_MEDIA_TYPES.contains(mediaType)) {
            mediaType = AUDIO;
        } else if (KsfProvenanceData.VIDEO_MEDIA_TYPES.contains(mediaType)) {
            mediaType = VIDEO;
        } else {
            errorsToReturn.add(KsfValidationError.constructInvalidMediaTypeMessage(provenanceId, mediaType));
            return errorsToReturn;
        }

        // Violation if text media types lack offset or length.
        if (mediaType.equals(TEXTUAL)) {
            if (provenanceObj.getOffset() == null) {
                errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, TEXTUAL, OFFSET));
            } else if (provenanceObj.getLength() == null) {
                errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, TEXTUAL, LENGTH));
            } else { // Violation if offset < 0 or length <= 1
                if (provenanceObj.getOffset() < 0f) {
                    errorsToReturn.add(KsfValidationError.constructInvalidValueMessage(errorLevel, PROVENANCE_DATA,
                            provenanceId, OFFSET, provenanceObj.getOffset()));
                } else if (provenanceObj.getLength() <= 1f) {
                    errorsToReturn.add(KsfValidationError.constructInvalidValueMessage(errorLevel, PROVENANCE_DATA,
                            provenanceId, LENGTH, provenanceObj.getLength()));
                }
            }
        }

        // Violation if image or video media types lack boundingBox with exactly four values.
        if (mediaType.equals(IMAGE) || mediaType.equals(VIDEO)) {
            boolean malformedBoundingBox = ValidationUtils.removeEmptyValues(provenanceObj.getBoundingBox());
            if (malformedBoundingBox || ValidationUtils.isNullOrEmptyList(provenanceObj.getBoundingBox()) ||
                    provenanceObj.getBoundingBox().size() != 4) {
                errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, mediaType, BOUNDING_BOX));
            }
        }

        // Violation if audio or video media types lack startTime, or endTime OR endTime <= startTime, or endTime - startTime > 15
        if (mediaType.equals(AUDIO) || mediaType.equals(VIDEO)) {
            Float startTime = provenanceObj.getStartTime();
            Float endTime = provenanceObj.getEndTime();
            if (startTime != null && endTime != null) {
                if (endTime <= startTime || endTime - startTime > PROVENANCE_MAX_DURATION) {
                    errorsToReturn.add(KsfValidationError.constructInvalidProvenanceTimesMessage(provenanceId, mediaType, startTime, endTime));
                }
            } else {
                if (startTime == null) {
                    errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, mediaType, START_TIME));
                }
                if (endTime == null) {
                    errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, mediaType, END_TIME));
                }
            }
        }

        // Violation if keyframes contains blank values.
        if (mediaType.equals(VIDEO)) {
            boolean malformedKeyframes = ValidationUtils.removeEmptyValues(provenanceObj.getKeyframes());
            if (malformedKeyframes) {
                errorsToReturn.add(KsfValidationError.constructInvalidMediaSubFieldMessage(errorLevel, provenanceId, VIDEO, KEYFRAMES));
            }
        }
        return errorsToReturn;
    }

    public static boolean isMismatchedListFieldCount(List<?>... fields) {
        if (fields == null || fields.length < 2) {
            return false;
        }
        int[] counts = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            counts[i] = fields[i] == null ? 0 : fields[i].size();
        }
        // Note: this is inefficient, but the number of fields is expected to be no more than 3.
        for (int count1 : counts) {
            for (int count2 : counts) {
                if (count1 != count2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isValidChildId(String childID) {
        return KsfProvenanceData.isValidChildID(childID);
    }

    public static boolean isValidParentId(String parentID) {
        return KsfProvenanceData.isValidParentID(parentID);
    }

    public static List<String> validateMediaTypeForId(String childID, String mediaType, String provenanceID) {
        List<String> errorsToReturn = new ArrayList<>();
        if (!KsfProvenanceData.isValidMediaTypeForId(childID, mediaType)) {
            errorsToReturn.add(KsfValidationError.constructMismatchedMediaTypeForIDMessage(provenanceID, childID, mediaType));
        }
        return errorsToReturn;
    }
}
