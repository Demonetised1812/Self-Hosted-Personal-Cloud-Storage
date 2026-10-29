package com.example.personal_drive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class FileStorageService {
    
    // Holds onto the root file path
    private final Path root;

    // This method exists so taht upload folder exists and the full file path is known
    public FileStorageService() throws IOException{
        // Gets the full file path and cleans it up
        Path uploadDirectory = Paths.get("uploads").toAbsolutePath().normalize();

        // Creates the directory if missing
        Files.createDirectories(uploadDirectory);

        // Assigns the actual full filepath to the root variable
        root = uploadDirectory.toRealPath();
    }

    //  checks if name is valid before upload
    private void validateName(String name){
        // rejects name if it will cause issues
        if(name == null
            || name.isBlank()
            || name.equals(".") // . means current directory
            || name.equals("..") // .. means parent directory
            || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") //checks for forbidden characters
            || name.endsWith(".") //causes problems on windows
            || name.endsWith(" ")) //causes problems on windows 
                {
            // Gives error code and message if name is not valid
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Enter a valid name for the file or folder"
            );
        }
    }    

    // only lists what is stored in the file itself
    private boolean isStoredItem(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
    }

    private FileInfo toFileInfo(Path path) throws IOException{
        BasicFileAttributes attributes = Files.readAttributes(
            path, 
            BasicFileAttributes.class, 
            LinkOption.NOFOLLOW_LINKS);

        String relativePath = root.relativize(path).toString().replace("\\", "/");

        String parentFolder = root.relativize(path.getParent()).toString().replace("\\", "/");

        return new FileInfo(
            path.getFileName().toString(),
            relativePath,
            parentFolder,
            // checks if its a folder or not
            attributes.isDirectory(),
            // gives the size of the file if it is not a folder 
            attributes.isDirectory() ? 0 : attributes.size(),
            attributes.creationTime().toInstant()
        );
    }

    // Upload multiple files into a folder
    public List<FileInfo> uploadFiles(MultipartFile[] files, String folder) throws IOException{

        Path destinationFolder = findFolder(folder);
        List<FileInfo> uploadedFiles = new ArrayList<>();

        if (files.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select at least one file."
            );
        }

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();

            validateName(name);

            Path destination = destinationFolder.resolve(name);

            // Copy the files into the folder
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }

            uploadedFiles.add(toFileInfo(destination));
        }

        return uploadedFiles;
    }

    // Create a folder inside another folder
    public FileInfo createFolder(String name, String parentFolder) throws IOException{

        validateName(name);

        Path parent = findFolder(parentFolder);
        // finds the destination folder name
        Path newFolder = parent.resolve(name);

        // Fails if an item with this name already exists.
        Files.createDirectory(newFolder);

        return toFileInfo(newFolder);
    }

    // Return only the items directly inside the selected folder
    public List<FileInfo> listFiles(String folder) throws IOException {
        Path directory = findFolder(folder);
        List<FileInfo> items = new ArrayList<>();

        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.toList()) {
                // Only include actual files and folders.
                if (isStoredItem(path)) {
                    items.add(toFileInfo(path));
                }
            }
        }

        // Put folders first, then sort their names alphabetically
        items.sort(
                Comparator.comparing(FileInfo::directory).reversed()
                        .thenComparing(
                                FileInfo::name,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return items;
    }

    // Return every folder, including subfolders, for the left sidebar
    public List<FileInfo> listFolders() throws IOException {
        List<FileInfo> folders = new ArrayList<>();

        // walk() looks inside subfolders
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                if (!path.equals(root)
                        && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    folders.add(toFileInfo(path));
                }
            }
        }

        // Arranges the files
        folders.sort(
                Comparator.comparing(
                        FileInfo::path,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        return folders;
    }

    // Return the 10 newest files
    public List<FileInfo> recentFiles() throws IOException {
        List<FileInfo> files = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    files.add(toFileInfo(path));
                }
            }
        }

        return files.stream()
                .sorted(
                        Comparator.comparing(FileInfo::createdAt).reversed()
                )
                .limit(10)
                .toList();
    }

    // Find an existing folder while keeping access inside uploads
    private Path findFolder(String folder) throws IOException {
        String requestedFolder = folder == null ? "" : folder;
        Path relativePath = Paths.get(requestedFolder);

        if (relativePath.isAbsolute()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use a path relative to My Drive."
            );
        }

        Path directory = root.resolve(relativePath).normalize();

        if (!directory.startsWith(root)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid folder location."
            );
        }

        // Resolve filesystem links before checking the location again
        Path realDirectory = directory.toRealPath();

        if (!realDirectory.startsWith(root)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid folder location."
            );
        }

        if (!Files.isDirectory(realDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This location is not a folder."
            );
        }

        return realDirectory;
    }

    // Permanently delete one file or an empty folder.
    public void deleteItem(String itemPath) throws IOException {
        if (itemPath == null || itemPath.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select a file or folder to delete."
            );
        }

        Path relativePath = Paths.get(itemPath);

        if (relativePath.isAbsolute()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use a path relative to the drive."
            );
        }

        Path target = root.resolve(relativePath).normalize();

        // Never allow deleting the main uploads folder or anything outside it
        if (!target.startsWith(root) || target.equals(root)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file or folder location."
            );
        }

        // Check the actual parent folder
        Path realParent = target.getParent().toRealPath();

        if (!realParent.startsWith(root)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file or folder location."
            );
        }

        Path checkedTarget = realParent.resolve(target.getFileName());

        Files.delete(checkedTarget);
    }
}
