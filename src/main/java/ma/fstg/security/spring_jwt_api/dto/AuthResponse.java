package ma.fstg.security.spring_jwt_api.dto;

import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private String role;
    private Long organizationId;
    private String organizationName;

    public AuthResponse(String token, String username, String role, Long organizationId, String organizationName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
    }
}
