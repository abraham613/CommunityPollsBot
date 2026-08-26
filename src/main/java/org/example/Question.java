package org.example;

import java.util.*;

public class Question {
    private final String text;
    private final List<String> options;
    private final Map<Integer, Integer> optionVoteCounts; // optionIndex -> count

    public Question(String text, List<String> options) {
        this.text = text;
        this.options = options;
        this.optionVoteCounts = new HashMap<>();
        for (int i = 0; i < options.size(); i++) {
            optionVoteCounts.put(i, 0);
        }
    }

    public String getText() { return text; }
    public List<String> getOptions() { return options; }

    public synchronized void recordVote(int optionIndex) {
        if (optionIndex >= 0 && optionIndex < options.size()) {
            optionVoteCounts.put(optionIndex, optionVoteCounts.getOrDefault(optionIndex, 0) + 1);
        }
    }

    public synchronized int getVotesForOption(int optionIndex) {
        return optionVoteCounts.getOrDefault(optionIndex, 0);
    }

    public synchronized int getTotalVotes() {
        return optionVoteCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}