// Confirms that the website backend is up and running, wont work if backend is down
package com.example.personal_drive;

// uses GetMapping to handle get requests and RestController to handle web requests
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// when people request for information from the web, it returns the data to the requester
@RestController
public class HealthController {

    // connects the request to a method and runs the method
    @GetMapping("/api/health")
    public String health() {
        return "Personal Drive is running";
    }
}