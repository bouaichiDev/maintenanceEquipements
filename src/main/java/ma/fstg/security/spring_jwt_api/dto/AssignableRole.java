package ma.fstg.security.spring_jwt_api.dto;

public enum AssignableRole {
    TECH,
    USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
