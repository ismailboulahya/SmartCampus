package ma.ensa.smartcampus.user;

/** Vue publique d'un utilisateur (jamais de hash de mot de passe). */
public record UserInfo(long id, String email, String firstName, String lastName, Role role) {

    public static UserInfo from(User user) {
        return new UserInfo(user.id(), user.email(), user.firstName(), user.lastName(), user.role());
    }
}
