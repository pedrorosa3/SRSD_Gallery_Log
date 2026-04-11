package repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;

import crypto.Encryption;
import models.DBCreateEntry;
import models.DBDeleteEntry;
import models.DBUpdateEntry;

public class Database {

    private static final String DB_FILENAME = "log_db.bin";

    public static void CreateEntry(String token, DBCreateEntry entry) throws IOException {
        try (Scanner sc = new Scanner(new File(DB_FILENAME))) {
            String row;

            while (sc.hasNextLine()) {
                row = Encryption.DecryptLog(sc.nextLine(), token);
                String[] fields = row.split(";");
                if (fields[0].equals(entry.username())) {
                    throw new IllegalArgumentException("user " + entry.username() + " already exists");
                }
            }
        }

        String newRow = entry.username() + ";" +
                String.valueOf(entry.room()) + ";" +
                String.valueOf(entry.curr_byte()) + ";" +
                String.valueOf(entry.last_byte());

        Files.writeString(Paths.get(DB_FILENAME), Encryption.EncryptLog(newRow, token, null),
                StandardOpenOption.APPEND);
    }

    public static void UpdateEntry(String token, DBUpdateEntry entry) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).split(";");

            if (row[0].equals(entry.username())) {
                String updatedRow = entry.username() + ";" +
                        entry.room() + ";" +
                        entry.curr_byte() + ";" +
                        row[2];

                lines.set(i, Encryption.EncryptLog(updatedRow, token, null));
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + entry.username() + " doesn't exist");
        }

        Files.write(path, lines);
    }

    public static void DeleteEntry(String token, DBDeleteEntry entry) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).split(";");

            if (row[0].equals(entry.username())) {
                lines.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + entry.username() + " doesn't exist");
        }

        Files.write(path, lines);
    }
}
