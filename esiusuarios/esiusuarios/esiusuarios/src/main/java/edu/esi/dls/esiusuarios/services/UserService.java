package edu.esi.dls.esiusuarios.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.esi.dls.esiusuarios.auxiliares.Manager;
import edu.esi.dls.esiusuarios.model.User;

@Service
public class UserService {
    private List<User> users;

    public UserService() {
        this.users = new ArrayList<>();
        this.users.add(new User("Pepe", "pepe@example.com", "pepe123", "1234"));
        this.users.add(new User("Ana", "ana@example.com", "ana123", "567"));
    }

    public String login(String name, String password) {
        for(User user : this.users){
            if(user.getName().equalsIgnoreCase(name) && user.getPassword().equals(password)){
                return "Login successful";
            }
        }
        return null;
    }

    public String checkToken(String token) {
        for(User user : this.users){
            if(user.getToken().equals(token)){
                return user.getName();
            }
        }
        return null;
    }

    public String register(String username, String email, String pwd1) {
        for(User user : this.users){
            if(user.getName().equalsIgnoreCase(username)){
                return null;
            }

            if(user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)){
                return null;
            }
        }

        User newUser = new User(username, email, pwd1, String.valueOf(this.users.size() + 1));
        this.users.add(newUser);
        Manager.getInstance().getEmailService().sendEmail(email, 
            "asunto", "Bienvenido a ESIUsuarios",
            "texto","Bienvenido al sistema, confirma tu registro aqui: http://localhost:8081/users/confirm?token=" + newUser.getToken());

        return "Le hemos enviado un correo de confirmación a " + email;
    }
}