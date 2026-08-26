package org.example;

import java.util.HashMap;
import java.util.Map;

public class SurveyParticipant {
    private final User user;
    private final Map<Integer, Integer> userAnswers = new HashMap<>(); // questionIndex -> optionIndex
    private boolean hasCompleted;

    public SurveyParticipant(User user) {
        this.user = user;
        this.hasCompleted = false;
    }

    public User getUser() { return user; }
    public int getQuestionsAnswered() { return userAnswers.size(); }
    public boolean isHasCompleted() { return hasCompleted; }

    public boolean hasAnsweredQuestion(int questionIndex) {
        return userAnswers.containsKey(questionIndex);
    }

    public void answerQuestion(int questionIndex, int optionIndex, int totalQuestions) {
        userAnswers.put(questionIndex, optionIndex);
        if (userAnswers.size() >= totalQuestions) {
            this.hasCompleted = true;
        }
    }

    public String getStatusString() {
        if (hasCompleted) return "השלים";
        if (userAnswers.isEmpty()) return "טרם ענה";
        return "בתהליך";
    }
}