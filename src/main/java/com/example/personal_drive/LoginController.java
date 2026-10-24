package com.example.personal_drive;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping ("/test/login")
public class LoginController {
    
    // creates a LoginService object so LoginController can use its methods
    private final LoginService loginService;

    public LoginController(LoginService loginService){
        this.loginService = loginService;
    }

    // @PostMapping sends and adds new information to the server
    @PostMapping 
    // @RequestBody reads the incoming data and converts it to usable form
    public boolean checkLogin(@RequestBody LoginInfo loginInfo){
        return loginService.checkLogin(
            loginInfo.email(), 
            loginInfo.password()
        );
    }

    // stores the information inside here adn defines what kind of data they are
    public record LoginInfo(String email, String password){

    };

}
