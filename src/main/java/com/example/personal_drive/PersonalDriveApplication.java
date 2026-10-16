// used to run the application only, other funtions are handled by the other java files
package com.example.personal_drive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PersonalDriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalDriveApplication.class, args);
	}

}