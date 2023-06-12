package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import com.ncc.kairos.moirai.clotho.model.SchemaEvent;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class ValidationUtils {

    private ValidationUtils() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static String safeGetString(String s) {
        return s == null ? "" : s;
    }

    public static String safeGetFirstString(List<String> list) {
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return "";
        } else {
            return list.get(0);
        }
    }

    // Return only non-blank strings from a list of strings
    public static List<String> safeGetStrings(List<String> strings) {
        List<String> valuesToReturn = new ArrayList<>();
        if (strings != null) {
            for (String string : strings) {
                if (string != null && !string.isBlank()) {
                    valuesToReturn.add(string);
                }
            }
        }
        return valuesToReturn;
    }

    public static <T> boolean isNullOrEmptyArray(T[] arr) {
        return arr == null || arr.length < 1 || arr[0] == null;
    }

    public static <T> boolean isNullOrEmptyList(List<T> list) {
        return list == null || list.isEmpty() || list.get(0) == null;
    }

    public static <T> boolean isNonEmptyList(List<T> list) {
        return !isNullOrEmptyList(list);
    }

    public static <T> boolean removeEmptyValues(List<T> list) {
        return list != null && list.removeIf(listItem -> listItem == null || listItem.toString().isBlank());
    }

    public static Set<String> getDuplicateStrings(List<String> list) {
        LinkedHashSet<String> duplicateElements = new LinkedHashSet<>(); // preserve insertion order to support tests
        if (list != null) {
            for (String outerId : list) {
                int i = 0;
                for (String innerId : list) {
                    if (outerId.equals(innerId) && ++i == 2) {
                        duplicateElements.add(outerId);
                        break;
                    }
                }
            }
        }
        return duplicateElements;
    }

    public static SchemaEvent getEventById(List<SchemaEvent> events, String targetId) {
        if (events != null && targetId != null) {
            for (SchemaEvent event : events) {
                if (targetId.equals(event.getAtId())) {
                    return event;
                }
            }
        }
        return null;
    }

    static boolean isValidLocalDateTime(LocalDateTime timeToValidate) {
        return timeToValidate != null && !timeToValidate.equals(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
    }

    static LocalDateTime safeParseLocalDateTime(String timeStringToParse) {
        LocalDateTime timeToReturn;
        try {
            if (timeStringToParse == null) {
                timeToReturn = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            } else {
                timeToReturn = LocalDateTime.parse(timeStringToParse);
            }
        } catch (DateTimeParseException ex) {
            // Parse exceptions will result in NULL for now
            timeToReturn = null;
        }
        return timeToReturn;
    }

    /**
     * Return the first non-blank string in a list of strings.
     * @param strings a list of strings
     * @return the first non-blank string in strings
     * @throws IllegalArgumentException if strings is null, or does not contain a non-blank string
     */
    public static String getFirstNonBlankString(List<String> strings) {
        if (strings != null) {
            for (String curS : strings) {
                if (!StringUtils.isBlank(curS)) {
                    return curS;
                }
            }
        }
        throw new IllegalArgumentException("argument must contain at least one non-blank string.");
    }
}
