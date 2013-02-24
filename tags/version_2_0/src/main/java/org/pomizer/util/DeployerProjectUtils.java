package org.pomizer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
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

public class DeployerProjectUtils {
    
    @SuppressWarnings("rawtypes")
    public static void loadCopySection(final Document configurationXmlDocument, 
            final Map<String, List<String>> filesToDeploy) {
        
        final List copyNodes = configurationXmlDocument.selectNodes("/deployer/copy");
        if (null != copyNodes) {
            for (int i = 0; i < copyNodes.size(); i++) {
                Node copyNode = (Node)copyNodes.get(i);
                String path = XmlUtils.getAttributeValue(copyNode, "path");
                if (!filesToDeploy.containsKey(path)) {
                    filesToDeploy.put(path, new ArrayList<String>());
                }
                final List targetPaths = copyNode.selectNodes("./target");
                if (null != targetPaths) {
                    for (int j = 0; j < targetPaths.size(); j++) {
                        Node targetPath = (Node)targetPaths.get(j);
                        filesToDeploy.get(path).add(targetPath.getText());
                    }
                }
            }
        }
    }
    
    public static void loadSettingsSection(final String xPathString, final Object parent, 
            final DeployerSettings deployerSettings) throws JaxenException {
        
        XPath xpath = new Dom4jXPath(xPathString);
        Node settings = (Node)xpath.selectSingleNode(parent);
        
        if (null != settings) {
            deployerSettings.useIndex = XmlUtils.getChildNodeBooleanValue(settings, 
                    XmlConstants.USE_INDEX, deployerSettings.useIndex);
        }
    }
    
    @SuppressWarnings("rawtypes")
    public static void loadProjectsSection(final List<DeployerProject> projects,  
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
                        File resourcePath = new File(FilenameUtils.concat(project.path, resourceInfo.path));
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
    
    @SuppressWarnings("rawtypes")
    public static void loadPostProcessCommands(final Document configurationXmlDocument,
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
    public static void loadPostProcessCallUrls(final Document configurationXmlDocument,
            List<String> postProcessCallUrls) {
        List callUrls = configurationXmlDocument.selectNodes("/deployer/call_url");
        if (null != callUrls) {
            for (int i = 0; i < callUrls.size(); i++) {
                Node callUrl = (Node)callUrls.get(i);
                postProcessCallUrls.add(callUrl.getText().trim());
            }
        }
    }
}
