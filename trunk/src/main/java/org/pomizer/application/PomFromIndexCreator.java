package org.pomizer.application;

import java.io.File;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.tree.DefaultElement;
import org.pomizer.constant.GlobalSettings;
import org.pomizer.model.Dependency;
import org.pomizer.model.IndexInfo;
import org.pomizer.model.JarInfo;
import org.pomizer.util.IndexUtils;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.MavenUtils;
import org.pomizer.util.XmlUtils;

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
        JavaUtils.checkDirectoryExists(FilenameUtils.concat(projectDirectory, relativeSourcesPath));

        processFile(indexFileName, projectDirectory, relativeSourcesPath);
    }

    private static void processFile(final String indexFileName, final String projectDirectory,
            final String relativeSourcesPath) {

        final String pomFileName = FilenameUtils.concat(projectDirectory, "pom.xml");
        final String projectName = new File(projectDirectory).getName();
        final List<Dependency> dependencies = new ArrayList<Dependency>();

        IndexInfo indeces = IndexUtils.loadIndeces(indexFileName);

        File pomFile = new File(pomFileName);
        if (!pomFile.exists()) {
            JavaUtils.printToConsole("Saving POM stub...");
            MavenUtils.savePomFile(dependencies, projectName, pomFileName, relativeSourcesPath);
        }

        try {
            JavaUtils.printToConsole("Parsing POM file...");
            final Document pomDocument = XmlUtils.loadXmlDocument(pomFileName);
            final DefaultElement dependenciesNode = MavenUtils.readPomDependencies(pomDocument, dependencies);
            assignJarIndexToDependency(dependencies, indeces);

            final List<JarInfo> newJarDependencies = new ArrayList<JarInfo>();
            final List<String> missingPackageErrors = new ArrayList<String>();
            final List<String> missingClassErrors = new ArrayList<String>();
            final List<AbstractMap.SimpleEntry<String, String>> missingClassWithPackageErrors = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
            
            MavenUtils.executeCleanTask(pomFileName);
            for (int i = 0; (i < MAX_ITERATIONS_COUNT)
                    && !MavenUtils.compilePomFile(pomFileName, missingPackageErrors, missingClassErrors,
                            missingClassWithPackageErrors); i++) {

                processMissingErrors(indeces, dependencies, newJarDependencies, missingPackageErrors, 
                        missingClassWithPackageErrors, missingClassErrors);

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
                MavenUtils.mergeDependencies(dependenciesNode, dependencies);
                XmlUtils.saveXmlDocument(pomDocument, pomFileName);
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

    private static void processMissingErrors(final IndexInfo indeces, final List<Dependency> dependencies, 
            final List<JarInfo> newJarDependencies, final List<String> missingPackageErrors,
            final List<SimpleEntry<String, String>> missingClassWithPackageErrors, final List<String> missingClassErrors)
            throws Exception {

        int missingClassesMovedFromPackagesCount = 0;
        for (int j = 0; j < missingPackageErrors.size(); j++) {
            JavaUtils.printToConsole("Resolving package: " + missingPackageErrors.get(j) + "...");
            int packageIndex = Arrays.binarySearch(indeces.packageNames, missingPackageErrors.get(j));
            if (packageIndex < 0) {
                // Move this package to class list in case if this is bad compiler assumption
                missingClassErrors.add(missingPackageErrors.get(j));
                missingClassesMovedFromPackagesCount++;
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
            processMissingClassErrors(indeces, dependencies, missingClassWithPackageErrors.get(j).getKey(),
                    missingClassWithPackageErrors.get(j).getValue(), newJarDependencies);
        }

        if ((missingClassesMovedFromPackagesCount == missingPackageErrors.size()) && (0 == missingClassWithPackageErrors.size())) {
            for (int j = 0; j < missingClassErrors.size(); j++) {
                processMissingClassErrors(indeces, dependencies, missingClassErrors.get(j), null, newJarDependencies);
            }
        }
    }

    private static void processMissingClassErrors(final IndexInfo indeces, final List<Dependency> dependencies, 
            final String className, final String packageName, final List<JarInfo> newJarDependencies) throws Exception {

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
            JavaUtils.printToConsole("Error: Could not find jar for class: " + className);
        }
        else {
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
    
            boolean foundPackageWithClass = false;
            for (int k = lowerClassIndex; k <= upperClassIndex; k++) {
    
                if (matchPackage) {
                    int packageIndex = indeces.classNamesPackageIndex[k];
                    if (indeces.packageNames[packageIndex].equals(packageName)) {
                        addClassDependency(indeces, dependencies, newJarDependencies, k, true);
                        foundPackageWithClass = true;
                        break;
                    }
                }
                else {
                    addClassDependency(indeces, dependencies, newJarDependencies, k, false);
                }
            }
            if (!foundPackageWithClass && matchPackage) {
                JavaUtils.printToConsole("Error: Could not find jar for class: " + packageName + "." + className);
            }
        }
    }

    private static void addClassDependency(final IndexInfo indeces, final List<Dependency> dependencies, 
            final List<JarInfo> newJarDependencies, final int position, final boolean isClassWithPackage) {

        int classJarIndex = indeces.classNamesJarIndeces[position][0];
        if (isClassWithPackage && (indeces.classNamesJarIndeces[position].length > 1)) {
            
            boolean foundAlreadyDetectedJar = false;
            if (newJarDependencies.size() > 0) {
                for (int j = 0; (j < indeces.classNamesJarIndeces[position].length) && !foundAlreadyDetectedJar; j++) {
                    for (int k = 0; k < newJarDependencies.size(); k++) {
                        if (newJarDependencies.get(k).index == indeces.classNamesJarIndeces[position][j]) {
                            foundAlreadyDetectedJar = true;
                            classJarIndex = indeces.classNamesJarIndeces[position][j];
                            break;
                        }
                    }
                }
            }
            
            if (!foundAlreadyDetectedJar) {
                List<Integer> relatedIndeces = new ArrayList<Integer>();
                int smallestIndex = indeces.classNamesJarIndeces[position].length;
                for (int i = 0; i < dependencies.size(); i++) {
                    int dependecyJarIndex = dependencies.get(i).jarIndex; 
                    for (int j = 0; j < indeces.classNamesJarIndeces[position].length; j++) {
                        if (indeces.classNamesJarIndeces[position][j] == dependecyJarIndex) {
                            relatedIndeces.add(i);
                            if (j < smallestIndex) {
                                smallestIndex = j;
                            }
                            break;
                        }
                    }
                }
                
                if (relatedIndeces.size() > 0) {
                    for (int i = dependencies.size() - 1; i >= 0; i--) {
                        if (relatedIndeces.indexOf(i) >= 0) {
                            dependencies.remove(i);
                        }
                    }
                    
                    if (smallestIndex < indeces.classNamesJarIndeces[position].length - 1) {
                        classJarIndex = indeces.classNamesJarIndeces[position][smallestIndex + 1]; 
                    }
                    else {
                        classJarIndex = indeces.classNamesJarIndeces[position][0];
                    }
                }
            }
        }
        JarInfo newJarDependecy = new JarInfo(indeces.jarNames[classJarIndex],
                indeces.jarNamesBasePathIndex[classJarIndex], classJarIndex);

        if (-1 == newJarDependencies.indexOf(newJarDependecy)) {
            newJarDependencies.add(newJarDependecy);
        }
    }

}
