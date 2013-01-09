package org.pomizer.util;

public class ClassUtils {

    public static String getClassNameFromFullName(final String fullName) {
        String result = fullName.trim();
        int lastIndexOfDot = result.lastIndexOf('.');
        if (lastIndexOfDot > -1) {
            result = result.substring(lastIndexOfDot + 1);
        }
        return result;
    }

    public static String getPackageFromFullName(final String fullName) {
        String result = fullName.trim();
        int lastIndexOfDot = result.lastIndexOf('.');
        if (lastIndexOfDot > -1) {
            result = result.substring(0, lastIndexOfDot);
        }
        else {
            result = "";
        }
        return result;
    }
}
