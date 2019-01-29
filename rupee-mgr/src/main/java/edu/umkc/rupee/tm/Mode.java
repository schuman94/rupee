package edu.umkc.rupee.tm;

public enum Mode {

    REGULAR(30,20),
    FAST(2,2),
    OUTPUT(30,20);

    private int dpIterations;
    private int scoreIterations;

    Mode(int dpIterations, int scoreIterations) {
       
        this.dpIterations = dpIterations;
        this.scoreIterations = scoreIterations;
    }

    public int getDpIterations() {
        return dpIterations;
    }

    public int getScoreIterations() {
        return scoreIterations;
    }
}