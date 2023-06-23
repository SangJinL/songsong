package com.example.songsong;

public class Song {
    private int number;
    private String song;
    private String singer;
    private String filename;
    private String hint1;
    private String hint2;

    public Song(int number, String song, String singer, String filename, String hint1, String hint2) {
        this.number = number;
        this.song = song;
        this.singer = singer;
        this.filename = filename;
        this.hint1 = hint1;
        this.hint2 = hint2;
    }

    public int getNumber() {
        return number;
    }

    public String getSong() {
        return song;
    }

    public String getSinger() {
        return singer;
    }

    public String getHint1() {
        return hint1;
    }

    public String getHint2() {
        return hint2;
    }

    public String fileName() {
        // split the fileName by dot and return the first part (i.e., the name without extension)
        return filename.split("\\.")[0];
    }
}