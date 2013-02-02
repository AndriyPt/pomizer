package org.pomizer.model;

public class DeployerSettings {
    
    public boolean useIndex = true;
    
    public DeployerSettings() {
    }
    
    public DeployerSettings(final DeployerSettings settings) {
        
        if (null != settings) {
            useIndex = settings.useIndex;
        }
    }
}
