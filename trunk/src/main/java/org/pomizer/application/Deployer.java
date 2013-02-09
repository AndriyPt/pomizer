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
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.pomizer.constant.XmlConstants;
import org.pomizer.exception.ApplicationException;
import org.pomizer.model.DeployerCommandInfo;
import org.pomizer.model.DeployerProject;
import org.pomizer.model.DeployerResourceInfo;
import org.pomizer.model.DeployerSettings;
import org.pomizer.model.DeployerSourcesInfo;
import org.pomizer.model.IndexInfo;
import org.pomizer.util.IndexUtils;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.StringUtils;
import org.pomizer.util.SvnUtils;
import org.pomizer.util.XmlUtils;
import org.pomizer.wrapper.DeployerChangeSet;

public class Deployer {
    
    private static String JAVA_FILE_EXTENSION = ".java";

    private static String CLASS_FILE_EXTENSION = ".java";
    
    private static String BACKUP_FILE_EXTENSION = "bck";

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
            Document configurationXmlDocument = XmlUtils.loadXmlDocument(configurationFileName);
            
            DeployerSettings globalSettings = new DeployerSettings();
            List<DeployerProject> projects = new ArrayList<DeployerProject>();
            IndexInfo index = null;
            List<String> postProcessCallUrls = new ArrayList<String>();
            List<DeployerCommandInfo> postProcessCallCommands = new ArrayList<DeployerCommandInfo>();
            
            Map<String, Map<String, String>> jarsToDeploy = 
                    new HashMap<String, Map<String, String>>();
            Map<String, List<String>> filesToDeploy = new HashMap<String, List<String>>();
            DeployerChangeSet changeset = new DeployerChangeSet(configurationFileName);
            
            loadSettingsSection("/deployer/settings", configurationXmlDocument, globalSettings);
            loadProjectsSection(projects, configurationXmlDocument, globalSettings);
            loadPostProcessCallUrls(configurationXmlDocument, postProcessCallUrls);
            loadPostProcessCommands(configurationXmlDocument, postProcessCallCommands);
            
            index = loadIndex(configurationXmlDocument, projects);
            
            JavaUtils.printToConsole("Processing changes...");
            processProjectChanges(projects, index, jarsToDeploy, filesToDeploy);
            
