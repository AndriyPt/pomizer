package org.pomizer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.pomizer.model.IndexInfo;
import org.pomizer.render.JarIndexRenderer;

public class IndexUtils {
    
    public static IndexInfo loadIndeces(final String indexFileName) {

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

    public static void getJarsForClass(final IndexInfo indeces, final String fullClassName, 
            final List<String> deploymentJarsList) {
        
        if ((null != indeces) && (null != deploymentJarsList)) {
            
            final String className = ClassUtils.getClassNameFromFullName(fullClassName);
            final String packageName = ClassUtils.getPackageFromFullName(fullClassName);
            
            JavaUtils.printToConsole("Getting JARs for class: " + fullClassName + "...");
        
            int classIndex = Arrays.binarySearch(indeces.classNames, className);
            boolean foundClass = false;
            if (classIndex >= 0) {
            
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
                
                for (int i = lowerClassIndex; i <= upperClassIndex; i++) {
                    int packageIndex = indeces.classNamesPackageIndex[i];
                    if (indeces.packageNames[packageIndex].equals(packageName)) {
                        foundClass = true;
                        for (int j = 0; j < indeces.classNamesJarIndeces[i].length; j++) {
                            addJarToDeploymentList(indeces, deploymentJarsList, indeces.classNamesJarIndeces[i][j]);
                        }
                    }
                }
            }
                
            if (!foundClass && (indeces.packageNames.length > 0)) {
                int packageIndex = Arrays.binarySearch(indeces.packageNames, packageName);
                if (packageIndex < 0) {
                    packageIndex = -packageIndex - 1;
                    if (0 != packageIndex) {
                        if (packageIndex < indeces.packageNames.length) {
                            int previousPackageDifferentCharIndex = StringUtils.getIndexOfDifferentChar(packageName, 
                                    indeces.packageNames[packageIndex - 1]);
                            int nextPackageDifferentCharIndex = StringUtils.getIndexOfDifferentChar(packageName, 
                                    indeces.packageNames[packageIndex]);
                            if (nextPackageDifferentCharIndex < previousPackageDifferentCharIndex) {
                                packageIndex--;
                            }
                        }
                        else {
                            packageIndex--;
                        }
                    }
                }
                for (int i = 0; i < indeces.packageNamesJarIndeces[packageIndex].length; i++) {
                    addJarToDeploymentList(indeces, deploymentJarsList, indeces.packageNamesJarIndeces[packageIndex][i]);
                }
            }
        }
    }

    private static void addJarToDeploymentList(final IndexInfo indeces, final List<String> deploymentJarsList,
            final int jarIndex) {
        int basePathIndex = indeces.jarNamesBasePathIndex[jarIndex];
        String jarPath = FilenameUtils.concat(indeces.basePaths[basePathIndex], 
                indeces.jarNames[jarIndex]);
        if (-1 == deploymentJarsList.indexOf(jarPath)) {
            deploymentJarsList.add(jarPath);
        }
    }
}
