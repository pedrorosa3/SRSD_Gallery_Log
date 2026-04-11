package models;

    public class LogEntry {
        String room;
        String userType;
        String userName;
        String action;
        long prevOffset;

        public static LogEntry parse(String line) {
            String[] parts = line.split(", ");
            if (parts.length < 6) {
                System.out.println("invalid parsing");
                System.exit(111);
            }
            LogEntry entry = new LogEntry();
            entry.room = parts[1].trim();
            entry.userType = parts[2].trim();
            entry.userName = parts[3].trim();
            entry.action = parts[4].trim();
            try {
                entry.prevOffset = Long.parseLong(parts[5].trim());
            } catch (NumberFormatException e) {
                System.out.println("invalid parsing");
                System.exit(111);
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
