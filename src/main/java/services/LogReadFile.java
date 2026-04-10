package services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import models.*;
import services.LogReadTerminal.ParsedArgs;

public class LogReadFile {

    public static void process(ParsedArgs parsedArgs) {
        //TODO implement token validation
        if (parsedArgs.isState) {
            LogReadState(parsedArgs.logFilePath);
        } else if (parsedArgs.isRooms) {
            try {
                LogReadRooms(parsedArgs.logFilePath, parsedArgs.users);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (parsedArgs.isIntersect) {
            // LogReadIntersect(parsedArgs.logFilePath, parsedArgs.users);
        }
    }

    private static void LogReadState(String logFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            HashSet<String> employees = new HashSet<>();
            HashSet<String> guests = new HashSet<>();
            HashMap<Integer, TreeSet<String>> roomOccupants = new HashMap<>();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                LogEntry entry = LogEntry.parse(line);

                if (entry.getRoom().equals("GALLERY")) {
                    if (entry.getAction().equals("IN")) {
                        if (entry.getUserType().equals("-E")) employees.add(entry.getUserName());
                        else guests.add(entry.getUserName());
                    } else {
                        if (entry.getUserType().equals("-E")) employees.remove(entry.getUserName());
                        else guests.remove(entry.getUserName());
                    }
                } else {
                    int roomId = Integer.parseInt(entry.getRoom());
                    if (entry.getAction().equals("IN")) {
                        roomOccupants.computeIfAbsent(roomId, k -> new TreeSet<>()).add(entry.getUserName());
                    } else {
                        if (roomOccupants.containsKey(roomId)) {
                            roomOccupants.get(roomId).remove(entry.getUserName());
                            if (roomOccupants.get(roomId).isEmpty())
                                roomOccupants.remove(roomId);
                        }
                    }
                }
            }

            System.out.println(String.join(",", new TreeSet<>(employees)));
            System.out.println(String.join(",", new TreeSet<>(guests)));
            new TreeMap<>(roomOccupants).forEach((roomId, occupants) ->
                System.out.println(roomId + ": " + String.join(",", occupants)));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void LogReadRooms(String logFilePath, List<User> userList) throws IOException {
        User user = userList.get(0);
        List<String> rooms = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(logFilePath, "r")) {
            Map<User, Long> offsets = findLastOffsets(logFilePath, userList);

            if (!offsets.containsKey(user)) {
                System.out.println();
                return;
            }

            long currentOffset = offsets.get(user);

            while (currentOffset != -1) {
                raf.seek(currentOffset);
                String line = raf.readLine();
                System.out.println("Read line: " + line);
                if (line == null) break;

                LogEntry entry = LogEntry.parse(line);

                if (!entry.getRoom().equals("GALLERY") && entry.getAction().equals("IN")) {
                    rooms.add(entry.getRoom());
                }
                currentOffset = entry.getPrevOffset();
            }
        }

        Collections.reverse(rooms);
        System.out.println(String.join(",", rooms));
    }

    private static Map<User, Long> findLastOffsets(String filePath, List<User> users) throws IOException {
        Map<User, Long> offsets = new HashMap<>();
        Set<User> found = new HashSet<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pointer = raf.length() - 1;
            StringBuilder sb = new StringBuilder();

            while (pointer >= 0 && found.size() < users.size()) {
                raf.seek(pointer);
                char c = (char) raf.read();
                pointer--;

                if (c == '\n') {
                    String line = sb.reverse().toString();
                    sb.setLength(0);

                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split(", ");
                        if (parts.length >= 3) {
                            String[] tokens = parts[2].trim().split(" ");
                            if (tokens.length >= 2) {
                                String lineType = tokens[0];
                                String lineName = tokens[1];

                                for (User user : users) {
                                    if (!found.contains(user)
                                            && lineName.equals(user.getUserName())
                                            && lineType.equals(user.getUserType() == User.UserType.EMPLOYEE ? "-E" : "-G")) {
                                        offsets.put(user, pointer + 2);
                                        found.add(user);
                                    }
                                }
                            }
                        }
                    }
                } else if (c != '\r') {
                    sb.append(c);
                }
            }
        }
        return offsets;
    }
}