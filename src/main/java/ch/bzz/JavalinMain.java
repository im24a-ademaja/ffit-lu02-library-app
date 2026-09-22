package ch.bzz;

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