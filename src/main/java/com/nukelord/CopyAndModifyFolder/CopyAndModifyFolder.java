package com.nukelord.CopyAndModifyFolder;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;


public class CopyAndModifyFolder {
    // ANSI escape code colors
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_GREY = "\u001B[90m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_PURPLE = "\u001B[35m"; // Purple color code added

    // Color variable id constants
    private static final String COLOR_WARNING = ANSI_YELLOW; // Change this to ANSI_YELLOW or any gold color code
    private static final String COLOR_SOURCE = ANSI_GREY;
    private static final String COLOR_OUTPUT = ANSI_RED;
    private static final String COLOR_OVERWRITE = ANSI_PURPLE;
    private static final String COLOR_DELETION = ANSI_PURPLE; // Change this to ANSI_PURPLE or any pink color code for deletions
    private static final String COLOR_GREY = ANSI_GREY;

    private static final String WARNING = COLOR_WARNING;
    private static final String SOURCE = COLOR_GREY;
    private static final String OUTPUT = COLOR_OUTPUT;
    private static final String OVERWRITE = COLOR_OVERWRITE;
    private static final String DELETION = COLOR_DELETION;
    private static final String GREY = COLOR_GREY;
    private static final String LOG_EXTENSION = ".log";
    private static final String CONFIG_EXTENSION = ".txt";
    private static final String ANSI_LOG_EXTENSION = ".ans";

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String DARK_GREEN = "\u001B[32m";
    private static final String GREEN = "\u001B[32m";
    private static final String PURPLE = "\u001B[35m";
    private static final String PINK = "\u001B[35m";
    public static void main(String[] args) {
        // Determine the configuration file name based on the program execution type
        String configFileName;
        String logFileName;

        if (isRunningAsCompiledJar()) {
            String jarFileName = getJarFileName();
            configFileName = jarFileName.replace(".jar", CONFIG_EXTENSION);
            logFileName = jarFileName.replace(".jar", ANSI_LOG_EXTENSION);
        } else {
            configFileName = CopyAndModifyFolder.class.getSimpleName() + CONFIG_EXTENSION;
            logFileName = CopyAndModifyFolder.class.getSimpleName() + ANSI_LOG_EXTENSION;
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
                println(logWriter, "MAIN_DIRECTORY_LEVEL:" + mainDirectoryLevel, WARNING);
                println(logWriter, "RUN_FROM_MODS:" + mainConfigEntry.isRunFromMods(), WARNING);
                println(logWriter, "GLOBAL_OVERRIDE_CUSTOM_OUTPUT_DIR:" + (mainConfigEntry.getGlobalOverrideCustomOutputDir() != null ?
                        "\"" + mainConfigEntry.getGlobalOverrideCustomOutputDir() + "\"" : "Not set"), WARNING);
                println(logWriter, "USE_CUSTOM_OUTPUT_DIR:" + mainConfigEntry.isUseCustomOutputDir(), WARNING);
                println(logWriter, "", WARNING); // Empty line for spacing


                for (SourceFolderConfig folderConfig : sourceFolders) {
                    String inputFolder = folderConfig.getSource();
                    String outputFolder = (mainConfigEntry.isUseCustomOutputDir() ? mainConfigEntry.getGlobalOverrideCustomOutputDir() : "") + folderConfig.getCustomOutputDir();

                    if (inputFolder == null || outputFolder == null) {
                        println(logWriter, getColoredMessage("Skipping entry: Incomplete configuration. Both source and destination folders must be specified.", WARNING), WARNING);
                        println(logWriter, getColoredMessage("Source folder: " + (inputFolder != null ? inputFolder : "Not set"), WARNING), WARNING);
                        println(logWriter, getColoredMessage("Destination folder: " + (outputFolder != null ? outputFolder : "Not set"), WARNING), WARNING);
                        continue;
                    }

                    File sourceEntryFolder = new File(current_directory, inputFolder);
                    File destinationEntryFolder = new File(current_directory, outputFolder);

                    // Log specific entry configurations
                    println(logWriter, "Processing entry:", WARNING);
                    printFilePath(logWriter, sourceEntryFolder.getAbsolutePath(), GREY);
                    printFilePath(logWriter, destinationEntryFolder.getAbsolutePath(), GREY);
                    println(logWriter, "CONFIG_LIST: " + mainConfigEntry.getConfigList(), WARNING);
                    println(logWriter, "FOLDER_LIST: " + mainConfigEntry.getFolderList(), WARNING);

                    // Copy the source folder to the destination folder
                    copyFolder(sourceEntryFolder, destinationEntryFolder, logWriter, inputFolder, outputFolder);

                    // Remove config files from the copied folder
                    removeConfigFiles(mainConfigEntry.getConfigList(), destinationEntryFolder, logWriter);

                    // Delete specified folders from the copied folder
                    deleteFolders(mainConfigEntry.getFolderList(), destinationEntryFolder, logWriter);

                    // Check for deletions in the destination folder
                    checkForDeletions(sourceEntryFolder, destinationEntryFolder, logWriter);

                    println(logWriter, getColoredMessage("Processing complete for this entry.", WARNING), WARNING);
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
                    println(logWriter, "Processing entry:", WARNING);
                    printFilePath(logWriter, sourceEntryFile.getAbsolutePath(), GREY);
                    printFilePath(logWriter, destinationEntryFile.getAbsolutePath(), GREY);

                    // Copy the source file to the destination file
                    copyFile(sourceEntryFile, destinationEntryFile, logWriter, inputFolder, outputFolder);

                    // Check for deletions in the destination file's parent folder
                    checkForDeletions(sourceEntryFile.getParentFile(), destinationEntryFile.getParentFile(), logWriter);

                    logWriter.println("Processing complete for this entry.");
                }

                println(logWriter, getColoredMessage("Task completed successfully.", WARNING), WARNING);
                logWriter.flush(); // Flush the log to ensure all messages are written before the program exits
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Helper method to apply color to a message based on the specified color variable id
    private static String getColoredMessage(String message, String color) {
        return color + message + ANSI_RESET;
    }

    // Modified println methods with color variable id
    private static void println(PrintWriter logWriter, String message, String color) {
        logWriter.println(getColoredMessage(message, color));
    }

    // Print source and destination file paths with colors
    private static void printFilePath(PrintWriter logWriter, String filePath, String color) {
        println(logWriter, filePath, color);
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
            List<File> deletedFiles = new ArrayList<>();

            for (File destinationFile : destinationFiles) {
                String relativePath = getRelativePath(destinationFile, destination);
                File sourceFile = new File(source, relativePath);

                if (!sourceFile.exists()) {
                    deletedFiles.add(destinationFile);
                }
            }

            if (!deletedFiles.isEmpty()) {
                logWriter.println("WARNING PROGRAM MALFUNCTION: The following files were deleted in the source folder:");
                for (File deletedFile : deletedFiles) {
                    logWriter.println("Deleted file: " + deletedFile.getAbsolutePath());
                }
                logWriter.println("Please verify your configuration and program logic to prevent unintended deletions.");
            } else {
                logWriter.println("No deletions detected in the source folder.");
            }
        } else {
            logWriter.println("Destination folder is empty or does not exist: " + destination.getAbsolutePath());
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

    private static void copyFolder(File source, File destination, PrintWriter logWriter, String sourceFolder, String destinationFolder) throws IOException {
        // Normalize the input and output folder paths
        Path sourcePath = Paths.get(source.getAbsolutePath()).normalize();
        Path destinationPath = Paths.get(destination.getAbsolutePath()).normalize();

        logWriter.println("Copying folder from:\n" + sourcePath + "\nto\n" + destinationPath);

        if (!destination.exists()) {
            Files.createDirectories(destination.toPath());
        }

        File[] files = source.listFiles();
        if (files != null) {
            for (File sourceFile : files) {
                File destinationFile = new File(destination, sourceFile.getName());

                try {
                    if (sourceFile.isDirectory()) {
                        copyFolder(sourceFile, destinationFile, logWriter, sourceFolder, destinationFolder);
                    } else {
                        copyFile(sourceFile, destinationFile, logWriter, sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    if (Files.isRegularFile(sourceFile.toPath())) {
                        logWriter.println("ERROR: Error copying file:\n" + sourceFile.getAbsolutePath() + " - " + e.getMessage());
                    } else {
                        logWriter.println(PINK + "WARNING: Access denied while copying file:\n" + sourceFile.getAbsolutePath() + RESET);
                    }
                }
            }
        } else {
            logWriter.println("ERROR: Source folder is empty or does not exist:\n" + source.getAbsolutePath());
        }

        logWriter.println(DARK_GREEN + "Folder copy complete.\nSource folder: " + sourceFolder + "\nDestination folder: " + destinationFolder + RESET);
    }


    private static void copyFile(File source, File destination, PrintWriter logWriter, String sourceFile, String destinationFile) throws IOException {
        // Normalize the input and output file paths
        Path sourcePath = Paths.get(source.getAbsolutePath()).normalize();
        Path destinationPath = Paths.get(destination.getAbsolutePath()).normalize();

        logWriter.println("Copying file from:\n" + sourcePath + "\nto\n" + destinationPath);

        File parentDir = destination.getParentFile();
        if (!parentDir.exists()) {
            Files.createDirectories(destination.toPath());
        }

        if (source.exists()) {
            try {
                if (destination.exists()) {
                    long sourceLastModified = source.lastModified();
                    long destinationLastModified = destination.lastModified();

                    if (destinationLastModified < sourceLastModified) {
                        if (!destination.delete()) {
                            logWriter.println("WARNING: Failed to delete existing file:\n" + destination.getAbsolutePath());
                            logWriter.println("Skipping file copy.");
                            return;
                        }
                        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        logWriter.println("File copied successfully.\nSource file: " + sourceFile + "\nDestination file: " + destinationFile);
                    } else if (destinationLastModified == sourceLastModified) {
                        logWriter.println("Destination file is up-to-date. Skipping file copy:\n" + destinationFile);
                    } else {
                        logWriter.println("Overwriting older file in destination folder:\n" + destinationFile);
                        Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        logWriter.println("File copied successfully.\nSource file: " + sourceFile + "\nDestination file: " + destinationFile);
                    }
                } else {
                    Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logWriter.println("File copied successfully.\nSource file: " + sourceFile + "\nDestination file: " + destinationFile);
                }
            } catch (IOException e) {
                if (Files.isRegularFile(source.toPath())) {
                    logWriter.println("ERROR: Error copying file: " + sourceFile + " - " + e.getMessage());
                } else {
                    logWriter.println(PINK + "WARNING: Access denied while copying file:\n" + sourceFile + RESET);
                }
            }
        } else {
            logWriter.println("ERROR: Source file not found:\n" + sourceFile);
        }
    }

    private static void removeConfigFiles(List<String> configList, File destination, PrintWriter logWriter) {
        println(logWriter, "Removing config files: " + configList + " from ", WARNING);
        printFilePath(logWriter, destination.getAbsolutePath(), WARNING);


        File[] destinationFiles = destination.listFiles();
        if (destinationFiles != null) {
            for (File destinationFile : destinationFiles) {
                String relativePath = getRelativePath(destinationFile, destination);
                logWriter.println("Checking relativePath: " + relativePath);

                if (configList.contains(relativePath)) {
                    logWriter.println("Found match in configList: " + relativePath);

                    File sourceFile = new File(destination, relativePath);
                    if (sourceFile.exists()) {
                        if (destinationFile.isDirectory()) {
                            deleteFolder(destinationFile, logWriter);
                        } else {
                            if (destinationFile.delete()) {
                                logWriter.println("Deleted: " + destinationFile.getAbsolutePath());
                            } else {
                                logWriter.println("Failed to delete: " + destinationFile.getAbsolutePath());
                            }
                        }
                    } else {
                        logWriter.println("Source file not found: " + sourceFile.getAbsolutePath());
                    }
                }
            }
        } else {
            logWriter.println("Destination folder is empty or does not exist: " + destination.getAbsolutePath());
        }

        println(logWriter, "Config file removal complete.", WARNING);
    }


    private static void deleteFolders(List<String> folderList, File destination, PrintWriter logWriter) {
        println(logWriter, "Deleting folders: " + folderList + " from ", WARNING);
        printFilePath(logWriter, destination.getAbsolutePath(), WARNING);

        for (String folderName : folderList) {
            File folder = new File(destination, folderName);
            if (folder.exists()) {
                deleteFolder(folder, logWriter);
            } else {
                println(logWriter, "Folder not found:", WARNING);
                printFilePath(logWriter, folder.getAbsolutePath(), WARNING);
            }
        }

        println(logWriter, "Folder deletion complete.", WARNING);
    }

    private static void deleteFolder(File folder, PrintWriter logWriter) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file, logWriter);
                } else {
                    if (file.delete()) {
                        println(logWriter, "Deleted:", DELETION);
                        printFilePath(logWriter, file.getAbsolutePath(), DELETION);
                    } else {
                        println(logWriter, "Failed to delete:", WARNING);
                        printFilePath(logWriter, file.getAbsolutePath(), WARNING);
                    }
                }
            }
        }

        if (folder.delete()) {
            println(logWriter, "Deleted folder:", DELETION);
            printFilePath(logWriter, folder.getAbsolutePath(), DELETION);
        } else {
            println(logWriter, "Failed to delete folder:", WARNING);
            printFilePath(logWriter, folder.getAbsolutePath(), WARNING);
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