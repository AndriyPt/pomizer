package org.pomizer.wrapper;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.pomizer.util.XmlUtils;

public class DeployerChangeSet {
    
    private final static String CHANGESET_EXTENSION = ".changeset";
    
    private final static String CHANGESET_NODE = "changeset";
    
    private final static String FILE_NODE = "file";
    
    private final static String PATH_ATTRIBUTE = "path";
    
    private final static String BACKUP_ATTRIBUTE = "backup";
    
    private String fileName;
    
    private Document document;
    
    public DeployerChangeSet(final String projectFileName) {
        fileName = projectFileName + CHANGESET_EXTENSION;
    }
    
    private void validateIndex(final int index) {
        if ((index < 0) || (index >= getSize())) {
            throw new IndexOutOfBoundsException("index parameter is out of bounds");
        }
    }
    
    public void load() throws DocumentException {
        
        final File file = new File(fileName);
        if (file.exists()) {
            document = XmlUtils.loadXmlDocument(fileName);
        }
        document = DocumentHelper.createDocument();
        document.addElement(CHANGESET_NODE);        
    }
    
    public int getSize() {
        return document.getRootElement().elements().size();
    }
    
    public String getAttributeValue(final int index, final String attributeName) {
        validateIndex(index);
        
        Node node = (Node)document.getRootElement().elements().get(index);
        return XmlUtils.getAttributeValue(node, attributeName);
    }

    
    public String getPath(final int index) {
        return getAttributeValue(index, PATH_ATTRIBUTE);
    }
    
    public String getBackupPath(final int index) {
        return getAttributeValue(index, BACKUP_ATTRIBUTE);
    }
    
    public void delete(final int index) {
        validateIndex(index);
        
        Node node = (Node)document.getRootElement().elements().get(index);
        node.detach();
    }
    
    public void add(final String path, final String backupPath) {
        
        Element root = document.getRootElement();
        root.addElement(FILE_NODE)
            .addAttribute(PATH_ATTRIBUTE, path)
            .addAttribute(BACKUP_ATTRIBUTE, backupPath);
    }
    
    public int indexOf(final String path) {
        int result = -1;
        
        for (int i = 0; (i < this.getSize()) && (-1 != result); i++) {
            if (this.getPath(i).equals(path)) {
                result = i;
                break;
            }
        }
        
        return result;
    }
    
    public void save() {
        XmlUtils.saveXmlDocument(document, fileName);
    }
}
