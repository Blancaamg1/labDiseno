package edu.esi.dls.esiusuarios.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.dls.esiusuarios.auxiliares.Manager;
import edu.esi.dls.esiusuarios.dao.UserDao;
import edu.esi.dls.esiusuarios.dto.UserInfoDto;
import edu.esi.dls.esiusuarios.model.User;

@Service
public class UserService {
    private final UserDao repository;
    private final MailtrapRegistrationNotifier mailtrapRegistrationNotifier;

    @Autowired
    public UserService(UserDao repository, MailtrapRegistrationNotifier mailtrapRegistrationNotifier) {
        this.repository = repository;
        this.mailtrapRegistrationNotifier = mailtrapRegistrationNotifier;
        if (repository.count() == 0) {
            repository.save(new User("Pepe", "pepe@example.com", "pepe123", "1234"));
            repository.save(new User("Ana", "ana@example.com", "ana123", "567"));
        }
    }

    public String login(String name, String password) {
        Optional<User> user = repository.findByNameIgnoreCase(name);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get().getToken();
        }
        return null;
    }

    public String checkToken(String token) {
        Optional<User> user = repository.findByToken(token);
        return user.map(User::getName).orElse(null);
    }

    public UserInfoDto getUserInfo(String token) {
        Optional<User> user = repository.findByToken(token);
        return user.map(u -> {
            UserInfoDto dto = new UserInfoDto();
            dto.setId(u.getId());
            dto.setName(u.getName());
            dto.setEmail(u.getEmail());
            return dto;
        }).orElse(null);
    }

    public String register(String username, String email, String pwd1) {
        if (repository.findByNameIgnoreCase(username).isPresent()) {
            return null;
        }

        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            return null;
        }

        User newUser = new User(username, email, pwd1, String.valueOf(repository.count() + 1));
        repository.save(newUser);
        Manager.getInstance().getEmailService().sendEmail(email,
            "asunto", "Bienvenido a ESIUsuarios",
            "texto", "Bienvenido al sistema, confirma tu registro aqui: http://localhost:8081/users/confirm?token=" + newUser.getToken());

        mailtrapRegistrationNotifier.notifyNewRegistration(username, email);

        return "Le hemos enviado un correo de confirmación a " + email;
    }
}