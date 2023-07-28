package com.nukelord.CopyAndModifyFolder;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

public class CopyAndModifyFolder {
    private static final String LOG_EXTENSION = ".log";
    private static final String CONFIG_EXTENSION = ".txt";

    public static void main(String[] args) {
        // Determine the configuration file name based on the program execution type
        String configFileName;
        String logFileName;

        if (isRunningAsCompiledJar()) {
            String jarFileName = getJarFileName();
            configFileName = jarFileName.replace(".jar", CONFIG_EXTENSION);
            logFileName = jarFileName.replace(".jar", LOG_EXTENSION);
        } else {
            configFileName = CopyAndModifyFolder.class.getSimpleName() + CONFIG_EXTENSION;
            logFileName = CopyAndModifyFolder.class.getSimpleName() + LOG_EXTENSION;
        }

        // Use the current working directory to locate the configuration file
        String configFilePath = getConfigFilePath(configFileName);

        // Read configuration from the config file
        ConfigEntry mainConfigEntry = readConfigFile(configFilePath);

        if (mainConfigEntry != null) {
            // Get the name of the JAR file or class file
            String jarFileName = new File(CopyAndModifyFolder.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath())
                    .getName();

            // Generate the log file name
            //String logFileName = jarFileName.replace(".jar", LOG_EXTENSION);

            // Get the main directory level (root folder from where the program should run)
            int mainDirectoryLevel = mainConfigEntry.getMainDirectoryLevel();

            // Set the source and destination folders based on the main directory level
            File current_directory = new File(".");

            for (int i = 0; i < mainDirectoryLevel; i++) {
                current_directory = current_directory.getParentFile();
            }

            // Create log file using the determined log file name
            try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFileName))) {
                List<SourceFolderConfig> sourceFolders = mainConfigEntry.getSourceFolders();

                // Log main configuration variables
                logWriter.println("MAIN_DIRECTORY_LEVEL:" + mainDirectoryLevel);
                logWriter.println("RUN_FROM_MODS:" + mainConfigEntry.isRunFromMods());
                logWriter.println("GLOBAL_OVERRIDE_CUSTOM_OUTPUT_DIR:" + (mainConfigEntry.getGlobalOverrideCustomOutputDir() != null ?
                        "\"" + mainConfigEntry.getGlobalOverrideCustomOutputDir() + "\"" : "Not set"));
                logWriter.println("USE_CUSTOM_OUTPUT_DIR:" + mainConfigEntry.isUseCustomOutputDir());
                logWriter.println();

                for (SourceFolderConfig folderConfig : sourceFolders) {
                    String inputFolder = folderConfig.getSource();
                    String outputFolder = (mainConfigEntry.isUseCustomOutputDir() ? mainConfigEntry.getGlobalOverrideCustomOutputDir() : "") + folderConfig.getCustomOutputDir();

                    if (inputFolder == null || outputFolder == null) {
                        logWriter.println("Skipping entry: Incomplete configuration. Both source and destination folders must be specified.");
                        logWriter.println("Source folder: " + (inputFolder != null ? inputFolder : "Not set"));
                        logWriter.println("Destination folder: " + (outputFolder != null ? outputFolder : "Not set"));
                        continue;
                    }

                    File sourceEntryFolder = new File(current_directory, inputFolder);
                    File destinationEntryFolder = new File(current_directory, outputFolder);

                    // Log specific entry configurations
                    logWriter.println("Processing entry:");
                    logWriter.println("Source folder: " + sourceEntryFolder.getAbsolutePath());
                    logWriter.println("Destination folder: " + destinationEntryFolder.getAbsolutePath());
                    logWriter.println("CONFIG_LIST: " + mainConfigEntry.getConfigList());
                    logWriter.println("FOLDER_LIST: " + mainConfigEntry.getFolderList());

                    // Copy the source folder to the destination folder
                    copyFolder(sourceEntryFolder, destinationEntryFolder, logWriter, inputFolder, outputFolder);

                    // Remove config files from the copied folder
                    removeConfigFiles(mainConfigEntry.getConfigList(), destinationEntryFolder, logWriter);

                    // Delete specified folders from the copied folder
                    deleteFolders(mainConfigEntry.getFolderList(), destinationEntryFolder, logWriter);

                    // Check for deletions in the destination folder
                    checkForDeletions(sourceEntryFolder, destinationEntryFolder, logWriter);

                    logWriter.println("Processing complete for this entry.");
                }

                for (SourceFilesConfig fileConfig : mainConfigEntry.getSourceFiles()) {
                    String inputFolder = fileConfig.getSource();
                    String outputFolder = (mainConfigEntry.isUseCustomOutputDir() ? mainConfigEntry.getGlobalOverrideCustomOutputDir() : "") + fileConfig.getCustomOutputDir();

                    if (inputFolder == null || outputFolder == null) {
                        logWriter.println("Skipping entry: Incomplete configuration. Both source and destination files must be specified.");
                        logWriter.println("Source file: " + (inputFolder != null ? inputFolder : "Not set"));
                        logWriter.println("Destination file: " + (outputFolder != null ? outputFolder : "Not set"));
                        continue;
                    }

                    File sourceEntryFile = new File(current_directory, inputFolder);
                    File destinationEntryFile = new File(current_directory, outputFolder);

                    // Log specific entry configurations
                    logWriter.println("Processing entry:");
                    logWriter.println("Source file: " + sourceEntryFile.getAbsolutePath());
                    logWriter.println("Destination file: " + destinationEntryFile.getAbsolutePath());

                    // Copy the source file to the destination file
                    copyFile(sourceEntryFile, destinationEntryFile, logWriter, inputFolder, outputFolder);
                    logWriter.println("Processing complete for this entry.");
                }

                logWriter.println("\nTask completed successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Config entry not found or config file is not valid.");
        }
    }

    private static boolean isRunningAsCompiledJar() {
        String jarFilePath = CopyAndModifyFolder.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        return jarFilePath.toLowerCase().endsWith(".jar");
    }

    private static String getJarFileName() {
        String jarFilePath;
        try {
            jarFilePath = CopyAndModifyFolder.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        File jarFile = new File(jarFilePath);
        return jarFile.getName();
    }

    private static String getConfigFilePath(String configFileName) {
        // Try to find the configuration file in the current working directory
        Path configPath = Paths.get(configFileName);
        if (Files.exists(configPath)) {
            return configPath.toString();
        }

        // If the configuration file is not found in the current working directory,
        // try to locate it relative to the class path
        URL resource = CopyAndModifyFolder.class.getClassLoader().getResource(configFileName);
        if (resource != null) {
            try {
                return Paths.get(resource.toURI()).toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        // If the configuration file is not found in both locations, return null
        return null;
    }

    private static void checkForDeletions(File source, File destination, PrintWriter logWriter) {
        logWriter.println("Checking for deletions in " + destination.getAbsolutePath());

        File[] destinationFiles = destination.listFiles();
        if (destinationFiles != null) {
            for (File destinationFile : destinationFiles) {
                String relativePath = getRelativePath(destinationFile, destination);

                File sourceFile = new File(source, relativePath);
                if (!sourceFile.exists()) {
                    if (destinationFile.isDirectory()) {
                        deleteFolder(destinationFile, logWriter);
                    } else {
                        if (destinationFile.delete()) {
                            logWriter.println("Deleted: " + destinationFile.getAbsolutePath());
                        } else {
                            logWriter.println("Failed to delete: " + destinationFile.getAbsolutePath());
                        }
                    }
                }
            }
        }

        logWriter.println("Deletion check complete.");
    }
    private static String getRelativePath(File file, File baseDir) {
        // Get the relative path of the file with respect to the base directory
        Path filePath = file.toPath();
        Path basePath = baseDir.toPath();
        return basePath.relativize(filePath).toString();
    }
    private static ConfigEntry readConfigFile(String configFilePath) {
        try (Scanner scanner = new Scanner(new File(configFilePath))) {
            List<SourceFolderConfig> sourceFolders = new ArrayList<>();
            List<SourceFilesConfig> sourceFiles = new ArrayList<>();
            List<String> configList = new ArrayList<>();
            List<String> folderList = new ArrayList<>();
            int mainDirectoryLevel = 0;
            boolean runFromMods = false;
            String globalOverrideCustomOutputDir = null;
            boolean useCustomOutputDir = false;
            boolean isSourceFiles = false;
            boolean isSourceFolders = false;
            boolean isConfigList = false;
            boolean isFolderList = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    // Skip empty lines and comments
                    continue;
                }

                if (line.startsWith("SOURCE_FOLDERS:")) {
                    isSourceFolders = true;
                    isConfigList = false;
                    isFolderList = false;
                    isSourceFiles = false;
                } else if (line.startsWith("SOURCE_FILES:")) {
                    isSourceFolders = false;
                    isConfigList = false;
                    isFolderList = false;
                    isSourceFiles = true;
                } else if (line.startsWith("CONFIG_LIST:")) {
                    isSourceFolders = false;
                    isConfigList = true;
                    isFolderList = false;
                    isSourceFiles = false;
                } else if (line.startsWith("FOLDER_LIST:")) {
                    isSourceFolders = false;
                    isConfigList = false;
                    isFolderList = true;
                    isSourceFiles = false;
                }
                else {
                    if (isSourceFolders || isSourceFiles) {
                        String source = "";
                        String customOutputDir = "";
                        while (scanner.hasNextLine()) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                if (parts[0].startsWith("-")) {
                                    source = parts[1].trim();
                                    line = scanner.nextLine().trim();
                                    continue;
                                }
                                customOutputDir = parts[1].trim();
                                break;
                            }
                        }
                        if (source.isEmpty() && customOutputDir.isEmpty()) {
                            System.out.println("Skipping this because empty");
                        }
                        if (isSourceFiles)
                            sourceFiles.add(new SourceFilesConfig(source, customOutputDir));
                        else
                            sourceFolders.add(new SourceFolderConfig(source, customOutputDir));
                    } else if (isConfigList) {
                        configList.add(line);
                    } else if (isFolderList) {
                        folderList.add(line);
                    } else {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            String variable = parts[0].trim();
                            String value = parts[1].trim();
                            if (variable.equals("MAIN_DIRECTORY_LEVEL")) {
                                mainDirectoryLevel = Integer.parseInt(value);
                            } else if (variable.equals("RUN_FROM_MODS")) {
                                runFromMods = Boolean.parseBoolean(value);
                            } else if (variable.equals("GLOBAL_OVERRIDE_CUSTOM_OUTPUT_DIR")) {
                                globalOverrideCustomOutputDir = value;
                            } else if (variable.equals("USE_CUSTOM_OUTPUT_DIR")) {
                                useCustomOutputDir = Boolean.parseBoolean(value);
                            }
                            // Add more else-if conditions if you have more variables in the config
                            // ...
                        }
                    }
                }
            }

            return new ConfigEntry(mainDirectoryLevel, runFromMods, globalOverrideCustomOutputDir,
                    useCustomOutputDir, sourceFolders, sourceFiles, configList, folderList);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void copyFolder(File source, File destination, PrintWriter logWriter, String inputFolder, String outputFolder) throws IOException {
        logWriter.println("Copying folder from: " + inputFolder + " to " + outputFolder);

        if (!destination.exists()) {
            Files.createDirectories(destination.toPath());
        }

        File[] files = source.listFiles();
        if (files != null) {
            for (File sourceFile : files) {
                File destinationFile = new File(destination, sourceFile.getName());

                try {
                    if (sourceFile.isDirectory()) {
                        copyFolder(sourceFile, destinationFile, logWriter, inputFolder, outputFolder);
                    } else {
                        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (AccessDeniedException e) {
                    logWriter.println("Access denied while copying file: " + sourceFile.getAbsolutePath());
                } catch (IOException e) {
                    logWriter.println("Error copying file: " + sourceFile.getAbsolutePath() + " - " + e.getMessage());
                }
            }
        } else {
            logWriter.println("Source folder is empty or does not exist: " + source.getAbsolutePath());
        }

        logWriter.println("Folder copy complete. Source folder: " + inputFolder + ", Destination folder: " + outputFolder);
    }



    private static void copyFile(File source, File destination, PrintWriter logWriter, String inputFolder, String outputFolder) throws IOException {
        logWriter.println("Copying file from: " + inputFolder + " to " + outputFolder);
        File parentDir = destination.getParentFile();
        if (!parentDir.exists()) {
            Files.createDirectories(destination.toPath());
        }
        if (source.exists()) {
            try {
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }catch (Exception e){
                logWriter.println("Error while copying file. "+e.getMessage());
                e.printStackTrace();
            }
        } else {
            logWriter.println("Source folder is empty or does not exist: " + source.getAbsolutePath());
        }

        logWriter.println("Folder copy complete. Source folder: " + inputFolder + ", Destination folder: " + outputFolder);
    }

    private static void removeConfigFiles(List<String> configList, File destination, PrintWriter logWriter) {
        logWriter.println("Removing config files: " + configList + " from " + destination.getAbsolutePath());

        for (String configFileName : configList) {
            File configFile = new File(destination, configFileName);
            if (configFile.exists()) {
                if (configFile.delete()) {
                    logWriter.println("Deleted: " + configFile.getAbsolutePath());
                } else {
                    logWriter.println("Failed to delete: " + configFile.getAbsolutePath());
                }
            } else {
                logWriter.println("Config file not found: " + configFile.getAbsolutePath());
            }
        }

        logWriter.println("Config file removal complete.");
    }

    private static void deleteFolders(List<String> folderList, File destination, PrintWriter logWriter) {
        logWriter.println("Deleting folders: " + folderList + " from " + destination.getAbsolutePath());

        for (String folderName : folderList) {
            File folder = new File(destination, folderName);
            if (folder.exists()) {
                deleteFolder(folder, logWriter);
            } else {
                logWriter.println("Folder not found: " + folder.getAbsolutePath());
            }
        }

        logWriter.println("Folder deletion complete.");
    }

    private static void deleteFolder(File folder, PrintWriter logWriter) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file, logWriter);
                } else {
                    if (file.delete()) {
                        logWriter.println("Deleted: " + file.getAbsolutePath());
                    } else {
                        logWriter.println("Failed to delete: " + file.getAbsolutePath());
                    }
                }
            }
        }

        if (folder.delete()) {
            logWriter.println("Deleted folder: " + folder.getAbsolutePath());
        } else {
            logWriter.println("Failed to delete folder: " + folder.getAbsolutePath());
        }
    }
    private static class SourceFilesConfig {
        private final String source;
        private final String customOutputDir;

        public SourceFilesConfig(String source, String customOutputDir) {
            this.source = source;
            this.customOutputDir = customOutputDir;
        }

        public String getSource() {
            return source;
        }

        public String getCustomOutputDir() {
            return customOutputDir;
        }
    }
    private static class SourceFolderConfig {
        private final String source;
        private final String customOutputDir;

        public SourceFolderConfig(String source, String customOutputDir) {
            this.source = source;
            this.customOutputDir = customOutputDir;
        }

        public String getSource() {
            return source;
        }

        public String getCustomOutputDir() {
            return customOutputDir;
        }
    }

    private static class ConfigEntry {
        private final int mainDirectoryLevel;
        private final boolean runFromMods;
        private final String globalOverrideCustomOutputDir;
        private final boolean useCustomOutputDir;
        private final List<SourceFolderConfig> sourceFolders;
        private final List<SourceFilesConfig> sourceFiles;
        private final List<String> configList;
        private final List<String> folderList;

        public ConfigEntry(int mainDirectoryLevel, boolean runFromMods, String globalOverrideCustomOutputDir,
                           boolean useCustomOutputDir, List<SourceFolderConfig> sourceFolders,  List<SourceFilesConfig> sourceFiles, List<String> configList,
                           List<String> folderList) {
            this.mainDirectoryLevel = mainDirectoryLevel;
            this.runFromMods = runFromMods;
            this.globalOverrideCustomOutputDir = globalOverrideCustomOutputDir;
            this.useCustomOutputDir = !useCustomOutputDir; // Inverting the value here
            this.sourceFolders = sourceFolders;
            this.sourceFiles = sourceFiles;
            this.configList = configList;
            this.folderList = folderList;
        }

        public int getMainDirectoryLevel() {
            return mainDirectoryLevel;
        }// no bruh k then ill send u link in discord and use on ur pc // i k there just talk to the jew if u get stuck kek jew coded so ask jew wtf this bomb is
        //k u can start also can u alt tab to edge see if u can talk to jewgpt  basically the code despite me adding more logging even logging nulls and even like shit that isnt used it wont show shit in the .log and also the main directory for the program in iDE is CopyAndModifyFolder (not in the class folder ignore that CopyAndModifyFolder Folder)
        public boolean isRunFromMods() {
            return runFromMods;
        }

        public String getGlobalOverrideCustomOutputDir() {
            return globalOverrideCustomOutputDir;
        }

        public boolean isUseCustomOutputDir() {
            return useCustomOutputDir;
        }

        public List<SourceFolderConfig> getSourceFolders() {
            return sourceFolders;
        }

        public List<SourceFilesConfig> getSourceFiles() {
            return sourceFiles;
        }

        public List<String> getConfigList() {
            return configList;
        }

        public List<String> getFolderList() {
            return folderList;
        }
    }
}