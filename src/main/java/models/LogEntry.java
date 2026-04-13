package models;

import Utils.Utils;

public class LogEntry {
        int timestamp;
        String room;
        String userType;
        String userName;
        String action;
        long prevOffset;

        public static LogEntry parse(String line) {
            String[] parts = line.split(",");
            if (parts.length < 6) {
                Utils.exitInvalid();
            }
            LogEntry entry = new LogEntry();
            entry.timestamp = Integer.parseInt(parts[0].trim());
            entry.room = parts[1].trim();
            entry.userType = parts[2].trim();
            entry.userName = parts[3].trim();
            entry.action = parts[4].trim();
            try {
                entry.prevOffset = Long.parseLong(parts[5].trim());
            } catch (NumberFormatException e) {
                Utils.exitInvalid();
            }
            return entry;
        }

        public String getRoom() {
            return room;
        }

        public String getUserType() {
            return userType;
        }

        public String getUserName() {
            return userName;
        }

        public String getAction() {
            return action;
        }

        public long getPrevOffset() {
            return prevOffset;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }
