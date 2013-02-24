package org.pomizer.model;

import java.util.ArrayList;
import java.util.List;

public class DeployerProject {
    
    public String path;
    
    public DeployerSettings settings;
    
    public List<DeployerSourcesInfo> sources = new ArrayList<DeployerSourcesInfo>();

    public List<DeployerResourceInfo> resources = new ArrayList<DeployerResourceInfo>();
    
    public DeployerProject(final DeployerSettings settings) {
        this.settings = new DeployerSettings(settings);
    }
}
