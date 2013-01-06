package org.pomizer.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.pomizer.constant.XmlConstants;
import org.pomizer.model.Dependency;


public class XmlUtils {
	
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

	public static Document downloadMavenXml(final String urlToGo) throws DocumentException {

		Document document = null;
		try {
			URL url = new URL(urlToGo);
			SAXReader reader = new SAXReader();
			document = reader.read(url);
		} 
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return document;
	}
	
	public static Dependency readDependencyFromXml(final Node node) {
		return readDependencyFromXml(node, false);
	}
	
	public static Dependency readDependencyFromXml(final Node node, final boolean usePomNamespace) {
		Dependency result = new Dependency();
		if (null != node) {
			result.groupId = getChildNodeTrimmedText(
					node, XmlConstants.ARTIFACT_GROUP_ID, usePomNamespace); 
			result.artifactId = getChildNodeTrimmedText(
					node, XmlConstants.ARTIFACT_ID, usePomNamespace); 
			result.version = getChildNodeTrimmedText(
					node, XmlConstants.ARTIFACT_VERSION, usePomNamespace);
		}
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public static List<Dependency> searchForAllDependecyArtifacts(final Document document) {

		List<Dependency> result = new ArrayList<Dependency>();
		if (null != document) {
			List nodes = document.selectNodes("/searchNGResponse/data/artifact");
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i) instanceof Node) {
					Dependency dependency = readDependencyFromXml((Node)nodes.get(i));
					boolean found = false;
					for (int j = 0; (j < result.size()) && !found; j++) {
						Dependency currentDependency = result.get(j); 
						if (StringUtils.areEqual(currentDependency.groupId, dependency.groupId) &&
								StringUtils.areEqual(currentDependency.artifactId, dependency.artifactId)) {
							found = true;
						}
					}
					
					if (!found) {
						result.add(dependency);
					}
				}
			}
		}
		return result;
	}
}
