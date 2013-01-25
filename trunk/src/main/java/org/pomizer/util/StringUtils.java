package org.pomizer.util;

import java.util.regex.Pattern;

public class StringUtils {

    public static String extractFileName(final String filePathName) {

        if (null == filePathName) {
            return null;
        }

        int dotPos = filePathName.lastIndexOf('.');
        int slashPos = filePathName.lastIndexOf('\\');
        if (slashPos == -1)
            slashPos = filePathName.lastIndexOf('/');

        if (dotPos > slashPos) {
            return filePathName.substring(slashPos > 0 ? slashPos + 1 : 0, dotPos);
        }

        return filePathName.substring(slashPos > 0 ? slashPos + 1 : 0);
    }

    public static String changeFileExtension(final String fileName, final String newExtension) {

        String result;
        int startOfExtension = fileName.lastIndexOf('.');
        if (-1 == startOfExtension) {
            result = fileName + '.' + "newExtension";
        }
        else {
            result = fileName.substring(0, startOfExtension + 1) + newExtension;
        }
        return result;
    }

    public static boolean areEqual(final String first, final String second) {

        if (first == second) {
            return true;
        }

        if (null != first) {
            return first.equals(second);
        }

        return false;
    }

    public static int hashCode(final String string) {
        if (null == string) {
            return 0;
        }
        return string.hashCode();
    }

    public static String convertPathToObjectName(final String path) {

        if (null == path) {
            return "";
        }

        String normalizedPath = path.replace('\\', ' ').replace('/', ' ').replace(':', ' ').replace('-', ' ');
        normalizedPath = normalizedPath.replace('$', ' ');
        String[] pathParts = normalizedPath.split(Pattern.quote(" "));
        String result = "";
        for (int i = 0; i < pathParts.length; i++) {
            if (!pathParts[i].isEmpty()) {
                result += "." + pathParts[i];
            }
        }

        if (result.length() > 0) {
            result = result.substring(1).toLowerCase();
        }

        return result;
    }

    public static boolean isNullOrEmpty(final String value) {
        return (null == value) || value.isEmpty();
    }
    
    public static int getCharOccurenceCount(final String string, final char character) {
        
        int result = 0;
        
        if (!isNullOrEmpty(string)) {
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) == character) {
                    result++;
                }
            }
        }
        
        return result;
    }
}