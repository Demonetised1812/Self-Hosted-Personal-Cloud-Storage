package com.example.personal_drive;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;
import java.nio.file.DirectoryNotEmptyException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storageService;

    public FileController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    // Upload the selected files into the requested folder
    @PostMapping("/upload")
    public List<FileInfo> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", defaultValue = "") String folder
    ) throws IOException {

        return storageService.uploadFiles(files, folder);
    }

    // Get the files and folders directly inside the requested folder
    @GetMapping
    public List<FileInfo> listFiles(
            @RequestParam(value = "folder", defaultValue = "") String folder
    ) throws IOException {

        return storageService.listFiles(folder);
    }

    // Create a folder using the name and parent folder sent as JSON data
    @PostMapping("/folders")
    public FileInfo createFolder(
            @RequestBody FolderRequest request
    ) throws IOException {

        return storageService.createFolder(
                request.name(),
                request.parentFolder()
        );
    }

    // Get all folders for the left sidebar
    @GetMapping("/folders")
    public List<FileInfo> listFolders() throws IOException {
        return storageService.listFolders();
    }

    // Get the 10 newest files
    @GetMapping("/recent")
    public List<FileInfo> recentFiles() throws IOException {
        return storageService.recentFiles();
    }

    // Holds the information received when creating a folder
    public record FolderRequest(String name, String parentFolder) {
    }

    // Gives reasoning on why it didnt work
    @ExceptionHandler(FileAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDuplicate() {
        return Map.of(
                "message",
                "A file or folder with that name already exists."
        );
    }

    // Gives reasoning on why it didnt work
    @ExceptionHandler(NoSuchFileException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingLocation() {
        return Map.of(
                "message",
                "The requested file or folder does not exist."
        );
    }

    // Handle other failures when reading or writing files
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleStorageError() {
        return Map.of(
                "message",
                "Unable to read or write files on the server."
        );
    }

    // Delete the selected items via its file path
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestParam("path") String path
    ) throws IOException {

        storageService.deleteItem(path);
    }

    // explaination on why folder cant be deleted
    @ExceptionHandler(DirectoryNotEmptyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleNonEmptyFolder() {
        return Map.of(
                "message",
                "This folder is not empty. Delete its contents first."
        );
    }
}