package org.pomizer.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.pomizer.constant.GlobalSettings;
import org.pomizer.model.Dependency;
import org.pomizer.model.IndexInfo;
import org.pomizer.model.JarInfo;
import org.pomizer.render.JarIndexRenderer;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.MavenUtils;

public class PomFromIndexCreator {

    private static final int MAX_ITERATIONS_COUNT = 10;

    public static void main(String[] args) {

        if (args.length < 2) {
            JavaUtils.printToConsole(String.format("Application usage: <program> <index file> <project directory> "
                    + "[<sources relative path, default \"%s\">] \n", GlobalSettings.DEFAULT_SOURCES_DIR));
            System.exit(1);
        }

        final String indexFileName = args[0];
        JavaUtils.checkFileExists(indexFileName);

        final String projectDirectory = args[1];
        JavaUtils.checkDirectoryExists(projectDirectory);

        String relativeSourcesPath = GlobalSettings.DEFAULT_SOURCES_DIR;
        if (args.length > 2) {
            relativeSourcesPath = args[2];
        }
        JavaUtils.checkDirectoryExists(JavaUtils.combinePaths(projectDirectory, relativeSourcesPath));

        processFile(indexFileName, projectDirectory, relativeSourcesPath);
    }

    private static void processFile(final String indexFileName, final String projectDirectory,
            final String relativeSourcesPath) {

        final String pomFileName = JavaUtils.combinePaths(projectDirectory, "pom.xml");
        final String projectName = new File(projectDirectory).getName();
        final List<Dependency> dependencies = new ArrayList<Dependency>();

        IndexInfo indeces = loadIndeces(indexFileName);

        File pomFile = new File(pomFileName);
        if (!pomFile.exists()) {
            JavaUtils.printToConsole("Saving POM stub...");
            MavenUtils.savePomFile(dependencies, projectName, pomFileName, relativeSourcesPath);
        }

        JavaUtils.printToConsole("Parsing POM file...");
        MavenUtils.readPomDependencies(pomFileName, dependencies);
        assignJarIndexToDependency(dependencies, indeces);

        final List<JarInfo> newJarDependencies = new ArrayList<JarInfo>();
        final List<String> missingPackageErrors = new ArrayList<String>();
        final List<String> missingClassErrors = new ArrayList<String>();
        final List<AbstractMap.SimpleEntry<String, String>> missingClassWithPackageErrors = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
        try {
            for (int i = 0; (i < MAX_ITERATIONS_COUNT)
                    && !MavenUtils.compilePomFile(pomFileName, missingPackageErrors, missingClassErrors,
                            missingClassWithPackageErrors); i++) {

                processMissingErrors(indeces, newJarDependencies, missingPackageErrors, missingClassWithPackageErrors,
                        missingClassErrors);

                for (int j = 0; j < newJarDependencies.size(); j++) {
                    JavaUtils.printToConsole(String.format("Installing file \"%s\" to local repository...",
                            newJarDependencies.get(j).name));
                    Dependency newDependency = MavenUtils.installJarFile(indeces.version,
                            indeces.basePaths[newJarDependencies.get(j).basePathIndex], newJarDependencies.get(j).name);
                    if (-1 == dependencies.indexOf(newDependency)) {
                        newDependency.jarIndex = newJarDependencies.get(j).index;
                        dependencies.add(newDependency);
                    }
                }

                newJarDependencies.clear();
                missingPackageErrors.clear();
                missingClassWithPackageErrors.clear();
                missingClassErrors.clear();

                JavaUtils.printToConsole("Saving POM file...");
                MavenUtils.savePomFile(dependencies, projectName, pomFileName, relativeSourcesPath);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        JavaUtils.printToConsole("Finished");
    }

    private static void assignJarIndexToDependency(final List<Dependency> dependencies, final IndexInfo indeces) {
        if (dependencies.size() > 0) {
            for (int i = 0; i < indeces.jarNames.length; i++) {
                int basePathIndex = indeces.jarNamesBasePathIndex[i]; 
                Dependency jarDependency = MavenUtils.createDependecy(indeces.basePaths[basePathIndex], 
                        indeces.jarNames[i], indeces.version);
                int dependencyIndex = dependencies.indexOf(jarDependency);
                if (-1 < dependencyIndex) {
                    dependencies.get(dependencyIndex).jarIndex = i;
                }
            }
        }
    }

    private static void processMissingErrors(final IndexInfo indeces, final List<JarInfo> newJarDependencies,
            final List<String> missingPackageErrors,
            final List<SimpleEntry<String, String>> missingClassWithPackageErrors, final List<String> missingClassErrors)
            throws Exception {

        for (int j = 0; j < missingPackageErrors.size(); j++) {
            JavaUtils.printToConsole("Resolving package: " + missingPackageErrors.get(j) + "...");
            int packageIndex = Arrays.binarySearch(indeces.packageNames, missingPackageErrors.get(j));
            if (packageIndex < 0) {
                // Move this package to class list in case if this is bad compiler assumption
                missingClassErrors.add(missingPackageErrors.get(j));
            }
            else {
                int packageJarIndex = indeces.packageNamesJarIndeces[packageIndex][0];
                JarInfo newJarDependecy = new JarInfo(indeces.jarNames[packageJarIndex],
                        indeces.jarNamesBasePathIndex[packageJarIndex], packageJarIndex);

                if (-1 == newJarDependencies.indexOf(newJarDependecy)) {
                    newJarDependencies.add(newJarDependecy);
                }
            }
        }

        for (int j = 0; j < missingClassWithPackageErrors.size(); j++) {
            processMissingClassErrors(indeces, missingClassWithPackageErrors.get(j).getKey(),
                    missingClassWithPackageErrors.get(j).getValue(), newJarDependencies);
        }

        if ((0 == missingPackageErrors.size()) && (0 == missingClassWithPackageErrors.size())) {
            for (int j = 0; j < missingClassErrors.size(); j++) {
                processMissingClassErrors(indeces, missingClassErrors.get(j), null, newJarDependencies);
            }
        }
    }

    private static void processMissingClassErrors(final IndexInfo indeces, final String className,
            final String packageName, final List<JarInfo> newJarDependencies) throws Exception {

        boolean matchPackage = false;
        if ((null != packageName) && (!packageName.isEmpty())) {
            matchPackage = true;
        }

        if (matchPackage) {
            JavaUtils.printToConsole("Resolving class: " + packageName + "." + className + "...");
        }
        else {
            JavaUtils.printToConsole("Resolving class: " + className + "...");
        }

        int classIndex = Arrays.binarySearch(indeces.classNames, className);
        if (classIndex < 0) {
            throw new Exception("Could not find jar for class: " + className);
        }
        int lowerClassIndex = classIndex;
        while ((lowerClassIndex >= 0) && (indeces.classNames[lowerClassIndex].equals(className))) {
            lowerClassIndex--;
        }
        lowerClassIndex++;

        int upperClassIndex = classIndex;
        while ((upperClassIndex < indeces.classesCount) && (indeces.classNames[upperClassIndex].equals(className))) {
            upperClassIndex++;
        }
        upperClassIndex--;

        for (int k = lowerClassIndex; k <= upperClassIndex; k++) {

            if (matchPackage) {
                int packageIndex = indeces.classNamesPackageIndex[k];
                if (indeces.packageNames[packageIndex].equals(packageName)) {
                    addClassDependency(indeces, newJarDependencies, k);
                }
            }
            else {
                addClassDependency(indeces, newJarDependencies, k);
            }
        }
    }

    private static void addClassDependency(final IndexInfo indeces, final List<JarInfo> newJarDependencies,
            final int position) {

        //TODO: Add here more logic for JARs selecting in case if package is not still found
        int classJarIndex = indeces.classNamesJarIndeces[position][0];
        JarInfo newJarDependecy = new JarInfo(indeces.jarNames[classJarIndex],
                indeces.jarNamesBasePathIndex[classJarIndex], classJarIndex);

        if (-1 == newJarDependencies.indexOf(newJarDependecy)) {
            newJarDependencies.add(newJarDependecy);
        }
    }

    private static IndexInfo loadIndeces(final String indexFileName) {

        JavaUtils.printToConsole("Loading indeces...");
        IndexInfo indeces = null;
        BufferedReader bufferedReader = null;
        try {

            bufferedReader = new BufferedReader(new FileReader(indexFileName));

            indeces = new IndexInfo(bufferedReader.readLine(), Integer.parseInt(bufferedReader.readLine()),
                    Integer.parseInt(bufferedReader.readLine()), Integer.parseInt(bufferedReader.readLine()),
                    Integer.parseInt(bufferedReader.readLine()));

            loadBasePathsIndex(indeces, bufferedReader);

            loadJarsIndex(indeces, bufferedReader);

            loadPackagesIndex(indeces, bufferedReader);

            loadClassesIndex(indeces, bufferedReader);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        finally {
            try {
                if (null != bufferedReader) {
                    bufferedReader.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return indeces;
    }

    private static void loadBasePathsIndex(final IndexInfo indeces, final BufferedReader bufferedReader) throws IOException, Exception {
        String line;
        for (int i = 0; i < indeces.basePathsCount; i++) {
            line = bufferedReader.readLine();
            if (null == line) {
                throw new Exception("Could not read all base paths. Finished on index " + i + " of "
                        + indeces.packagesCount);
            }
            indeces.basePaths[i] = line.trim();
        }
    }

    private static void loadJarsIndex(final IndexInfo indeces, final BufferedReader bufferedReader) throws IOException, Exception {
        String line;
        for (int i = 0; i < indeces.jarsCount; i++) {
            line = bufferedReader.readLine();
            if (null == line) {
                throw new Exception("Could not read all jars. Finished on index " + i + " of " + indeces.jarsCount);
            }
            String[] values = line.trim().split(Pattern.quote(JarIndexRenderer.VALUES_DELIMITER));
            if (values.length <= 1) {
                throw new Exception("Jar without base path index. Finished on index " + i + " of "
                        + indeces.jarsCount);
            }

            indeces.jarNames[i] = values[0];
            indeces.jarNamesBasePathIndex[i] = Integer.parseInt(values[1]);
        }
    }

    private static void loadClassesIndex(final IndexInfo indeces, final BufferedReader bufferedReader) throws IOException, Exception {
        String line;
        for (int i = 0; i < indeces.classesCount; i++) {
            line = bufferedReader.readLine();
            if (null == line) {
                throw new Exception("Could not read all classes. Finished on index " + i + " of "
                        + indeces.classesCount);
            }
            String[] values = line.trim().split(Pattern.quote(JarIndexRenderer.VALUES_DELIMITER));
            if (values.length <= 2) {
                throw new Exception("Class without all indeces. Finished on index " + i + " of "
                        + indeces.classesCount);
            }

            indeces.classNames[i] = values[0];
            indeces.classNamesPackageIndex[i] = Integer.parseInt(values[1]);
            
            
            indeces.classNamesJarIndeces[i] = new int[values.length - 2];
            for (int j = 0; j < values.length - 2; j++) {
                indeces.classNamesJarIndeces[i][j] = Integer.parseInt(values[j + 2]);
            }
        }
    }

    private static void loadPackagesIndex(final IndexInfo indeces, final BufferedReader bufferedReader) throws IOException, Exception {
        String line;
        for (int i = 0; i < indeces.packagesCount; i++) {
            line = bufferedReader.readLine();
            if (null == line) {
                throw new Exception("Could not read all packages. Finished on index " + i + " of "
                        + indeces.packagesCount);
            }
            String[] values = line.trim().split(Pattern.quote(JarIndexRenderer.VALUES_DELIMITER));
            if (values.length <= 1) {
                throw new Exception("Package without jar file index. Finished on index " + i + " of "
                        + indeces.packagesCount);
            }

            indeces.packageNames[i] = values[0];
            indeces.packageNamesJarIndeces[i] = new int[values.length - 1];
            for (int j = 0; j < values.length - 1; j++) {
                indeces.packageNamesJarIndeces[i][j] = Integer.parseInt(values[j + 1]);
            }
        }
    }
}
