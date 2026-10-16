// Used to handle upload, download and delete file requests
package com.example.personal_drive;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Handles web requests by returening values directly back to sender as response data
@RestController 
public class FileController {

    // calls the upload file method after a post request arrives
    @PostMapping("/api/files")
    // Takes the uploaded file item and lets java access its contents
    public String uploadFile(@RequestParam ("file") MultipartFile file) {

        if (file.isEmpty()) {
            return "Please select a file that is not empty.";
        }
    
        // If file isnt empty get the file name and size provided by the sender
        return "Received: " + file.getOriginalFilename()
                + " | Size: " + file.getSize() + " bytes";

    }

}

// to test functionality create txt file
// ".\mvnw.cmd spring-boot:run" paste that into terminal
// "curl.exe -F "file=@test.txt" http://localhost:8080/api/files" paste that to send the file in another terminal