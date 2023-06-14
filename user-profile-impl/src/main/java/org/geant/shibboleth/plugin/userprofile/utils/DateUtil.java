package org.geant.shibboleth.plugin.userprofile.utils;

import java.util.Date;

/**
 * Helper for date operations.
 */
public final class DateUtil {

    /** Constructor. */
    private DateUtil() {
    }

    /**
     * Seconds from epoch to Date.
     * 
     * @param seconds seconds from epoch
     * @return Date instance
     */
    public static Date epochSecondsToDate(long seconds) {
        return new Date(seconds * 1000);
    }

    /**
     * Seconds from epoch to Date.
     * 
     * @param inSeconds string representing seconds from epoch
     * @return Date Instance
     */
    public static Date epochSecondsToDate(String inSeconds) {
        return epochSecondsToDate(Long.parseLong(inSeconds));
    }

}
