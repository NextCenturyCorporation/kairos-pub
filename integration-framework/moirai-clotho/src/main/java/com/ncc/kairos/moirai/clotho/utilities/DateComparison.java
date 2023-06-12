package com.ncc.kairos.moirai.clotho.utilities;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateComparison {
    private static final String OFFSET_MINUS_14_00 = "-14:00";
    private static final String OFFSET_PLUS_14_00 = "+14:00";
    private static final String LOCAL = "LOCAL";
    private static final String ZONED = "ZONED";

    private DateComparison() {
        throw new IllegalStateException("Utility class, not to be instantiated.");
    }

    private static String localOrZoned(String d1) {
        try {
            LocalDateTime.parse(d1);
            return LOCAL;
        } catch (Exception e) {
            try {
                ZonedDateTime.parse(d1);
                return ZONED;
            } catch (Exception e2) {
                return "";
            }
        }
    }

    static boolean before(String d1, String d2) {
        if (isNullOrEmptyString(d1) || isNullOrEmptyString(d2)) {
            return false;
        }
        String d1Type = localOrZoned(d1);
        String d2Type = localOrZoned(d2);

        /* Since it isn't correct to assume which timezone should apply to a LocalDateTime
         * we check the max range of total time zones and see if we can get a consistent answer over the whole range.
         * Eg. comparing the LocalDateTime 2021:01:01T00:00:00 to the ZonedDateTime 2021:01:01T01:00:00Z would give different answers depends on which timezone you assume.
         * Eg. comparing the LocalDateTime 2021:01:01T00:00:00 to the ZonedDateTime 2018:01:01T01:00:00Z would always give the same answer because they are so far apart.
         */

        if (d1Type.equals(LOCAL) && d2Type.equals(LOCAL)) {
            return LocalDateTime.parse(d1).compareTo(LocalDateTime.parse(d2)) < 0;
        } else if (d1Type.equals(ZONED) && d2Type.equals(ZONED)) {
            return ZonedDateTime.parse(d1).compareTo(ZonedDateTime.parse(d2)) < 0;
        } else if (d1Type.equals(LOCAL) && d2Type.equals(ZONED)) {
            LocalDateTime trueD1 = LocalDateTime.parse(d1);
            ZonedDateTime earliestD1 = ZonedDateTime.of(trueD1, ZoneId.of(OFFSET_PLUS_14_00));
            ZonedDateTime latestD1 = ZonedDateTime.of(trueD1, ZoneId.of(OFFSET_MINUS_14_00));
            ZonedDateTime trueD2 = ZonedDateTime.parse(d2);
            return (earliestD1.compareTo(trueD2) < 0 && latestD1.compareTo(trueD2) < 0);
        } else if (d1Type.equals(ZONED) && d2Type.equals(LOCAL)) {
            ZonedDateTime trueD1 = ZonedDateTime.parse(d1);
            LocalDateTime trueD2 = LocalDateTime.parse(d2);
            ZonedDateTime earliestD2 = ZonedDateTime.of(trueD2, ZoneId.of(OFFSET_PLUS_14_00));
            ZonedDateTime latestD2 = ZonedDateTime.of(trueD2, ZoneId.of(OFFSET_MINUS_14_00));
            return (trueD1.compareTo(earliestD2) < 0 && trueD1.compareTo(latestD2) < 0);
        } else {
            return false;
        }
    }

    static boolean beforeEqual(String d1, String d2) {
        if (isNullOrEmptyString(d1) || isNullOrEmptyString(d2)) {
            return false;
        }
        String d1Type = localOrZoned(d1);
        String d2Type = localOrZoned(d2);

        /* Since it isn't correct to assume which timezone should apply to a LocalDateTime
         * we check the max range of total time zones and see if we can get a consistent answer over the whole range.
         * Eg. comparing the LocalDateTime 2021:01:01T00:00:00 to the ZonedDateTime 2021:01:01T01:00:00Z would give different answers depends on which timezone you assume.
         * Eg. comparing the LocalDateTime 2021:01:01T00:00:00 to the ZonedDateTime 2018:01:01T01:00:00Z would always give the same answer because they are so far apart.
         */

        if (d1Type.equals(LOCAL) && d2Type.equals(LOCAL)) {
            return LocalDateTime.parse(d1).compareTo(LocalDateTime.parse(d2)) <= 0;
        } else if (d1Type.equals(ZONED) && d2Type.equals(ZONED)) {
            return ZonedDateTime.parse(d1).compareTo(ZonedDateTime.parse(d2)) <= 0;
        } else if (d1Type.equals(LOCAL) && d2Type.equals(ZONED)) {
            LocalDateTime trueD1 = LocalDateTime.parse(d1);
            ZonedDateTime earliestD1 = ZonedDateTime.of(trueD1, ZoneId.of(OFFSET_PLUS_14_00));
            ZonedDateTime latestD1 = ZonedDateTime.of(trueD1, ZoneId.of(OFFSET_MINUS_14_00));
            ZonedDateTime trueD2 = ZonedDateTime.parse(d2);
            return (earliestD1.compareTo(trueD2) <= 0 && latestD1.compareTo(trueD2) <= 0);
        } else if (d1Type.equals(ZONED) && d2Type.equals(LOCAL)) {
            ZonedDateTime trueD1 = ZonedDateTime.parse(d1);
            LocalDateTime trueD2 = LocalDateTime.parse(d2);
            ZonedDateTime earliestD2 = ZonedDateTime.of(trueD2, ZoneId.of(OFFSET_PLUS_14_00));
            ZonedDateTime latestD2 = ZonedDateTime.of(trueD2, ZoneId.of(OFFSET_MINUS_14_00));
            return (trueD1.compareTo(earliestD2) <= 0 && trueD1.compareTo(latestD2) <= 0);
        } else {
            return false;
        }
    }

    static boolean isNullOrEmptyString(String s) {
        return s == null || s.trim().isEmpty();
    }

}
