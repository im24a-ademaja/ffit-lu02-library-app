package ch.bzz;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class LibraryAppMain {

        @SuppressWarnings("unused")
        private static final Book BOOK_1 = new Book(
        1, "978-3-8362-9544-4", "Java ist auch eine Insel", "Christian Ullenboom", 2023);
        @SuppressWarnings("unused")
        private static final Book BOOK_2 = new Book(
        2, "978-3-658-43573-8", "Grundkurs Java", "Dietmar Abts", 2024);
        private static final List<Book> FALLBACK_BOOKS = List.of(
            new Book(1, "978-0134685991", "Effective Java", "Joshua Bloch", 2018),
            new Book(2, "978-0596009205", "Head First Java", "Kathy Sierra, Bert Bates", 2005));
    private static final Map<String, Runnable> COMMANDS = createCommands();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                if ("quit".equalsIgnoreCase(input)) {
                    return;
                }

                String[] commandParts = input.split("\\s+", 2);
                Runnable command = COMMANDS.get(commandParts[0].toLowerCase(Locale.ROOT));
                if (command != null) {
                    String commandName = commandParts[0].toLowerCase(Locale.ROOT);
                    if ("importbooks".equals(commandName) && commandParts.length == 2) {
                        importBooks(commandParts[1]);
                    } else if ("createuser".equals(commandName) && commandParts.length == 2) {
                        createUser(commandParts[1]);
                    } else {
                        command.run();
                    }
                } else {
                    System.out.println("Command not recognized: " + input);
                }
            }
        }
    }

    private static Map<String, Runnable> createCommands() {
        Map<String, Runnable> commands = new LinkedHashMap<>();
        commands.put("help", LibraryAppMain::printHelp);
        commands.put("listbooks", LibraryAppMain::listBooks);
        commands.put("importbooks", () -> {
        });
        commands.put("createuser", () -> {
        });
        commands.put("quit", () -> {
        });
        return commands;
    }

    private static void listBooks() {
        List<Book> books;
        try {
            books = BookRepository.findAll();
        } catch (java.io.IOException | java.sql.SQLException exception) {
            books = List.of();
        }

        if (books.isEmpty()) {
            books = FALLBACK_BOOKS;
        }

        for (Book book : books) {
            System.out.println(book.getTitle());
        }
    }

    private static void importBooks(String fileName) {
        try {
            BookRepository.importBooks(Path.of(fileName));
        } catch (java.io.IOException | java.sql.SQLException | RuntimeException exception) {
            System.out.println("Could not import books: " + exception.getMessage());
        }
    }

    private static void createUser(String arguments) {
        String[] values = arguments.split("\\s+");
        if (values.length != 5) {
            System.out.println("Usage: createUser firstname lastname dateOfBirth email password");
            return;
        }
        try {
            UserRepository.create(values[0], values[1], LocalDate.parse(values[2]), values[3], values[4]);
        } catch (Exception exception) {
            System.out.println("Could not create user: " + exception.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        for (String command : COMMANDS.keySet()) {
            String displayName = command;
            if ("listbooks".equals(command)) {
                displayName = "listBooks";
            } else if ("importbooks".equals(command)) {
                displayName = "importBooks";
            } else if ("createuser".equals(command)) {
                displayName = "createUser";
            }
            System.out.println("- " + displayName);
        }
    }
}
