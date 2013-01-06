package org.pomizer.model;

import org.pomizer.util.StringUtils;

public class Dependency {
	
	public String groupId;
	
	public String artifactId;
	
	public String version;
	
	@Override
	public boolean equals(final Object object) {
		
        if (null == object) {
            return false;
        }
        
        if (object == this) {
            return true;
        }
        
        if (object.getClass() != getClass()) {
            return false;
        }
        
        Dependency dependency = (Dependency)object;
        
        return StringUtils.areEqual(dependency.groupId, this.groupId) &&
        		StringUtils.areEqual(dependency.artifactId, this.artifactId) &&
        		StringUtils.areEqual(dependency.version, this.version);
    }
	
	@Override 
	public int hashCode() {
		return StringUtils.hashCode(this.groupId) ^ 
				StringUtils.hashCode(this.artifactId) ^ 
				StringUtils.hashCode(this.version);  
	}
}
