package ma.ensa.smartcampus.auth;

public class LoginService {
    public boolean authenticate(String username, String password) {
    if (username == null || username.isBlank()
        || password == null || password.length() < 8) {
        return false;
    }
    return "admin".equals(username) && "password123".equals(password); // simulation
}}
