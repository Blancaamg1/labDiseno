package edu.esi.dls.esiusuarios.http;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.dls.esiusuarios.dao.UserDao;
import edu.esi.dls.esiusuarios.model.User;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserDao userDao;

    @GetMapping("/dashboard")
    public Object adminDashboard() {
        // Funcionalidad real de ADMIN: Listar todos los usuarios registrados
        List<User> users = (List<User>) userDao.findAll();
        
        return users.stream().map(u -> {
            java.util.Map<String, String> userInfo = new java.util.HashMap<>();
            userInfo.put("name", u.getName());
            userInfo.put("email", u.getEmail());
            userInfo.put("role", u.getRole() != null ? u.getRole().name() : "USER");
            return userInfo;
        }).collect(Collectors.toList());
    }
}
