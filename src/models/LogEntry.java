package src.models;

    public class LogEntry {
        String room;
        String userType;
        String userName;
        String action;
        long prevOffset;

        public static LogEntry parse(String line) {
            String[] parts = line.split(", ");
            LogEntry entry = new LogEntry();
            entry.room = parts[1].trim();
            entry.userType = parts[2].trim().split(" ")[0];
            entry.userName = parts[2].trim().split(" ")[1];
            entry.action = parts[3].trim();
            entry.prevOffset = Long.parseLong(parts[4].trim());
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
