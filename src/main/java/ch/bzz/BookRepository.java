package ch.bzz;

import java.io.IOException;
import java.io.InputStream;
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

    private BookRepository() {
    }

    public static List<Book> findAll() throws IOException, SQLException {
        Properties configuration = loadConfiguration();
        String url = configuration.getProperty("DB_URL");
        String user = configuration.getProperty("DB_USER");
        String password = configuration.getProperty("DB_PASSWORD");

        if (url == null || url.isBlank()) {
            return List.of();
        }

        List<Book> books = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
                PreparedStatement statement = connection.prepareStatement(QUERY);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                books.add(new Book(
                        resultSet.getInt("id"),
                        resultSet.getString("isbn"),
                        resultSet.getString("title"),
                        resultSet.getString("author"),
                        resultSet.getInt("publication_year")));
            }
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