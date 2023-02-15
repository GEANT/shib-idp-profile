package org.geant.shibboleth.plugin.userprofile.utils;

import java.util.Date;

public final class DateUtil {

    /** Constructor. */
    private DateUtil() {
    }

    public static Date epochSecondsToDate(long seconds) {
        return new Date(seconds * 1000);
    }

    public static Date epochSecondsToDate(String inSeconds) {
        return epochSecondsToDate(Long.parseLong(inSeconds));
    }

}
