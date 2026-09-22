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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public final class UserRepository {

    private static final String INSERT = "INSERT INTO users "
            + "(firstname, lastname, date_of_birth, email, password_hash, password_salt) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        private static final String FIND_BY_EMAIL = "SELECT id, firstname, lastname, date_of_birth, email, "
            + "password_hash, password_salt FROM users WHERE email = ?";
    private static final List<User> importedUsers = new ArrayList<>();

    private UserRepository() {
    }

    public static User create(String firstname, String lastname, LocalDate dateOfBirth,
            String email, String password) throws IOException, SQLException, java.security.NoSuchAlgorithmException {
        byte[] salt = PasswordHandler.generateSalt();
        byte[] hash = PasswordHandler.hashPassword(password, salt);
        User user = new User(0, firstname, lastname, dateOfBirth, email,
                Base64.getEncoder().encodeToString(hash),
                Base64.getEncoder().encodeToString(salt));

        Properties configuration = loadConfiguration();
        String url = configuration.getProperty("DB_URL");
        if (url == null || url.isBlank()) {
            importedUsers.add(user);
            return user;
        }

        try (Connection connection = DriverManager.getConnection(url,
                configuration.getProperty("DB_USER"), configuration.getProperty("DB_PASSWORD"));
                PreparedStatement statement = connection.prepareStatement(INSERT,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFirstname());
            statement.setString(2, user.getLastname());
            statement.setObject(3, user.getDateOfBirth());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getPasswordHash());
            statement.setString(6, user.getPasswordSalt());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
        return user;
    }

    public static List<User> findAll() {
        return List.copyOf(importedUsers);
    }

    public static User findByEmail(String email) throws IOException, SQLException {
        Properties configuration = loadConfiguration();
        String url = configuration.getProperty("DB_URL");
        if (url == null || url.isBlank()) {
            return importedUsers.stream()
                    .filter(user -> email.equals(user.getEmail()))
                    .findFirst()
                    .orElse(null);
        }

        try (Connection connection = DriverManager.getConnection(url,
                configuration.getProperty("DB_USER"), configuration.getProperty("DB_PASSWORD"));
                PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("firstname"),
                        resultSet.getString("lastname"),
                        resultSet.getObject("date_of_birth", LocalDate.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("password_salt"));
            }
        }
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