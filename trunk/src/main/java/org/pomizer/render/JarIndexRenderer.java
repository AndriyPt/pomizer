package org.pomizer.render;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.pomizer.model.ClassInfo;
import org.pomizer.model.PackageInfo;
import org.pomizer.model.RawClassInfo;
import org.pomizer.model.RawJarInfo;

public class JarIndexRenderer {

    public static final String VALUES_DELIMITER = "|";

    public static void writeHeader(final FileWriter fileWritter, final String version, final int basePathsCount,
            final int jarsCount, final int packagesCount, final int classesCount) throws IOException {

        fileWritter.write(version + '\n');
        fileWritter.write("" + basePathsCount + '\n');
        fileWritter.write("" + jarsCount + '\n');
        fileWritter.write("" + packagesCount + '\n');
        fileWritter.write("" + classesCount + '\n');
    }

    public static void writeBasePaths(final FileWriter fileWritter, final List<String> basePaths) throws IOException {

        for (String basePath : basePaths) {
            fileWritter.write(basePath + '\n');
        }
    }

    public static void writeJarFiles(final FileWriter fileWritter, final List<RawJarInfo> jarNames) throws IOException {

        for (RawJarInfo jarInfo : jarNames) {
            fileWritter.write(jarInfo.name + VALUES_DELIMITER + jarInfo.basePathIndex + '\n');
        }
    }

    public static void writePackageNames(final FileWriter fileWritter, final List<PackageInfo> packageNames)
            throws IOException {

        for (PackageInfo packageInfo : packageNames) {
            String line = packageInfo.name;

            for (Integer jarIndex : packageInfo.jarIndeces) {
                line += VALUES_DELIMITER + jarIndex;
            }
            fileWritter.write(line + '\n');
        }
    }

    public static void writeClassNames(final FileWriter fileWritter, final List<RawClassInfo> classNames)
            throws IOException {

        for (ClassInfo classInfo : classNames) {
            String line = classInfo.name + VALUES_DELIMITER + classInfo.packageIndex;
            
            for (int i = 0; i < classInfo.jarFileIndeces.length; i++) {
                line += VALUES_DELIMITER + classInfo.jarFileIndeces[i]; 
            }
            
            fileWritter.write(line + '\n');
        }
    }
}
