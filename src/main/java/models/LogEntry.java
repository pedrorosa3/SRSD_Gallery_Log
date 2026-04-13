package models;

import Utils.Utils;

public class LogEntry {
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
            entry.room = parts[1];
            entry.userType = parts[2];
            entry.userName = parts[3];
            entry.action = parts[4];
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
    }
