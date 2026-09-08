package ch.bzz;

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

                Runnable command = COMMANDS.get(input.toLowerCase(Locale.ROOT));
                if (command != null) {
                    command.run();
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

    private static void printHelp() {
        System.out.println("Available commands:");
        for (String command : COMMANDS.keySet()) {
            System.out.println("- " + command);
        }
    }
}