            final String backupFolder = FilenameUtils.concat(FilenameUtils.getBaseName(configurationFileName), 
                    BACKUP_FOLDER);
            deployJars(jarsToDeploy, changeset, backupFolder);
            deployFiles(filesToDeploy, changeset, backupFolder);
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
                JavaUtils.printToConsole("Executing command: " + commandInfo.commandLine);
                JavaUtils.executeCommand(commandInfo.commandLine);
            }
        }
    }

    private static void processUrls(List<String> postProcessCallUrls) throws IOException {
        JavaUtils.printToConsole("Calling URLs...");
        for (String url : postProcessCallUrls) {
            JavaUtils.printToConsole("Calling \"" + url + "\"...");
            JavaUtils.downloadUrl(url);
        }
    }

    private static void deployFiles(final Map<String, List<String>> filesToDeploy, final DeployerChangeSet changeset,
            final String backupFolder) throws IOException {
        
        JavaUtils.printToConsole("Deploying files...");
        for (String changedFileName : filesToDeploy.keySet()) {
            
            int position = changeset.indexOf(changedFileName);
            File backupFile;
            if (-1 == position) {
                backupFile = File.createTempFile(FilenameUtils.getName(changedFileName).replace(File.separatorChar, '_'), 
                        BACKUP_FILE_EXTENSION, new File(BACKUP_FOLDER));
                backupFile.delete();
                FileUtils.copyFile(new File(changedFileName), backupFile);
                changeset.add(changedFileName, backupFile.getAbsolutePath());
                changeset.save();
            }
            else {
                backupFile = new File(backupFolder, changeset.getBackupPath(position));
            }
            
            final File changedFile = new File(changedFileName);
            
            for (String pathToDeploy : filesToDeploy.get(changedFileName)) {
                JavaUtils.printToConsole(String.format("Deploying file \"%s\" to \"%s\"...", 
                        changedFile, pathToDeploy));
                FileUtils.copyFile(changedFile, new File(pathToDeploy));
            }
        }
    }

    private static void deployJars(final Map<String, Map<String, String>> jarsToDeploy, final DeployerChangeSet changeset,
            final String backupFolder) throws IOException {
        
        JavaUtils.printToConsole("Deploying jars...");
        for (String jar : jarsToDeploy.keySet()) {
            int position = changeset.indexOf(jar);
            File backupFile;
            if (-1 == position) {
                backupFile = File.createTempFile(FilenameUtils.getName(jar).replace(File.separatorChar, '_'), 
                        BACKUP_FILE_EXTENSION, new File(BACKUP_FOLDER));
                backupFile.delete();
                FileUtils.copyFile(new File(jar), backupFile);
                changeset.add(jar, backupFile.getAbsolutePath());
                changeset.save();
            }
            else {
                backupFile = new File(backupFolder, changeset.getBackupPath(position));
            }
            
            Map<String, Collection<File>> filesToAdd = new HashMap<String, Collection<File>>();
            for (String classPath : jarsToDeploy.get(jar).keySet()) {
                List<String> classNameWildcards = new ArrayList<String>();
                classNameWildcards.add(classPath + CLASS_FILE_EXTENSION);
                classNameWildcards.add(classPath + "$*" + CLASS_FILE_EXTENSION);
                Collection<File> foundFiles = FileUtils.listFiles(new File(jarsToDeploy.get(jar).get(classPath)), 
                        new WildcardFileFilter(classNameWildcards), null);
                filesToAdd.put(jarsToDeploy.get(jar).get(classPath), foundFiles);
            }
            
            JavaUtils.printToConsole(String.format("Deploying classes to \"%s\"...", jar));
            for (String basePath : filesToAdd.keySet()) {
                JavaUtils.printToConsole("  from path \"" + basePath + "\"");
                for (File addedFiles : filesToAdd.get(basePath)) {
                    JavaUtils.printToConsole("    adding file \"" + addedFiles.getPath() + "\"...");
                }
            }
            
            JavaUtils.addFilesToExistingZip(jar, filesToAdd);
        }
    }

    private static void processProjectChanges(final List<DeployerProject> projects, final IndexInfo index,
            final Map<String, Map<String, String>> jarsToDeploy, final Map<String, List<String>> filesToDeploy)
            throws IOException, DocumentException {
        
        for (int i = 0; i < projects.size(); i++) {
            
            DeployerProject project = projects.get(i);
            List<String> changedFiles = new ArrayList<String>();
            loadChangesetForProject(project.path, changedFiles);
            
            for (int j = 0; j < changedFiles.size(); j++) {
                processSourcesChanges(index, jarsToDeploy, project, changedFiles.get(j));
                processResourcesChanges(filesToDeploy, project, changedFiles.get(j));
            }
        }
    }

    private static void processSourcesChanges(IndexInfo index,
            Map<String, Map<String, String>> jarsToDeploy, DeployerProject project,
            final String changedFile) throws IOException {
        
        for (int i = 0; i < project.sources.size(); i++) {
            final DeployerSourcesInfo sourcesInfo = project.sources.get(i);
            String sourcesFullPath = JavaUtils.combinePaths(project.path, sourcesInfo.path);
        
            if (JavaUtils.isParentOf(sourcesFullPath, changedFile) && 
                    changedFile.endsWith(JAVA_FILE_EXTENSION)) {
                
                sourcesFullPath = JavaUtils.ensurePathHasSeparatorAtTheEnd(sourcesFullPath);
                
                String targetPath = JavaUtils.combinePaths(project.path, sourcesInfo.binariesFolder);
                targetPath = JavaUtils.ensurePathHasSeparatorAtTheEnd(targetPath);
                
                String classPath = changedFile.substring(sourcesFullPath.length(), 
                        changedFile.length() - JAVA_FILE_EXTENSION.length());
                
                List<String> deploymentJarsList = new ArrayList<String>(project.sources.get(i).deploymentPaths);
                if (project.settings.useIndex && (null != index)) {
                    String className = classPath.replace(File.separatorChar, '.');
                    IndexUtils.getJarsForClass(index, className, deploymentJarsList);
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
            String resourceFullPath = JavaUtils.combinePaths(project.path, resourceInfo.path);
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
                if (JavaUtils.isParentOf(resourceFullPath, changedFile)) {
                    for (int j = 0; j < resourceInfo.deploymentPaths.size(); j++) {
                        
                        final String relativeResourcePath = changedFile.substring(resourceFullPath.length());
                        final String resourceDeploymentPath = JavaUtils.combinePaths(resourceInfo.deploymentPaths.get(j), 
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
            final List<String> changedFiles) throws IOException, DocumentException {
        
        Document projectChangeset = SvnUtils.getChangeset(projectPath);
        List changedFilesNodes = projectChangeset.selectNodes("/status/target/entry/@path");
        if (null != changedFilesNodes) {
            for (int i = 0; i < changedFilesNodes.size(); i++) {
                Node changedFilesNode = (Node)changedFilesNodes.get(i);
                changedFiles.add(JavaUtils.combinePaths(projectPath, changedFilesNode.getText()));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void loadPostProcessCommands(final Document configurationXmlDocument,
            List<DeployerCommandInfo> postProcessCallCommands) {
        List commands = configurationXmlDocument.selectNodes("/deployer/command");
        if (null != commands) {
            for (int i = 0; i < commands.size(); i++) {
                Node command = (Node)commands.get(i);
                DeployerCommandInfo commandInfo = new DeployerCommandInfo();
                commandInfo.onUpdatedPath = XmlUtils.getAttributeValue(command, "updated_path");
                commandInfo.commandLine = XmlUtils.getAttributeValue(command, "run");
                postProcessCallCommands.add(commandInfo);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void loadPostProcessCallUrls(final Document configurationXmlDocument,
            List<String> postProcessCallUrls) {
        List callUrls = configurationXmlDocument.selectNodes("/deployer/call_url");
        if (null != callUrls) {
            for (int i = 0; i < callUrls.size(); i++) {
                Node callUrl = (Node)callUrls.get(i);
                postProcessCallUrls.add(callUrl.getText().trim());
            }
        }
    }

    private static IndexInfo loadIndex(final Document configurationXmlDocument, List<DeployerProject> projects) {
        IndexInfo index = null;
        boolean loadIndex = false;
        for (int i = 0; !loadIndex && (i < projects.size()); i++) {
            loadIndex = projects.get(i).settings.useIndex;
        }
        
        if (loadIndex) {
            final String indexFileName = XmlUtils.getChildNodeTrimmedText(configurationXmlDocument, "index");
            if (!StringUtils.isNullOrEmpty(indexFileName)) {
                index = IndexUtils.loadIndeces(indexFileName);
            }
        }
        return index;
    }

    @SuppressWarnings("rawtypes")
    private static void loadProjectsSection(final List<DeployerProject> projects,  
            final Document configurationXmlDocument, final DeployerSettings globalSettings) 
            throws ApplicationException, JaxenException {
        
        List projectsNodes = configurationXmlDocument.selectNodes("/deployer/project");
        if (null != projectsNodes) {
            for (int i = 0; i < projectsNodes.size(); i++) {
                DeployerProject project = new DeployerProject(globalSettings);
                loadSettingsSection("./settings", projectsNodes.get(i), project.settings);
                Node projectNode = (Node)projectsNodes.get(i);
                
                project.path = XmlUtils.getAttributeValue(projectNode, "path");
                JavaUtils.checkDirectoryExists(project.path);
                
                List sources = projectNode.selectNodes("./sources");
                if (null != sources) {
                    for (int j = 0; j < sources.size(); j++) {
                        DeployerSourcesInfo sourcesInfo = new DeployerSourcesInfo();
                        Node sourcesNode = (Node)sources.get(j);
                        sourcesInfo.path = XmlUtils.getAttributeValue(sourcesNode, "path");  
                        sourcesInfo.binariesFolder = XmlUtils.getAttributeValue(sourcesNode, "output");
                        loadDeploymentPaths(sourcesInfo, sourcesNode, true);
                        project.sources.add(sourcesInfo);
                    }
                }
                
                List resources = projectNode.selectNodes("./resources");
                if (null != resources) {
                    for (int j = 0; j < resources.size(); j++) {
                        DeployerResourceInfo resourceInfo = new DeployerResourceInfo();
                        Node resourceNode = (Node)resources.get(j);
                        resourceInfo.path = XmlUtils.getAttributeValue(resourceNode, "path");
                        File resourcePath = new File(JavaUtils.combinePaths(project.path, resourceInfo.path));
                        if (!resourcePath.exists()) {
                            throw new ApplicationException("Resource path \"" + resourceInfo.path + "\" doesn't exists in the project \"" 
                                    + project.path + "\" ");
                        }
                        loadDeploymentPaths(resourceInfo, resourceNode, resourcePath.isFile());
                        project.resources.add(resourceInfo);
                    }
                }
                projects.add(project);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void loadDeploymentPaths(final DeployerResourceInfo resourceInfo, 
            final Node parent, final boolean mustBeFile) throws ApplicationException {
        List deploymentPaths = parent.selectNodes("./target");
        if (null != deploymentPaths) {
            for (int k = 0; k < deploymentPaths.size(); k++) {
                Node deploymentPath = (Node)deploymentPaths.get(k);
                final String path = deploymentPath.getText();
                resourceInfo.deploymentPaths.add(path);
                if (mustBeFile) {
                    if (!JavaUtils.isFile(path)) {
                        throw new ApplicationException("Target path \"" + path + "\" for resorce should be a file");
                    }
                }
                else {
                    if (JavaUtils.isFile(path)) {
                        throw new ApplicationException("Target path \"" + path + "\" for resorce should be a directory");
                    }
                }
            }
        }
    }

    private static void loadSettingsSection(final String xPathString, final Object parent, 
            final DeployerSettings deployerSettings) throws JaxenException {
        
        XPath xpath = new Dom4jXPath(xPathString);
        Node settings = (Node)xpath.selectSingleNode(parent);
        
        if (null != settings) {
            deployerSettings.useIndex = XmlUtils.getChildNodeBooleanValue(settings, 
                    XmlConstants.USE_INDEX, deployerSettings.useIndex);
        }
    }
}
