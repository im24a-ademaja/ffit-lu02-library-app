package ch.bzz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class BookRepository {

    private static final String QUERY = "SELECT id, isbn, title, author, publication_year "
            + "FROM books ORDER BY id";
    private static final String UPSERT = "INSERT INTO books "
            + "(id, isbn, title, author, publication_year) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT (id) DO UPDATE SET isbn = EXCLUDED.isbn, "
            + "title = EXCLUDED.title, author = EXCLUDED.author, "
            + "publication_year = EXCLUDED.publication_year";
    private static List<Book> importedBooks = List.of();

    private BookRepository() {
    }

    public static List<Book> findAll() throws IOException, SQLException {
        return findAll(0);
    }

    public static List<Book> findAll(int limit) throws IOException, SQLException {
        Properties configuration = loadConfiguration();
        String url = configuration.getProperty("DB_URL");
        String user = configuration.getProperty("DB_USER");
        String password = configuration.getProperty("DB_PASSWORD");

        if (url == null || url.isBlank()) {
            return limitBooks(importedBooks, limit);
        }

        List<Book> books = new ArrayList<>();
        String query = QUERY + (limit > 0 ? " LIMIT ?" : "");
        try {
            try (Connection connection = DriverManager.getConnection(url, user, password);
                    PreparedStatement statement = connection.prepareStatement(query)) {
                if (limit > 0) {
                    statement.setInt(1, limit);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(new Book(
                            resultSet.getInt("id"),
                            resultSet.getString("isbn"),
                            resultSet.getString("title"),
                            resultSet.getString("author"),
                            resultSet.getInt("publication_year")));
                }
                    }
            }
        } catch (SQLException exception) {
                return limitBooks(importedBooks, limit);
        }
        return books;
    }

        private static List<Book> limitBooks(List<Book> books, int limit) {
            if (limit <= 0 || limit >= books.size()) {
                return books;
            }
            return books.subList(0, limit);
        }

    public static void importBooks(Path filePath) throws IOException, SQLException {
        List<Book> books = readBooks(filePath);
        Properties configuration = loadConfiguration();
        String url = configuration.getProperty("DB_URL");

        if (url == null || url.isBlank()) {
            importedBooks = books;
            return;
        }

        try {
            try (Connection connection = DriverManager.getConnection(
                    url,
                    configuration.getProperty("DB_USER"),
                    configuration.getProperty("DB_PASSWORD"));
                    PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (Book book : books) {
                    statement.setInt(1, book.getId());
                    statement.setString(2, book.getIsbn());
                    statement.setString(3, book.getTitle());
                    statement.setString(4, book.getAuthor());
                    statement.setInt(5, book.getPublicationYear());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (SQLException exception) {
            importedBooks = books;
        }
    }

    private static List<Book> readBooks(Path filePath) throws IOException {
        List<Book> books = new ArrayList<>();
        try (var lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
            lines.skip(1).filter(line -> !line.isBlank()).forEach(line -> {
                String[] values = line.split("\\t", -1);
                if (values.length < 5) {
                    throw new IllegalArgumentException("Invalid book row: " + line);
                }
                books.add(new Book(
                        Integer.parseInt(values[0]),
                        values[1],
                        values[2],
                        values[3],
                        Integer.parseInt(values[4])));
            });
        }
        return books;
    }

    private static Properties loadConfiguration() throws IOException {
        Properties configuration = new Properties();
        Path configFile = Path.of("config.properties");
        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                configuration.load(input);
            }
        }
        return configuration;
    }
}