package com.example.personal_drive;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController 
public class FileController {
    @PostMapping("/api/files")
    public String uploadFile(@RequestParam ("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Please select a file that is not empty.";
        }
    
        return "Received: " + file.getOriginalFilename()
                + " | Size: " + file.getSize() + " bytes";
    }
}

