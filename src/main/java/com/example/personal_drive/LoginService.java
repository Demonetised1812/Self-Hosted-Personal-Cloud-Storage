package com.example.personal_drive;

import org.springframework.stereotype.Service;

@Service
public class LoginService {
    
    // input validation for the login page
    public boolean checkLogin(String email, String password){
        if(email == null || password == null){
            return false;
        }

        String lowerEmail = email.toLowerCase();

        return lowerEmail.endsWith("@gmail.com") &&
               lowerEmail.length() > "@gmail.com".length() &&
               !password.isBlank();
    }

}
