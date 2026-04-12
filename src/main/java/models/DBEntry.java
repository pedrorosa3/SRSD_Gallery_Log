package models;

public record DBEntry(User user, String lastAction, int room, long curr_byte, int timestamp) {}
