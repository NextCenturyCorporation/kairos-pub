package com.ncc.kairos.moirai.zeus.utililty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public final class TimeUtil {

    // Private null constructor
    private TimeUtil() {
    }

    public static OffsetDateTime getDatabaseTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }

    public static String getTimeInSeconds() {
        return String.valueOf(System.currentTimeMillis());
    }
}
