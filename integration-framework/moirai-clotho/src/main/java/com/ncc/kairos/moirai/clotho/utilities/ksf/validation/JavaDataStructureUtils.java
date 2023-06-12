package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public final class JavaDataStructureUtils {

    private JavaDataStructureUtils() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    public static boolean isNullOrEmptyString(String value) {
        return StringUtils.isAllEmpty(value);
    }

    private static void safePutMapKey(Map<String, List<String>> map, String key) {
        if (map != null && !isNullOrEmptyString(key) && !map.containsKey(key)) { // NOSONAR, because the prescribed solution throws a ConcurrentModificationException.
            map.put(key, new ArrayList<>());
        }
    }

    public static void safePutMapKeysIfEmpty(Map<String, List<String>> map, Set<String> keys) {
        if (map != null && keys != null) {
            for (String curKey : keys) {
                if (!map.containsKey(curKey)) {
                    JavaDataStructureUtils.safePutMapKey(map, curKey);
                }
            }
        }
    }

    private static void safePutMapVal(Map<String, List<String>> map, String key, String val) {
        if (map != null && !isNullOrEmptyString(key) && !isNullOrEmptyString(val)) {
            if (map.containsKey(key) && map.get(key) != null) {
                List<String> existingValList = map.get(key);
                if (!existingValList.contains(val)) {
                    existingValList.add(val);
                    map.put(key, existingValList);
                }
            } else {
                ArrayList<String> valToAdd = new ArrayList<>();
                valToAdd.add(val);
                map.put(key, valToAdd);
            }
        }
    }

    private static void safePutMapVal(Map<String, List<String>> map, String key, List<String> vals) {
        if (map != null && !isNullOrEmptyString(key) && vals != null) {
            for (String curValToAdd : vals) {
                safePutMapVal(map, key, curValToAdd);
            }
        }
    }

    public static void safePutMapKeysVals(Map<String, List<String>> map, List<String> keys, List<String> vals) {
        if (map != null && keys != null && vals != null) {
            for (String curKey : keys) {
                JavaDataStructureUtils.safePutMapVal(map, curKey, vals);
            }
        }
    }

}
