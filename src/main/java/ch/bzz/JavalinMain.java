package ch.bzz;

import io.javalin.Javalin;

import java.util.List;

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