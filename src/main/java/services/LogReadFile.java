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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import Utils.*;
import crypto.Encryption;
import models.*;

public class LogReadFile {

    /*
    * Process the log file based on the parsed arguments.
    * @param parsedArgs The parsed command-line arguments.
    */
    public static void process(LogReadArgs parsedArgs) {
        try {
            if (parsedArgs.isState) {
                LogReadState(parsedArgs.logFile, parsedArgs.token);
            } else if (parsedArgs.isRooms) {
                LogReadRooms(parsedArgs.logFile, parsedArgs.users, parsedArgs.token);
            } else if (parsedArgs.isIntersect) {
                LogReadIntersect(parsedArgs.logFile, parsedArgs.users, parsedArgs.token);
            }
        } catch (IOException e) {
            System.out.println("invalid");
            System.exit(111);
        }
    }

    /*
     * Process the log file to read the state of the gallery and rooms.
     * @param logFilePath The path to the log file.
     * @param token The encryption token.
     */
    private static void LogReadState(String logFilePath, String token) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            HashSet<String> employees = new HashSet<>();
            HashSet<String> guests = new HashSet<>();
            HashMap<Integer, HashSet<String>> roomOccupants = new HashMap<>();
            DecryptedLog log;

            while ((log = Encryption.DecryptLog(br.readLine(), token)) != null) {
                String line = log.plaintext();
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
                        roomOccupants.computeIfAbsent(roomId, k -> new HashSet<>()).add(entry.getUserName());
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
                System.out.println(roomId + ": " + String.join(",", new TreeSet<>(occupants))));
        }
    }


    /*
    * Process the log file to give the path of a user through the rooms of the gallery.
    * @param logFilePath The path to the log file.
    * @param userList The list of users to query.
    * @param token The encryption token.
    */
    private static void LogReadRooms(String logFilePath, List<User> userList, String token) throws IOException {
        User user = userList.get(0);
        List<String> rooms = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(logFilePath, "r")) {
            Map<User, Long> offsets = findLastOffsets(logFilePath, userList, token);

            if (!offsets.containsKey(user)) {
                System.out.println();
                return;
            }

            long currentOffset = offsets.get(user);

            while (currentOffset != -1) {
                raf.seek(currentOffset);
                String line = raf.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                DecryptedLog log = Encryption.DecryptLog(line, token);
                if (log == null) continue; // skip invalid lines

                LogEntry entry = LogEntry.parse(log.plaintext());

                if (!entry.getRoom().equals("GALLERY") && entry.getAction().equals("IN")) {
                    rooms.add(entry.getRoom());
                }
                currentOffset = entry.getPrevOffset();
            }
        }

        Collections.reverse(rooms);
        System.out.println(String.join(",", rooms));
    }

    /*
     * Find the last offset of each user in the log file.
     * @param filePath The path to the log file.
     * @param users The list of users to query.
     * @param token The encryption token.
     * @return A map of users to their last offsets.
     */
    private static Map<User, Long> findLastOffsets(String filePath, List<User> users, String token) throws IOException {
        Map<User, Long> offsets = new HashMap<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long pointer = raf.length() - 1;
            StringBuilder sb = new StringBuilder();

            while (pointer >= 0 && offsets.size() < users.size()) {
                raf.seek(pointer);
                char c = (char) raf.read();
                pointer--;

                if (c == '\n') {
                    String line = sb.reverse().toString();
                    sb.setLength(0);
                    if (!line.trim().isEmpty()) {
                        DecryptedLog log = Encryption.DecryptLog(line, token);
                        if (log == null) continue; // skip invalid lines
                        LogEntry entry = LogEntry.parse(log.plaintext());
                        for (User user : users) {
                            if (!offsets.containsKey(user)
                                    && entry.getUserName().equals(user.getUserName())
                                    && entry.getUserType().equals(user.getUserType() == User.UserType.EMPLOYEE ? "-E" : "-G")) {
                                offsets.put(user, pointer + 2);
                                break;
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

    /*
     * Process the log file to find the intersection of rooms visited by a list of users.
     * @param logFilePath The path to the log file.
     * @param userList The list of users to query.
     * @param token The encryption token.
     */
    public static void LogReadIntersect(String logFilePath, List<User> userList, String token) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(logFilePath, "r")) {
            Map<User, Long> lastOffsets = findLastOffsets(logFilePath, userList, token);
            if (lastOffsets.size() < userList.size()) return;

            PriorityQueue<UserOffset> orderedOffsets = new PriorityQueue<>(
                (a, b) -> Long.compare(b.offset(), a.offset())
            );
            for (Map.Entry<User, Long> userLastAction : lastOffsets.entrySet()) {
                orderedOffsets.offer(new UserOffset(userLastAction.getKey(), userLastAction.getValue()));
            }

            Set<User> firstActionSeen = new HashSet<>();
            Map<String, Set<User>> roomVisits = new HashMap<>();
            Set<String> result = new HashSet<>();

            while (!orderedOffsets.isEmpty()) {
                UserOffset currentOffset = orderedOffsets.poll();
                raf.seek(currentOffset.offset());
                String line = raf.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                DecryptedLog log = Encryption.DecryptLog(line, token);
                if (log == null) continue; // skip invalid lines

                LogEntry entry = LogEntry.parse(log.plaintext());
                User user = currentOffset.user();

                // if OUT: the user visited the room;
                // if IN && is the first action seen, the user is on the room;
                // if IN && is not the first action seen, the user left the room;

                if (!"GALLERY".equals(entry.getRoom())) {
                    if ("OUT".equals(entry.getAction())) {
                        roomVisits.computeIfAbsent(entry.getRoom(), k -> new HashSet<>()).add(user);
                    } else if ("IN".equals(entry.getAction())) {
                        if (!firstActionSeen.contains(user)) {
                            roomVisits.computeIfAbsent(entry.getRoom(), k -> new HashSet<>()).add(user);
                        } else {
                            roomVisits.computeIfAbsent(entry.getRoom(), k -> new HashSet<>()).remove(user);
                        }
                    }

                    if (roomVisits.containsKey(entry.getRoom()) && roomVisits.get(entry.getRoom()).containsAll(userList)) {
                        result.add(entry.getRoom());
                    }
                }

                firstActionSeen.add(user);

                if (entry.getPrevOffset() != -1) {
                    orderedOffsets.offer(new UserOffset(user, entry.getPrevOffset()));
                }
            }

            String output = result.stream()
                .map(Integer::parseInt)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

            if (!output.isEmpty()) System.out.println(output);
        }
    }
}