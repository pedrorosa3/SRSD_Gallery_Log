package models;

/*
    * A record to hold the offset of a user in the log file.
    * @param user The user.
    * @param offset The offset.
    */
public record UserOffset(User user, long offset) {}
