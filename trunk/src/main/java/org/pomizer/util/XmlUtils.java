package org.pomizer.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultElement;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.pomizer.constant.XmlConstants;
import org.pomizer.model.Dependency;

public class XmlUtils {

    public static String getAttributeValue(final Node node, final String attributeName) {
        
        String result = null;
        if (null != node) {
            result = (String)node.selectObject(String.format("string(./@%s)",  attributeName));  
        }
        return result;
    }
    
    public static boolean getChildNodeBooleanValue(final Node parent, final String childName, 
            final boolean defaultValue) {
        
        String value = getChildNodeTrimmedText(parent, childName);
        if (StringUtils.isNullOrEmpty(value)) {
            return defaultValue;
        }
        
        return Boolean.parseBoolean(value);
        
    }
    
    public static String getChildNodeTrimmedText(final Node parent, final String childName) {
        return getChildNodeTrimmedText(parent, childName, false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String getChildNodeTrimmedText(final Node parent, final String childName,
            final boolean usePomNamespace) {

        String result = "";

        if (null != parent) {

            try {
                HashMap map = new HashMap();
                map.put("default", XmlConstants.POM_DEFAULT_NAMESPACE);

                String prefix = "./";
                if (usePomNamespace) {
                    prefix += "default:";
                }
                XPath xpath = new Dom4jXPath(prefix + childName);
                xpath.setNamespaceContext(new SimpleNamespaceContext(map));

                Node child = (Node) xpath.selectSingleNode(parent);
                if (null != child) {
                    if (null != child.getText()) {
                        result = child.getText().trim();
                    }
                }
            }
            catch (JaxenException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static Dependency readDependencyFromXml(final Node node) {
        return readDependencyFromXml(node, false);
    }

    public static Dependency readDependencyFromXml(final Node node, final boolean usePomNamespace) {
        Dependency result = new Dependency();
        if (null != node) {
            result.groupId = getChildNodeTrimmedText(node, XmlConstants.ARTIFACT_GROUP_ID, usePomNamespace);
            result.artifactId = getChildNodeTrimmedText(node, XmlConstants.ARTIFACT_ID, usePomNamespace);
            result.version = getChildNodeTrimmedText(node, XmlConstants.ARTIFACT_VERSION, usePomNamespace);
        }
        return result;
    }
    
    public static void addDependencyToXmlParent(final DefaultElement dependenciesNode, final Dependency dependency) {
        
        if (null != dependenciesNode) {
            Element dependencyNode = dependenciesNode.addElement(XmlConstants.DEPENDENCY);
            dependencyNode.addElement(XmlConstants.ARTIFACT_GROUP_ID).addText(dependency.groupId);
            dependencyNode.addElement(XmlConstants.ARTIFACT_ID).addText(dependency.artifactId);
            dependencyNode.addElement(XmlConstants.ARTIFACT_VERSION).addText(dependency.version);
        }
    }
    
    public static Document loadXmlDocument(final String xmlFileName) throws DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(xmlFileName);
    }
    
    public static Document loadXmlDocument(final InputStream inputStream) throws DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(inputStream);
    }
    
    public static void saveXmlDocument(final Document xmlDocument, final String xmlFileName) {
        
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = null; 
        try {
            writer = new XMLWriter(new FileWriter(xmlFileName), format);
            writer.write(xmlDocument);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
