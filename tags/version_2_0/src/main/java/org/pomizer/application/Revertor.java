package org.pomizer.application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.pomizer.model.DeployerCommandInfo;
import org.pomizer.util.DeployerProjectUtils;
import org.pomizer.util.JavaUtils;
import org.pomizer.util.StringUtils;
import org.pomizer.util.XmlUtils;
import org.pomizer.wrapper.DeployerChangeSet;

public class Revertor {
    
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Application usage: <program> <path to configuration file>\n");
            System.exit(1);
        }

        String configurationFileName = args[0];
        
        JavaUtils.checkFileExists(configurationFileName);

        revertFiles(configurationFileName);
    }

    private static void revertFiles(final String configurationFileName) {
        
        try {
            JavaUtils.printToConsole("Loading configuration file...");
            
            DeployerChangeSet changeset = new DeployerChangeSet(configurationFileName);
            changeset.load();
            
            if (0 == changeset.getSize()) {
                JavaUtils.printToConsole("No changes found");
            }
            else {
            
                final Document configurationXmlDocument = XmlUtils.loadXmlDocument(configurationFileName);
                
                List<String> postProcessCallUrls = new ArrayList<String>();
                List<DeployerCommandInfo> postProcessCallCommands = new ArrayList<DeployerCommandInfo>();
                
                DeployerProjectUtils.loadPostProcessCallUrls(configurationXmlDocument, postProcessCallUrls);
                DeployerProjectUtils.loadPostProcessCommands(configurationXmlDocument, postProcessCallCommands);
                
                JavaUtils.printToConsole("Reverting changes...");
                final List<String> changedFiles = new ArrayList<String>();
                final List<File> filesToDelete = new ArrayList<File>();
                
                for (int i = 0; i < changeset.getSize(); i++) {
                    final String changedFileName = changeset.getPath(i); 
                    if (-1 == changedFiles.indexOf(changedFileName)) {
                        changedFiles.add(changedFileName);
                    }
                    
                    final String backupFileName = changeset.getBackupPath(i);
                    
                    JavaUtils.printToConsole("  reverting \"" + changedFileName + "\"...");
                    final File changedFile = new File(changedFileName);
                    if (StringUtils.isNullOrEmptyOrBlank(backupFileName)) {
                        filesToDelete.add(changedFile);
                    }
                    else {
                        final File backupFile = new File(backupFileName);
                        filesToDelete.add(backupFile);
                        FileUtils.copyFile(backupFile, changedFile);
                    }
                }
                changeset.delete();
                
                for (File file : filesToDelete) {
                    if (file.exists()) {
                        file.delete();
                    }
                }
                
                processUrls(postProcessCallUrls);
                processCommands(postProcessCallCommands, changedFiles);
            }
            
            JavaUtils.printToConsole("Finished");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processCommands(final List<DeployerCommandInfo> postProcessCallCommands, 
            final List<String> changedFiles) throws IOException {
        
        JavaUtils.printToConsole("Executing commands...");
        for (DeployerCommandInfo commandInfo : postProcessCallCommands) {
            
            boolean foundUpdatePath = false;
            final String cannonicalUpdatePath = new File(commandInfo.onUpdatedPath).getCanonicalPath();
            for (String changedFile : changedFiles) {
                if (FilenameUtils.directoryContains(cannonicalUpdatePath, new File(changedFile).getCanonicalPath())) {
                    foundUpdatePath = true;
                    break;
                }
            }
            
            if (foundUpdatePath) {
                JavaUtils.printToConsole("  command: " + commandInfo.commandLine);
                JavaUtils.executeCommand(commandInfo.commandLine);
            }
        }
    }

    private static void processUrls(final List<String> postProcessCallUrls) throws IOException {
        JavaUtils.printToConsole("Calling URLs...");
        for (String url : postProcessCallUrls) {
            JavaUtils.printToConsole("  " + url + " ...");
            JavaUtils.downloadUrl(url);
        }
    }
}
