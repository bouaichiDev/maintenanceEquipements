package ma.fstg.security.spring_jwt_api.web;


import jakarta.validation.Valid;
import ma.fstg.security.spring_jwt_api.dto.AuthResponse;
import ma.fstg.security.spring_jwt_api.dto.LoginRequest;
import ma.fstg.security.spring_jwt_api.dto.RegisterRequest;
import ma.fstg.security.spring_jwt_api.services.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
