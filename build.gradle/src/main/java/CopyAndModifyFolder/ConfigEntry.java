package main.java.CopyAndModifyFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ConfigEntry {
    private String source;
    private boolean useCustomOutputDir;
    private String customOutputDir;
    private List<String> configList;
    private List<String> folderList;

    public ConfigEntry(Map<String, Object> configMap) {
        this.source = (String) configMap.get("source");
        this.useCustomOutputDir = (boolean) configMap.getOrDefault("useCustomOutputDir", false);
        this.customOutputDir = (String) configMap.get("customOutputDir");
        this.configList = (List<String>) configMap.getOrDefault("configList", new ArrayList<>());
        this.folderList = (List<String>) configMap.getOrDefault("folderList", new ArrayList<>());
    }

    public String getSource() {
        return source;
    }

    public boolean isUseCustomOutputDir() {
        return useCustomOutputDir;
    }

    public String getCustomOutputDir() {
        return customOutputDir;
    }

    public List<String> getConfigList() {
        return configList;
    }

    public List<String> getFolderList() {
        return folderList;
    }
}