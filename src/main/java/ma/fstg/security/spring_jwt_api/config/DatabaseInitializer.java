package ma.fstg.security.spring_jwt_api.config;


import ma.fstg.security.spring_jwt_api.entities.Organization;
import ma.fstg.security.spring_jwt_api.entities.Role;
import ma.fstg.security.spring_jwt_api.entities.User;
import ma.fstg.security.spring_jwt_api.repositories.OrganizationRepository;
import ma.fstg.security.spring_jwt_api.repositories.RoleRepository;
import ma.fstg.security.spring_jwt_api.repositories.UserRepository;
import ma.fstg.security.spring_jwt_api.security.CurrentUserProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseInitializer {

    @Bean
    CommandLineRunner init(RoleRepository roleRepo, UserRepository userRepo,
                           OrganizationRepository orgRepo, BCryptPasswordEncoder encoder) {
        return args -> {
            Role superRole = getOrCreate(roleRepo, CurrentUserProvider.ROLE_SUPER_ADMIN);
            Role adminRole = getOrCreate(roleRepo, CurrentUserProvider.ROLE_ADMIN);
            Role techRole = getOrCreate(roleRepo, CurrentUserProvider.ROLE_TECH);
            Role userRole = getOrCreate(roleRepo, CurrentUserProvider.ROLE_USER);

            if (userRepo.existsByUsername("superadmin")) {
                return;
            }

            User superAdmin = new User(null, "superadmin", encoder.encode("admin123"), true, null, List.of(superRole));
            userRepo.save(superAdmin);

            Organization demo = new Organization();
            demo.setName("ENS Marrakech");
            demo.setActive(true);
            demo = orgRepo.save(demo);

            User admin = new User(null, "admin", encoder.encode("1234"), true, demo, List.of(adminRole));
            User tech = new User(null, "tech", encoder.encode("1234"), true, demo, List.of(techRole));
            User user = new User(null, "user", encoder.encode("1111"), true, demo, List.of(userRole));
            userRepo.saveAll(List.of(admin, tech, user));
        };
    }

    private Role getOrCreate(RoleRepository roleRepo, String name) {
        Role role = roleRepo.findByName(name);
        return role != null ? role : roleRepo.save(new Role(null, name));
    }
}
