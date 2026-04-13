package repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import crypto.Encryption;
import models.*;

public class Database {

    public static void CreateEntry(String dbFilePath, String token, DBEntry entry) throws IOException {
        File file = new File(dbFilePath);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        try (Scanner sc = new Scanner(new File(dbFilePath))) {
            while (sc.hasNextLine()) {
                DecryptedData db_entry = Encryption.decryptDBRow(sc.nextLine(), token);
                if (db_entry == null) throw new IOException("Decrypt Failed");
                String[] fields = db_entry.plaintext().split(",");
                if (fields[0].equals(entry.user().getUserName())) {
                    throw new IllegalArgumentException("user " + entry.user().getUserName() + " already exists");
                }
            }
        }

        String newRow = entry.user().getUserName() + "," +
                entry.user().getUserType() + "," +
                entry.lastAction() + "," +
                String.valueOf(entry.room()) + "," +
                String.valueOf(entry.curr_byte()) + "," +
                String.valueOf(entry.timestamp());

        Files.writeString(Paths.get(dbFilePath), Encryption.encryptDBRow(newRow, token) + System.lineSeparator(),
                StandardOpenOption.APPEND);
    }

    public static void UpdateEntry(String dbFilePath, String token, DBEntry entry) throws IOException {
        Path path = Paths.get(dbFilePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            CreateEntry(dbFilePath, token, entry);
            return;
        }
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            DecryptedData db_entry = Encryption.decryptDBRow(lines.get(i), token);
            if (db_entry == null) throw new IOException("Decrypt Failed");
            String[] row = db_entry.plaintext().split(",");

            if (row[0].equals(entry.user().getUserName())) {
                String updatedRow = entry.user().getUserName() + "," +
                        entry.user().getUserType() + "," +
                        entry.lastAction() + "," +
                        String.valueOf(entry.room()) + "," +
                        String.valueOf(entry.curr_byte()) + "," +
                        String.valueOf(entry.timestamp());

                lines.set(i, Encryption.encryptDBRow(updatedRow, token));
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + entry.user().getUserName() + " doesn't exist");
        }

        Files.write(path, lines.stream()
            .map(l -> l)
            .collect(Collectors.toList()));
    }

    public static void DeleteEntry(String dbFilePath, String token, String user) throws IOException {
        Path path = Paths.get(dbFilePath);
        List<String> lines = Files.readAllLines(path);

        boolean found = false;

        for (int i = 0; i < lines.size(); i++) {
            DecryptedData entry = Encryption.decryptDBRow(lines.get(i), token);
            if (entry == null) throw new IOException("Decrypt Failed");
            String[] row = entry.plaintext().split(",");

            if (row[0].equals(user)) {
                lines.remove(i);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("user " + user + " doesn't exist");
        }

        Files.write(path, lines.stream()
            .map(l -> l + System.lineSeparator())
            .collect(Collectors.toList()));
    }

    public static DBEntry GetEntry(String dbFilePath, String token, String user) throws IOException {
        Path path = Paths.get(dbFilePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return null;
        }
        List<String> lines = Files.readAllLines(path);

        for (int i = 0; i < lines.size(); i++) {
            DecryptedData entry = Encryption.decryptDBRow(lines.get(i), token);
            if (entry == null) throw new IOException("Decrypt Failed");
            String[] row = entry.plaintext().split(",");

            if (row[0].equals(user)) {
                return new DBEntry(new User(row[0], row[1]), row[2], Integer.parseInt(row[3]),
                        Long.parseLong(row[4]),
                        Integer.parseInt(row[5]));
            }
        }

        return null;
    }

    public static long GetLastUserOffset(String dbFilePath, String token, String user) throws IOException {
        Path path = Paths.get(dbFilePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return -1;
        }
        List<String> lines = Files.readAllLines(path);

        for (int i = 0; i < lines.size(); i++) {
            DecryptedData entry = Encryption.decryptDBRow(lines.get(i), token);
            if (entry == null) throw new IOException("Decrypt Failed");
            String[] row = entry.plaintext().split(",");

            if (row[0].equals(user)) {
                return Long.parseLong(row[4]);
            }
        }

        return -1;
    }
}