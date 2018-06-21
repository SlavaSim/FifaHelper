package ru.slavasim.fifa.model;

public class AvScore {
    public String matchId;
    public int category;
    public boolean available;
    public int score;

    public AvScore(String matchId, int category, boolean available) {
        this.matchId = matchId;
        this.category = category;
        this.available = available;
    }
}
