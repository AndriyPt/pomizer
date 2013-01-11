package org.pomizer.application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.pomizer.comparator.JarIndexComparator;
import org.pomizer.comparator.RawClassInfoComparator;
import org.pomizer.constant.GlobalSettings;
import org.pomizer.model.PackageInfo;
import org.pomizer.model.RawClassInfo;
import org.pomizer.model.RawJarInfo;
import org.pomizer.render.JarIndexRenderer;
import org.pomizer.util.ClassUtils;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.StringUtils;

public class JarIndexer {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out
                    .println("Application usage: <program> <version> <base path 1> [<base path 2> ... [<base path N>]] \n");
            System.exit(1);
        }

        String version = args[0];

        List<String> basePaths = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            JavaUtils.checkDirectoryExists(args[i]);
            if (args[i].endsWith(File.separator)) {
                basePaths.add(args[i]);
            }
            else {
                basePaths.add(args[i] + File.separator);
            }
        }

        processJarDirectory(version, basePaths);
    }

    private static void processJarDirectory(final String version, final List<String> basePaths) {

        List<RawJarInfo> jarNames = new ArrayList<RawJarInfo>();
        List<PackageInfo> packageNames = new ArrayList<PackageInfo>();
        List<RawClassInfo> classNames = new ArrayList<RawClassInfo>();

        try {

            Collections.sort(basePaths);

            for (int i = 0; i < basePaths.size(); i++) {

                JavaUtils.printToConsole("Scanning directory \"" + basePaths.get(i) + "\" for JAR files...");

                Collection<File> jarFiles = FileUtils.listFiles(new File(basePaths.get(i)), new String[] { "jar" },
                        true);

                JavaUtils.printToConsole("Found " + jarFiles.size() + " files");
                processClasses(basePaths.get(i), i, jarFiles, jarNames, packageNames, classNames);
            }
            sortIndeces(jarNames, packageNames, classNames);
            saveIndeces(version, basePaths, jarNames, packageNames, classNames);

            JavaUtils.printToConsole("Finished");

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveIndeces(final String version, final List<String> basePaths, final List<RawJarInfo> jarNames,
            final List<PackageInfo> packageNames, final List<RawClassInfo> classNames) throws IOException {

        JavaUtils.printToConsole("Saving index to file ...");
        FileWriter fileWritter = null;
        try {
            fileWritter = new FileWriter(JavaUtils.getPathToFileInCurrentDirectory(GlobalSettings.INDEX_FILE_NAME));
            JarIndexRenderer.writeHeader(fileWritter, version, basePaths.size(), jarNames.size(), packageNames.size(),
                    classNames.size());
            JarIndexRenderer.writeBasePaths(fileWritter, basePaths);
            JarIndexRenderer.writeJarFiles(fileWritter, jarNames);
            JarIndexRenderer.writePackageNames(fileWritter, packageNames);
            JarIndexRenderer.writeClassNames(fileWritter, classNames);
        }
        finally {
            if (null != fileWritter) {
                try {
                    fileWritter.flush();
                    fileWritter.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void sortIndeces(final List<RawJarInfo> jarNames, final List<PackageInfo> packageNames,
            final List<RawClassInfo> classNames) throws Exception {
        JavaUtils.printToConsole("Sorting indeces ...");
        
        RawClassInfoComparator rawClassInfoComparator = new RawClassInfoComparator();
        
        Collections.sort(jarNames);
        Collections.sort(packageNames);
        Collections.sort(classNames, rawClassInfoComparator);
        
        JavaUtils.printToConsole("Calculating index information ...");
        int classListLength = classNames.size();
        for (int i = 0; i < classListLength; i++) {
            RawClassInfo classInfo = classNames.get(i);
            int j = i + 1; 
            while ((j < classListLength) && (0 == rawClassInfoComparator.compare(classInfo, classNames.get(j)))) {
                RawClassInfo classInfoToMerge = classNames.get(j);
                for (int k = 0; k < classInfoToMerge.jarInfoList.size(); k++) {
                    if (-1 == classInfo.jarInfoList.indexOf(classInfoToMerge.jarInfoList.get(k))) {
                        classInfo.jarInfoList.add(classInfoToMerge.jarInfoList.get(k));
                    }
                }
                classNames.remove(j);
                classListLength--;
            }
        }

        adjustSortedIndeces(jarNames, packageNames, classNames);
    }

    private static void adjustSortedIndeces(final List<RawJarInfo> jarNames, final List<PackageInfo> packageNames,
            final List<RawClassInfo> classNames) throws Exception {
        
        JarIndexComparator jarIndexComparator = new JarIndexComparator(jarNames);
        for (int i = 0; i < classNames.size(); i++) {
            RawClassInfo classInfo = classNames.get(i);
            
            List<Integer> jarFileIndeces = new ArrayList<Integer>();
            for (int j = 0; j < classInfo.jarInfoList.size(); j++) {
                int jarIndex = Collections.binarySearch(jarNames, classInfo.jarInfoList.get(j));
                if (jarIndex < 0) {
                    throw new Exception("Unknown jar \"" + classInfo.jarInfoList.get(j).name + "\" for class \"" + classInfo.name
                            + "\"");
                }
                jarFileIndeces.add(jarIndex);
            }
            Collections.sort(jarFileIndeces, jarIndexComparator);
            classInfo.jarFileIndeces = new int[jarFileIndeces.size()];
            for (int j = 0; j < jarFileIndeces.size(); j++) {
                classInfo.jarFileIndeces[j] = jarFileIndeces.get(j);
            }
                
            int packageIndex = Collections.binarySearch(packageNames, classInfo.packageInfo);
            if (packageIndex < 0) {
                throw new Exception("Unknown package \"" + classInfo.packageInfo.name + "\" for class \""
                        + classInfo.name + "\"");
            }
            classInfo.packageIndex = packageIndex;
            
            List<Integer> packageJarIndeces = packageNames.get(packageIndex).jarIndeces;
            for (int j = 0; j < jarFileIndeces.size(); j++) {
                if (-1 == packageJarIndeces.indexOf(jarFileIndeces.get(j))) {
                    packageJarIndeces.add(jarFileIndeces.get(j));
                }
            }
        }

        for (int i = 0; i < packageNames.size(); i++) {
            Collections.sort(packageNames.get(i).jarIndeces, jarIndexComparator);
        }
    }

    private static void processClasses(final String basePath, final int basePathIndex, Collection<File> jarFiles,
            List<RawJarInfo> jarNames, List<PackageInfo> packageNames, List<RawClassInfo> classNames) throws IOException {
        final String CLASS_EXTENSION = ".class";

        int basePathLength = basePath.length();
        for (File foundFile : jarFiles) {
            JavaUtils.printToConsole("Processing " + foundFile.getAbsolutePath() + "...");

            String relativeJarFileName = foundFile.getAbsolutePath().substring(basePathLength);
            if (!JavaUtils.containsDirectoriesInPath(relativeJarFileName, "JRE", "JRE64", "TMP")) {
                RawJarInfo currentJarInfo = new RawJarInfo(relativeJarFileName, basePathIndex, 
                        foundFile.lastModified());
                jarNames.add(currentJarInfo);

                JarFile jarFile = new JarFile(foundFile);
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.endsWith(CLASS_EXTENSION)) {
                        entryName = entryName.substring(0, entryName.length() - CLASS_EXTENSION.length());
                        entryName = entryName.replace('/', '.');

                        String packageName = ClassUtils.getPackageFromFullName(entryName);
                        String className = ClassUtils.getClassNameFromFullName(entryName);

                        if (!StringUtils.isNullOrEmpty(packageName)) {

                            // Nested class handling
                            // TODO: Add here more logic to handle classes MyName$1.class
                            className = className.replace('$', '.');

                            PackageInfo packageInfo = new PackageInfo();
                            packageInfo.name = packageName;
                            int packageIndex = packageNames.indexOf(packageInfo);
                            if (-1 == packageIndex) {
                                packageNames.add(packageInfo);
                            }
                            else {
                                packageInfo = packageNames.get(packageIndex);
                            }

                            RawClassInfo classInfo = new RawClassInfo();
                            classInfo.name = className;
                            classInfo.jarInfoList.add(currentJarInfo);
                            classInfo.packageInfo = packageInfo;
                            classNames.add(classInfo);
                        }
                    }
                }
            }
        }
    }
}
