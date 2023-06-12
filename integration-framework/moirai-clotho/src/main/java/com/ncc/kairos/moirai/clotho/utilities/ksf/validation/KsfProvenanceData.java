package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manage dataset-specific KSF provenance data parentID/childID/mediaType validity-checking.
 * Checking can be disabled via {@link #setPassThroughMode(boolean)}.
 * Non-pass-through checks must be preceded by a call to {@link #loadProvenanceData()}.
 * @author Darren Gemoets
 */
public final class KsfProvenanceData {

    // Media formats taken from https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
    // The full list is at https://www.iana.org/assignments/media-types/media-types.xhtml
    /**
     * IANA Text media types.
     */
    protected static final List<String> TEXT_MEDIA_TYPES;
    /**
     * IANA Image media types.
     */
    protected static final List<String> IMAGE_MEDIA_TYPES;
    /**
     * IANA Audio media types.
     */
    protected static final List<String> AUDIO_MEDIA_TYPES;
    /**
     * IANA Video media types.
     */
    protected static final List<String> VIDEO_MEDIA_TYPES;

    // This could be used for video media types as performers may cite just the audio or image aspect of video file,
    // but current guidance from LDC is Warn in these cases as well.
    private static final List<String> COMBO_MEDIA_TYPES = new ArrayList<>();
    private static final String PROVENANCE_DATA_FILE = "/provenanceData.tab";
    private static final short PARENT_IDS_INDEX = 0;
    private static final short CHILD_ID_INDEX = 1;
    private static final short MEDIA_TYPE_INDEX = 2;
    private static Map<String, List<String>> provenanceMap = null;
    private static Set<String> parentIds = null;
    private static boolean passThroughMode = false;

    private KsfProvenanceData() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    static {
        TEXT_MEDIA_TYPES = Arrays.asList("text/plain", "application/msword", "application/pdf", "application/rtf");
        IMAGE_MEDIA_TYPES = Arrays.asList("image/jpg", "image/jpeg", "image/gif", "image/png", "image/bmp", "image/vnd.microsoft.icon", "image/tiff");
        AUDIO_MEDIA_TYPES = Arrays.asList("audio/mp3", "audio/aac", "audio/midi", "audio/x-midi", "audio/ogg", "audio/wav");
        VIDEO_MEDIA_TYPES = Arrays.asList("video/jpeg", "video/mp4", "video/mpeg", "video/ogg", "video/quicktime", "video/raw", "video/x-msvideo", "application/x-shockwave-flash");
        COMBO_MEDIA_TYPES.addAll(IMAGE_MEDIA_TYPES);
        COMBO_MEDIA_TYPES.addAll(VIDEO_MEDIA_TYPES);
        COMBO_MEDIA_TYPES.addAll(AUDIO_MEDIA_TYPES);
    }

    /**
     * Load dataset-specific provenance data from disk.  If pass-through mode is desired,
     * then call {@link #setPassThroughMode(boolean)} prior to this call to avoid needless loading.
     * Provenance data will only be loaded once (subsequent calls will be ignored).
     */
    public static void loadProvenanceData() {
        if (passThroughMode) {
            return;
        }

        if (provenanceMap == null) { // only initialize once
            provenanceMap = new HashMap<>();
            parentIds = new HashSet<>();
            try {
                // Convert target filename to a list of Strings
                InputStream inputData = KsfProvenanceData.class.getResourceAsStream(PROVENANCE_DATA_FILE);
                List<String> rawProvenanceData = IOUtils.readLines(inputData, StandardCharsets.UTF_8);

                rawProvenanceData.remove(0); // remove header
                for (String line : rawProvenanceData) {
                    String[] fields = line.split("\t");
                    parentIds.add(fields[PARENT_IDS_INDEX]);
                    if (!"n/a".equals(fields[CHILD_ID_INDEX])) {
                        switch (fields[MEDIA_TYPE_INDEX]) {
                            case ".ltf.xml" :
                                provenanceMap.put(fields[CHILD_ID_INDEX], TEXT_MEDIA_TYPES);
                                break;
                            case ".mp4.ldcc" :
                                provenanceMap.put(fields[CHILD_ID_INDEX], VIDEO_MEDIA_TYPES);
                                break;
                            case ".mp3.ldcc" :
                                provenanceMap.put(fields[CHILD_ID_INDEX], AUDIO_MEDIA_TYPES);
                                break;
                            case ".gif.ldcc" :
                            case ".jpg.ldcc" :
                            case ".png.ldcc" :
                            case ".svg.ldcc" :
                                provenanceMap.put(fields[CHILD_ID_INDEX], IMAGE_MEDIA_TYPES);
                                break;
                            default:
                                break; // Ignore unknown media types
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error loading provenance data", e);
            }
        }
    }

    /**
     * Enable/disable pass-through mode, in which all valid checks always return true.
     * If pass-through mode is desired, then call this prior to {@link #loadProvenanceData()} to avoid needless loading.
     * @param newValue true to enable pass-through mode, otherwise false
     */
    public static void setPassThroughMode(boolean newValue) {
        passThroughMode = newValue;
    }

    private static boolean isPassThroughMode() {
        if (passThroughMode) {
            return true;
        }
        if (provenanceMap == null || parentIds == null) {
            throw new IllegalStateException("KsfProvenanceData uninitialized.");
        }
        return false;
    }

    /**
     * Test whether the specified childID a valid childID in the pre-loaded LDC dataset.
     * @param childID the {@link String} childID to test
     * @return true if childID is a childID in the pre-loaded LDC dataset, false otherwise
     */
    public static boolean isValidChildID(String childID) {
        return isPassThroughMode() || provenanceMap.containsKey(childID);
    }

    /**
     * Test whether the specified parentID a valid parentID in the pre-loaded LDC dataset.
     * @param parentID the {@link String} parentID to test
     * @return true if parentID is a parentID in the pre-loaded LDC dataset, false otherwise
     */
    public static boolean isValidParentID(String parentID) {
        return isPassThroughMode() || parentIds.contains(parentID);
    }

    /**
     * Test whether the specified mediaType matches the specified childID in the pre-loaded LDC dataset.
     * If the childID doesn't exist in the dataset,
     * @param childID a {@link String} childID
     * @param mediaType the {@link String} mediaType to test against childID
     * @return true if mediaType matches the media type of childID, or if childID is not found; otherwise false
     */
    public static boolean isValidMediaTypeForId(String childID, String mediaType) {
        if (isPassThroughMode()) {
            return true;
        } else {
            List<String> mediaTypes = provenanceMap.get(childID);
            return mediaTypes == null || mediaTypes.contains(mediaType);
        }
    }
}
