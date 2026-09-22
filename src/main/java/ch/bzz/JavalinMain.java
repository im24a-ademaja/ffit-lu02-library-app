package ch.bzz;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import ch.bzz.security.JwtHandler;
import io.javalin.Javalin;

public final class JavalinMain {

    private static final int PORT = 7070;

    private JavalinMain() {
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create()
                .get("/books", context -> {
                    int limit = parseLimit(context.queryParam("limit"));
                    List<Book> books = BookRepository.findAll(limit);
                    context.json(books);
            })
            .post("/auth/login", context -> {
                try {
                    Map<?, ?> json = context.bodyValidator(Map.class)
                        .check(body -> body.containsKey("email"), "email is required")
                        .check(body -> body.containsKey("password"), "password is required")
                        .get();

                    String inputEmail = (String) json.get("email");
                    String inputPassword = (String) json.get("password");
                    User user = UserRepository.findByEmail(inputEmail);

                    if (user != null
                        && PasswordHandler.verifyPassword(
                            inputPassword,
                            Base64.getDecoder().decode(user.getPasswordHash()),
                            Base64.getDecoder().decode(user.getPasswordSalt()))) {
                        String jwt = JwtHandler.createJwt(inputEmail, user.getId());
                        context.json(Map.of("token", jwt));
                        return;
                    }

                    context.status(401).json(Map.of("error", "Invalid email or password"));
                } catch (Exception exception) {
                    context.status(401).json(Map.of("error", "Invalid email or password"));
                }
            })
            .put("/auth/change-password", context -> {
                String authHeader = context.header("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    context.status(401).json(Map.of("error", "Authorization required"));
                    return;
                }

                User user;
                try {
                    Integer userId = JwtHandler.getUserId(authHeader.substring("Bearer ".length()));
                    user = UserRepository.findById(userId);
                } catch (RuntimeException | IOException | SQLException exception) {
                    context.status(401).json(Map.of("error", "Invalid authorization"));
                    return;
                }
                if (user == null) {
                    context.status(401).json(Map.of("error", "Invalid authorization"));
                    return;
                }

                Map<?, ?> json = context.bodyValidator(Map.class)
                        .check(body -> body.containsKey("oldPassword"), "oldPassword is required")
                        .check(body -> body.containsKey("newPassword"), "newPassword is required")
                        .get();
                String oldPassword = (String) json.get("oldPassword");
                String newPassword = (String) json.get("newPassword");

                try {
                    if (!PasswordHandler.verifyPassword(
                            oldPassword,
                            Base64.getDecoder().decode(user.getPasswordHash()),
                            Base64.getDecoder().decode(user.getPasswordSalt()))) {
                        context.status(401).json(Map.of("error", "Invalid old password"));
                        return;
                    }

                    byte[] newHash = PasswordHandler.hashPassword(
                            newPassword,
                            Base64.getDecoder().decode(user.getPasswordSalt()));
                    user.setPasswordHash(Base64.getEncoder().encodeToString(newHash));
                    UserRepository.updatePassword(user);
                    context.json(Map.of("message", "Password changed successfully"));
                } catch (java.security.NoSuchAlgorithmException | IOException | SQLException exception) {
                    context.status(500).json(Map.of("error", "Could not change password"));
                }
            });
        app.start(PORT);
    }

    private static int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            int limit = Integer.parseInt(value);
            if (limit < 0) {
                throw new IllegalArgumentException("limit must not be negative");
            }
            return limit;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("limit must be a number", exception);
        }
    }
}