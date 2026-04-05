package repository;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import models.DBCreateEntry;
import models.DBDeleteEntry;
import models.DBUpdateEntry;

public class Database {

    public static void CreateEntry(String token, DBCreateEntry entry) throws IOException, CsvValidationException {
        // TODO: use token
        try (CSVReader reader = new CSVReader(new FileReader("log_db.csv"))) {
            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row[0].equals(entry.username())) {
                    throw new IllegalArgumentException("user " + entry.username() + " already exists");
                }
            }
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter("log_db.csv", true))) {
            String[] row = { entry.username(), String.valueOf(entry.room()),
                    String.valueOf(entry.curr_line()), String.valueOf(entry.last_line()) };
            writer.writeNext(row);
        }
    }

    public static void UpdateEntry(String token, DBUpdateEntry entry) throws CsvValidationException, IOException {
        // TODO: use token
        String[] row;
        try (CSVReader reader = new CSVReader(new FileReader("log_db.csv"))) {
            while ((row = reader.readNext()) != null) {
                if (row[0].equals(entry.username())) {
                    break;
                }
            }
        }
        if (row == null) {
            throw new IllegalArgumentException("user " + entry.username() + " doesn't exist");
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter("log_db.csv", true))) {
            String[] updatedRow = { entry.username(), String.valueOf(entry.room()),
                    String.valueOf(entry.curr_line()), String.valueOf(row[2]) };
            writer.writeNext(updatedRow);
        }
    }

    public static void DeleteEntry(String token, DBDeleteEntry entry) throws IOException, CsvValidationException {
        // TODO: use token
        try (CSVReader reader = new CSVReader(new FileReader("log_db.csv"));
            CSVWriter writer = new CSVWriter(new FileWriter("temp.csv"))) {
            String[] row;

            while ((row = reader.readNext()) != null) {
                if (row[0].equals(entry.username())) {
                    continue;
                }

                writer.writeNext(row);
            }
        }

        Files.move(
                Paths.get("temp.csv"),
                Paths.get("log_db.csv"),
                StandardCopyOption.REPLACE_EXISTING);
    }
}
