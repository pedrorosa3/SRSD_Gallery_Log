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
import models.*;

public class Database {

    private static final String DB_FILENAME = "repository/log_db.bin";

    public static void CreateEntry(String token, DBEntry entry) throws IOException {
        File file = new File(DB_FILENAME);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        try (Scanner sc = new Scanner(new File(DB_FILENAME))) {
            String row;

            while (sc.hasNextLine()) {
                row = Encryption.DecryptLog(sc.nextLine(), token).plaintext();
                String[] fields = row.split(",");
                if (fields[0].equals(entry.user().getUserName())) {
                    throw new IllegalArgumentException("user " + entry.user().getUserName() + " already exists");
                }
            }
        }

        String newRow = entry.user().getUserName() + "," +
                entry.user().getUserType() + "," +
                String.valueOf(entry.room()) + "," +
                String.valueOf(entry.curr_byte()) + "," +
                String.valueOf(entry.timestamp());

        Files.writeString(Paths.get(DB_FILENAME), Encryption.EncryptLog(newRow, token, null),
                StandardOpenOption.APPEND);
    }

    public static void UpdateEntry(String token, DBEntry entry) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            CreateEntry(token, entry);
        }
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).plaintext().split(",");

            if (row[0].equals(entry.user().getUserName())) {
                String updatedRow = entry.user().getUserName() + "," +
                        entry.user().getUserType() + "," +
                        String.valueOf(entry.room()) + "," +
                        String.valueOf(entry.curr_byte()) + "," +
                        String.valueOf(entry.timestamp());

                lines.set(i, Encryption.EncryptLog(updatedRow, token, null));
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + entry.user().getUserName() + " doesn't exist");
        }

        Files.write(path, lines);
    }

    public static void DeleteEntry(String token, String user) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).plaintext().split(",");

            if (row[0].equals(user)) {
                lines.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + user + " doesn't exist");
        }

        Files.write(path, lines);
    }

    public static DBEntry GetEntry(String token, String user) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return null;
        }
        List<String> lines = Files.readAllLines(path);

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).plaintext().split(",");

            if (row[0].equals(user)) {
                return new DBEntry(new User(row[0], row[1]), Integer.parseInt(row[2]),
                        Long.parseLong(row[3]),
                        Integer.parseInt(row[4]));
            }
        }

        return null;
    }

    public static int GetLastTimestamp(String token) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return 0;
        }
        List<String> lines = Files.readAllLines(path);
        int lastTimestamp = 0;

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).plaintext().split(",");

            int timestamp = Integer.parseInt(row[4]);
            if (timestamp > lastTimestamp) {
                lastTimestamp = timestamp;
            }
        }

        return lastTimestamp;
    }

    public static long GetLastUserOffset(String token, String user) throws IOException {
        Path path = Paths.get(DB_FILENAME);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return -1;
        }
        List<String> lines = Files.readAllLines(path);

        for (int i = 0; i < lines.size(); i++) {
            String[] row = Encryption.DecryptLog(lines.get(i), token).plaintext().split(",");

            if (row[0].equals(user)) {
                return Long.parseLong(row[3]);
            }
        }

        return -1;
    }
}
