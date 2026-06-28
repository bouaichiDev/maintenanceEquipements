package ma.fstg.security.spring_jwt_api.web;

import ma.fstg.security.spring_jwt_api.dto.UserProfileDto;
import ma.fstg.security.spring_jwt_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userService.getProfile(username));
    }
}
