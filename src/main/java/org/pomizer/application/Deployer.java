package org.pomizer.application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.pomizer.exception.ApplicationException;
import org.pomizer.model.DeployerCommandInfo;
import org.pomizer.model.DeployerProject;
import org.pomizer.model.DeployerResourceInfo;
import org.pomizer.model.DeployerSettings;
import org.pomizer.model.DeployerSourcesInfo;
import org.pomizer.model.IndexInfo;
import org.pomizer.util.DeployerProjectUtils;
import org.pomizer.util.IndexUtils;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.StringUtils;
import org.pomizer.util.SvnUtils;
import org.pomizer.util.XmlUtils;
import org.pomizer.wrapper.DeployerChangeSet;

public class Deployer {
    
    private static String JAVA_FILE_EXTENSION = ".java";

    private static String CLASS_FILE_EXTENSION = ".class";
    
    private static String BACKUP_FILE_EXTENSION = ".bck";

    private static String BACKUP_FOLDER = "backup";
    
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Application usage: <program> <path to configuration file>\n");
            System.exit(1);
        }

        String configurationFileName = args[0];
        
        JavaUtils.checkFileExists(configurationFileName);

        deployFiles(configurationFileName);
    }

    private static void deployFiles(final String configurationFileName) {
        
        try {
            JavaUtils.printToConsole("Loading configuration file...");
            
            Document configurationXmlDocument = XmlUtils.loadXmlDocumentWithValidation(configurationFileName, 
                    "deployer.rng");
            
            DeployerSettings globalSettings = new DeployerSettings();
            List<DeployerProject> projects = new ArrayList<DeployerProject>();
            IndexInfo indeces = null;
            List<String> postProcessCallUrls = new ArrayList<String>();
            List<DeployerCommandInfo> postProcessCallCommands = new ArrayList<DeployerCommandInfo>();
            
            Map<String, Map<String, String>> jarsToDeploy = 
                    new HashMap<String, Map<String, String>>();
            Map<String, List<String>> filesToDeploy = new HashMap<String, List<String>>();
            DeployerChangeSet changeset = new DeployerChangeSet(configurationFileName);
            changeset.load();
            
            DeployerProjectUtils.loadSettingsSection("/deployer/settings", configurationXmlDocument, globalSettings);
            DeployerProjectUtils.loadProjectsSection(projects, configurationXmlDocument, globalSettings);
            DeployerProjectUtils.loadCopySection(configurationXmlDocument, filesToDeploy);
            DeployerProjectUtils.loadPostProcessCallUrls(configurationXmlDocument, postProcessCallUrls);
            DeployerProjectUtils.loadPostProcessCommands(configurationXmlDocument, postProcessCallCommands);
            
            indeces = loadIndex(configurationXmlDocument, projects);
            
            JavaUtils.printToConsole("Processing changes...");
            processProjectChanges(projects, indeces, jarsToDeploy, filesToDeploy);
            
            final String backupFolder = FilenameUtils.concat(FilenameUtils.getFullPath(configurationFileName),
                    BACKUP_FOLDER);
            final File backupFolderFile = new File(backupFolder); 
            FileUtils.forceMkdir(backupFolderFile);
            
            deployJars(jarsToDeploy, changeset, backupFolderFile);
            deployFiles(filesToDeploy, changeset, backupFolderFile);
            processUrls(postProcessCallUrls);
            processCommands(postProcessCallCommands, jarsToDeploy, filesToDeploy);
            
            JavaUtils.printToConsole("Finished");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processCommands(List<DeployerCommandInfo> postProcessCallCommands,
            Map<String, Map<String, String>> jarsToDeploy, Map<String, List<String>> filesToDeploy) throws IOException {
        JavaUtils.printToConsole("Executing commands...");
        for (DeployerCommandInfo commandInfo : postProcessCallCommands) {
            
            boolean foundUpdatePath = false;
            final String cannonicalUpdatePath = new File(commandInfo.onUpdatedPath).getCanonicalPath();
            for (String jar : jarsToDeploy.keySet()) {
                if (FilenameUtils.directoryContains(cannonicalUpdatePath, new File(jar).getCanonicalPath())) {
                    foundUpdatePath = true;
                    break;
                }
            }
            
            if (!foundUpdatePath) {
                for (String fileName : filesToDeploy.keySet()) {
                    for (int i = 0; i < filesToDeploy.get(fileName).size(); i++) {
                        String deploymentPath = filesToDeploy.get(fileName).get(i);
                        if (FilenameUtils.directoryContains(cannonicalUpdatePath, 
                                new File(deploymentPath).getCanonicalPath())) {
                            foundUpdatePath = true;
                            break;
                        }
                    }
                }
                if (foundUpdatePath) {
                    break;
                }
            }
            
            if (foundUpdatePath) {
                JavaUtils.printToConsole("  command: " + commandInfo.commandLine);
                JavaUtils.executeCommand(commandInfo.commandLine);
            }
        }
    }

    private static void processUrls(List<String> postProcessCallUrls) throws IOException {
        JavaUtils.printToConsole("Calling URLs...");
        for (String url : postProcessCallUrls) {
            JavaUtils.printToConsole("  " + url + " ...");
            JavaUtils.downloadUrl(url);
        }
    }

    private static void deployFiles(final Map<String, List<String>> filesToDeploy, final DeployerChangeSet changeset,
            final File backupFolderFile) throws IOException {
        
        JavaUtils.printToConsole("Deploying files...");
        for (String changedFileName : filesToDeploy.keySet()) {
            
            final File changedFile = new File(changedFileName);
            
            if (changedFile.exists()) {
                for (String pathToDeploy : filesToDeploy.get(changedFileName)) {
                    JavaUtils.printToConsole(String.format("  file \"%s\" to \"%s\"...", 
                            changedFileName, pathToDeploy));
                    final File pathToDeployFile = new File(pathToDeploy);
                    if (pathToDeployFile.exists()) { 
                        getBackupFile(changeset, backupFolderFile, pathToDeploy);
                    }
                    else {
                        int position = changeset.indexOf(pathToDeploy);
                        if (-1 == position) {
                            changeset.add(pathToDeploy, "");
                            changeset.save();
                        }
                    }
                    FileUtils.copyFile(changedFile, pathToDeployFile);
                }
            }
            else {
                JavaUtils.printToConsole(String.format("  file \"%s\" doesn\'t exist...", 
                        changedFileName));
            }
        }
    }

    private static void deployJars(final Map<String, Map<String, String>> jarsToDeploy, 
            final DeployerChangeSet changeset, final File backupFolderFile) throws IOException {
        
        JavaUtils.printToConsole("Deploying jars...");
        for (String jar : jarsToDeploy.keySet()) {
            File backupFile = getBackupFile(changeset, backupFolderFile, jar);
            
            Map<String, List<String>> filesToAdd = new HashMap<String, List<String>>();
            for (String classPath : jarsToDeploy.get(jar).keySet()) {
                List<String> classNameWildcards = new ArrayList<String>();
                classNameWildcards.add(FilenameUtils.getBaseName(classPath) + CLASS_FILE_EXTENSION);
                classNameWildcards.add(FilenameUtils.getBaseName(classPath) + "$*" + CLASS_FILE_EXTENSION);
                String targetDirectory = JavaUtils.ensurePathHasSeparatorAtTheEnd(
                        jarsToDeploy.get(jar).get(classPath));
                String fullClassFileDirectory = FilenameUtils.concat(targetDirectory, 
                        FilenameUtils.getFullPath(classPath)); 
                Collection<File> foundFiles = FileUtils.listFiles(new File(fullClassFileDirectory), 
                        new WildcardFileFilter(classNameWildcards), null);
                if (!filesToAdd.containsKey(targetDirectory)) {
                    filesToAdd.put(targetDirectory, new ArrayList<String>());
                }
                for (File file : foundFiles) {
                    filesToAdd.get(targetDirectory).add(file.getAbsolutePath().substring(
                            targetDirectory.length()));
                }
            }
            
            JavaUtils.printToConsole(String.format("Deploying classes to \"%s\"...", jar));
            for (String basePath : filesToAdd.keySet()) {
                JavaUtils.printToConsole("  from path \"" + basePath + "\"");
                for (String addedFile : filesToAdd.get(basePath)) {
                    JavaUtils.printToConsole("    adding file \"" + addedFile + "\"...");
                }
            }
            
            final File tempJarFile = File.createTempFile("jar_temp_file_", null);
            tempJarFile.delete();
            FileUtils.copyFile(backupFile, tempJarFile);
            JavaUtils.addFilesToExistingZip(tempJarFile, filesToAdd);
            FileUtils.copyFile(tempJarFile, new File(jar));
            tempJarFile.delete();
        }
    }

    private static File getBackupFile(final DeployerChangeSet changeset, final File backupFolderFile, 
            final String fileName) throws IOException {
        int position = changeset.indexOf(fileName);
        File backupFile;
        if (-1 == position) {
            backupFile = File.createTempFile(FilenameUtils.getName(fileName).replace('.', '_')  + '_', 
                    BACKUP_FILE_EXTENSION, backupFolderFile);
            backupFile.delete();
            FileUtils.copyFile(new File(fileName), backupFile);
            changeset.add(fileName, backupFile.getAbsolutePath());
            changeset.save();
        }
        else {
            backupFile = new File(changeset.getBackupPath(position));
        }
        return backupFile;
    }

    private static void processProjectChanges(final List<DeployerProject> projects, final IndexInfo indeces,
            final Map<String, Map<String, String>> jarsToDeploy, final Map<String, List<String>> filesToDeploy)
            throws IOException, DocumentException, ApplicationException {
        
        for (int i = 0; i < projects.size(); i++) {
            
            DeployerProject project = projects.get(i);
            List<String> changedFiles = new ArrayList<String>();
            loadChangesetForProject(project.path, changedFiles);
            
            for (int j = 0; j < changedFiles.size(); j++) {
                processSourcesChanges(indeces, jarsToDeploy, project, changedFiles.get(j));
                processResourcesChanges(filesToDeploy, project, changedFiles.get(j));
            }
        }
    }

    private static void processSourcesChanges(IndexInfo indeces,
            Map<String, Map<String, String>> jarsToDeploy, DeployerProject project,
            final String changedFileName) throws IOException {
        
        final File changedFile = new File(changedFileName);
        for (int i = 0; i < project.sources.size(); i++) {
            final DeployerSourcesInfo sourcesInfo = project.sources.get(i);
            String sourcesFullPath = FilenameUtils.concat(project.path, sourcesInfo.path);
            final File sourceFullDirectory = new File(sourcesFullPath);
        
            if (FileUtils.directoryContains(sourceFullDirectory, changedFile) && 
                    changedFileName.endsWith(JAVA_FILE_EXTENSION)) {
                
                sourcesFullPath = JavaUtils.ensurePathHasSeparatorAtTheEnd(sourcesFullPath);
                
                String targetPath = FilenameUtils.concat(project.path, sourcesInfo.binariesFolder);
                targetPath = JavaUtils.ensurePathHasSeparatorAtTheEnd(targetPath);
                
                String classPath = changedFileName.substring(sourcesFullPath.length(), 
                        changedFileName.length() - JAVA_FILE_EXTENSION.length());
                
                List<String> deploymentJarsList = new ArrayList<String>(project.sources.get(i).deploymentPaths);
                if (project.settings.useIndex && (null != indeces)) {
                    String className = classPath.replace(File.separatorChar, '.');
                    IndexUtils.getJarsForClass(indeces, className, deploymentJarsList);
                }
                
                for (int j = 0; j < deploymentJarsList.size(); j++) {
                    
                    final String jarPath = deploymentJarsList.get(j);
                    if (!jarsToDeploy.containsKey(jarPath)) {
                        jarsToDeploy.put(jarPath, new HashMap<String, String>());
                    }
                    jarsToDeploy.get(jarPath).put(classPath, targetPath);
                    //TODO: Add here warning that adding two classPaths to the same JAR
                }
            }
        }
    }
    
    private static void processResourcesChanges(final Map<String, List<String>> filesToDeploy, final DeployerProject project,
            final String changedFile) throws IOException {
        
        for (int i = 0; i < project.resources.size(); i++) {
            final DeployerResourceInfo resourceInfo = project.resources.get(i);
            String resourceFullPath = FilenameUtils.concat(project.path, resourceInfo.path);
            if (JavaUtils.isFile(resourceFullPath)) {
                if (JavaUtils.isTheSamePath(resourceFullPath, changedFile)) {
                    for (int j = 0; j < resourceInfo.deploymentPaths.size(); j++) {
                        
                        if (!filesToDeploy.containsKey(changedFile)) {
                            filesToDeploy.put(changedFile, new ArrayList<String>());
                        }
                        filesToDeploy.get(changedFile).add(resourceInfo.deploymentPaths.get(j));
                    }
                }
            }
            else {
                resourceFullPath = JavaUtils.ensurePathHasSeparatorAtTheEnd(resourceFullPath);
                if (FilenameUtils.directoryContains(resourceFullPath, changedFile)) {
                    for (int j = 0; j < resourceInfo.deploymentPaths.size(); j++) {
                        
                        final String relativeResourcePath = changedFile.substring(resourceFullPath.length());
                        final String resourceDeploymentPath = FilenameUtils.concat(resourceInfo.deploymentPaths.get(j), 
                                relativeResourcePath);
                        
                        if (!filesToDeploy.containsKey(changedFile)) {
                            filesToDeploy.put(changedFile, new ArrayList<String>());
                        }
                        filesToDeploy.get(changedFile).add(resourceDeploymentPath);
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void loadChangesetForProject(final String projectPath, 
            final List<String> changedFiles) throws IOException, DocumentException, ApplicationException {
        
        Document projectChangeset = SvnUtils.getChangeset(projectPath);
        List changedFilesNodes = projectChangeset.selectNodes("/status/target/entry/@path");
        if (null != changedFilesNodes) {
            for (int i = 0; i < changedFilesNodes.size(); i++) {
                Node changedFilesNode = (Node)changedFilesNodes.get(i);
                final String changedFileName = FilenameUtils.concat(projectPath, changedFilesNode.getText());
                final File changedFile = new File(changedFileName);
                
                if (changedFile.exists()) {
                    if (changedFile.isDirectory()) {
                        
                       final Collection<File> files = FileUtils.listFiles(changedFile, TrueFileFilter.INSTANCE, 
                               TrueFileFilter.INSTANCE);
                       for (File file : files) {
                           if (file.isFile()) {
                               changedFiles.add(file.getAbsolutePath());
                           }
                       }
                        
                    }
                    else {
                        changedFiles.add(changedFileName);
                    }
                }
            }
        }
    }

    private static IndexInfo loadIndex(final Document configurationXmlDocument, List<DeployerProject> projects) {
        IndexInfo indeces = null;
        boolean loadIndex = false;
        for (int i = 0; !loadIndex && (i < projects.size()); i++) {
            loadIndex = projects.get(i).settings.useIndex;
        }
        
        if (loadIndex) {
            final String indexFileName = XmlUtils.getChildNodeTrimmedText(
                    configurationXmlDocument.getRootElement(), "index");
            if (!StringUtils.isNullOrEmpty(indexFileName)) {
                indeces = IndexUtils.loadIndeces(indexFileName);
            }
        }
        return indeces;
    }

}
