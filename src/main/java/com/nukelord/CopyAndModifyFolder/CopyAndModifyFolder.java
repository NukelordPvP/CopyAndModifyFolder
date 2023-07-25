package com.nukelord.CopyAndModifyFolder;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CopyAndModifyFolder {
    private static final String CONFIG_FILE = "config-server-client-side-files.txt";
    private static final String SOURCE_FOLDER = "source_folder";
    private static final String DESTINATION_FOLDER = "destination_folder";
    private static final String LOG_FILE = "CopyAndModifyFolder.log";

    public static void main(String[] args) {
        String configFilePath = "config-server-client-side-files.txt"; // Provide the correct path to the config file
//the reason why its a string is because it supports renaming the config.txt via cmd so for non mc uses its not named larp kek
        // Read configuration from the config file
        ConfigEntry mainConfigEntry = readConfigFile(configFilePath);

        if (mainConfigEntry != null) {
            // Get the main directory level (root folder from where the program should run)
            int mainDirectoryLevel = mainConfigEntry.getMainDirectoryLevel();

            // Set the source and destination folders based on the main directory level
            File current_directory = new File( ".");

            for (int i = 0; i < mainDirectoryLevel; i++) {
                current_directory = current_directory.getParentFile();
            }

            // Create log file
            try (PrintWriter logWriter = new PrintWriter(new FileWriter(LOG_FILE))) {
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
                    String outputFolder =(mainConfigEntry.isUseCustomOutputDir() ? mainConfigEntry.getGlobalOverrideCustomOutputDir() :"")+ folderConfig.getCustomOutputDir();

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

                logWriter.println("\nTask completed successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Config entry not found or config file is not valid.");
        }
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
            List<String> configList = new ArrayList<>();
            List<String> folderList = new ArrayList<>();
            int mainDirectoryLevel = 0;
            boolean runFromMods = false;
            String globalOverrideCustomOutputDir = null;
            boolean useCustomOutputDir = false;
            boolean isSourceFolders = false;
            boolean isConfigList = false;
            boolean isFolderList = false;
//WAIT so THATS how u see variable info KEK ive been looking for that shit no wonder i couldnt do java LOL
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    // Skip empty lines and comments
                    continue;
                }
// ree also the .txt to use isnt in classes it is reading form main dir of the project. src or project PROJECt cuz bomb kek
                if (line.startsWith("SOURCE_FOLDERS:")) {
                    isSourceFolders = true;
                    isConfigList = false;
                    isFolderList = false;
                } else if (line.startsWith("CONFIG_LIST:")) {
                    isSourceFolders = false;
                    isConfigList = true;
                    isFolderList = false;
                } else if (line.startsWith("FOLDER_LIST:")) {
                    isSourceFolders = false;
                    isConfigList = false;
                    isFolderList = true;
                } else {
                    if (isSourceFolders) {
                        String source = "";
                        String customOutputDir = "";
                        while(scanner.hasNextLine()) {
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
                        if(source.isEmpty() && customOutputDir.isEmpty()){
                            System.out.println("Skipping this because empty");
                        }
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
                    useCustomOutputDir, sourceFolders, configList, folderList);
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

                if (sourceFile.isDirectory()) {
                    copyFolder(sourceFile, destinationFile, logWriter, inputFolder, outputFolder);
                } else {
                    Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
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
        private final List<String> configList;
        private final List<String> folderList;

        public ConfigEntry(int mainDirectoryLevel, boolean runFromMods, String globalOverrideCustomOutputDir,
                           boolean useCustomOutputDir, List<SourceFolderConfig> sourceFolders, List<String> configList,
                           List<String> folderList) {
            this.mainDirectoryLevel = mainDirectoryLevel;
            this.runFromMods = runFromMods;
            this.globalOverrideCustomOutputDir = globalOverrideCustomOutputDir;
            this.useCustomOutputDir = !useCustomOutputDir; // Inverting the value here
            this.sourceFolders = sourceFolders; // bomb. source folders is empty also this is way easier to understand cuz in redstone i can see the shit stored in each variable i make kek like this. yeah this is why debugger is invented kek
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

        public List<String> getConfigList() {
            return configList;
        }

        public List<String> getFolderList() {
            return folderList;
        }
    }
}