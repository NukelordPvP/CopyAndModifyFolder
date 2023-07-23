package main.java.CopyAndModifyFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.*;
import org.yaml.snakeyaml.Yaml;

public class CopyAndModifyFolder {
    private static final Logger logger = Logger.getLogger(CopyAndModifyFolder.class.getName());

    public static void main(String[] args) {
        try {
            setupLogging();

            // Read the configuration from the YAML file
            String configFilePath = "./config-server-client-side-files.yaml";
            List<ConfigEntry> configEntries = readConfigEntries(configFilePath);

            for (ConfigEntry configEntry : configEntries) {
                // Process each config entry and perform the copy and modifications
                String sourceFolderPath = configEntry.getSource();
                String destinationFolderPath = getDestinationFolder(configEntry);
                List<String> configList = configEntry.getConfigList();
                List<String> folderList = configEntry.getFolderList();

                // Copy the source folder to the destination folder
                copyFolder(sourceFolderPath, destinationFolderPath);

                // Remove the client-side configs from the copied folder
                removeConfigs(destinationFolderPath, configList);

                // Delete folders within the copied folder
                deleteFolders(destinationFolderPath, folderList);

                // Check for files/folders that should be deleted but were not specified in the config file
                checkForDeletions(sourceFolderPath, destinationFolderPath);
            }

            logger.info("Folders copied, client-side configs removed, and folders deleted successfully.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "An error occurred during execution.", e);
        }
    }

    private static void setupLogging() throws IOException {
        // Create a file handler to log to the CopyAndModifyFolder.log file
        Handler fileHandler = new FileHandler("CopyAndModifyFolder.log");

        // Create a simple formatter to format log records
        java.util.logging.SimpleFormatter simpleFormatter = new java.util.logging.SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);

        // Add the file handler to the logger
        logger.addHandler(fileHandler);
    }

    private static List<ConfigEntry> readConfigEntries(String configFilePath) throws IOException {
        List<ConfigEntry> configEntries = new ArrayList<>();
        Yaml yaml = new Yaml();

        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            Iterable<Object> objects = yaml.loadAll(fis);
            for (Object obj : objects) {
                if (obj instanceof Map) {
                    configEntries.add(new ConfigEntry((Map<String, Object>) obj));
                }
            }
        }

        return configEntries;
    }

    private static String getDestinationFolder(ConfigEntry configEntry) {
        String destinationFolderPath;
        if (configEntry.isUseCustomOutputDir()) {
            destinationFolderPath = configEntry.getCustomOutputDir();
        } else {
            destinationFolderPath = "./config-Server";
        }
        return destinationFolderPath;
    }

    private static void copyFolder(String sourceFolderPath, String destinationFolderPath) throws IOException {
        // The copyFolder method remains the same
    }

    private static void removeConfigs(String destinationFolderPath, List<String> configList) throws IOException {
        // The removeConfigs method remains the same
    }

    private static void deleteFolders(String destinationFolderPath, List<String> folderList) throws IOException {
        // The deleteFolders method remains the same
    }

    private static void checkForDeletions(String sourceFolderPath, String destinationFolderPath) throws IOException {
        // The checkForDeletions method remains the same
    }
}