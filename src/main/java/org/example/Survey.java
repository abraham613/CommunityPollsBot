package org.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Survey {
    private final List<Question> questions;
    private final Map<Long, SurveyParticipant> participants = new HashMap<>();
    private LocalDateTime startTime;
    private LocalDateTime scheduledStartTime;
    private boolean isActive = false;
    private boolean isFinished = false;
    private boolean reminderSent = false;

    public Survey(List<Question> questions, List<User> initialParticipants, int delayMinutes) {
        this.questions = questions;
        for (User user : initialParticipants) {
            this.participants.put(user.getChatId(), new SurveyParticipant(user));
        }
        if (delayMinutes > 0) {
            this.scheduledStartTime = LocalDateTime.now().plusMinutes(delayMinutes);
        } else {
            this.scheduledStartTime = LocalDateTime.now();
        }
    }

    public List<Question> getQuestions() { return questions; }
    public Map<Long, SurveyParticipant> getParticipants() { return participants; }
    public boolean isActive() { return isActive; }
    public boolean isFinished() { return isFinished; }
    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean sent) { this.reminderSent = sent; }

    /**
     * הפעלת הסקר מיידית - מאפסת את השהיית התחלה ומפעילה את טיימר ה-5 דקות מעכשיו
     */
    public void startNow() {
        this.scheduledStartTime = null; // ביטול זמן ההמתנה
        this.startTime = LocalDateTime.now(); // הגדרת שעת ההתחלה לרגע זה
        this.isActive = true;
    }

    public void close() {
        this.isActive = false;
        this.isFinished = true;
    }

    /**
     * מחזירה את השניות שנותרו עד לתחילת הסקר.
     * אם הסקר כבר פעיל או שאין השהיה, מחזירה 0.
     */
    public long getSecondsUntilStart() {
        if (isActive || scheduledStartTime == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), scheduledStartTime).getSeconds();
        return Math.max(0, secs);
    }

    public long getRemainingSeconds() {
        if (startTime == null || !isActive) return 0;
        long elapsed = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        return Math.max(0, (5 * 60) - elapsed);
    }

    public synchronized boolean recordAnswer(long chatId, int questionIndex, int optionIndex) {
        SurveyParticipant participant = participants.get(chatId);
        if (participant != null && isActive && !participant.hasAnsweredQuestion(questionIndex)) {
            participant.answerQuestion(questionIndex, optionIndex, questions.size());
            questions.get(questionIndex).recordVote(optionIndex);
            return true;
        }
        return false;
    }

    public synchronized boolean isCompletedByAll() {
        if (participants.isEmpty()) return false;
        return participants.values().stream().allMatch(SurveyParticipant::isHasCompleted);
    }

    public int getCompletedCount() {
        return (int) participants.values().stream().filter(SurveyParticipant::isHasCompleted).count();
    }
}