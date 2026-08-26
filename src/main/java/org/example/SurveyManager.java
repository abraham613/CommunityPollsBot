package org.example;

import javax.swing.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class SurveyManager {
    public interface SurveyStateListener {
        void onSurveyUpdated(Survey survey);
        void onSurveyEnded(Survey survey);
    }

    private final CommunityManager communityManager;
    private final List<SurveyStateListener> listeners = new CopyOnWriteArrayList<>();
    private Survey currentActiveSurvey;
    private Timer surveyTimer;
    private TelegramBotHandler botHandler;
    private boolean isScheduledStart = false;

    public SurveyManager(CommunityManager communityManager) {
        this.communityManager = communityManager;
    }

    public void setBotHandler(TelegramBotHandler botHandler) {
        this.botHandler = botHandler;
    }

    public void addListener(SurveyStateListener listener) {
        listeners.add(listener);
    }

    public synchronized boolean startNewSurvey(List<Question> questions, int delayMinutes) {
        if (currentActiveSurvey != null && (currentActiveSurvey.isActive() || currentActiveSurvey.getSecondsUntilStart() > 0)) {
            return false;
        }
        if (communityManager.getCommunitySize() < 3) {
            return false;
        }

        List<User> participants = communityManager.getAllMembers();
        currentActiveSurvey = new Survey(questions, participants, delayMinutes);

        if (delayMinutes > 0) {
            isScheduledStart = true;
            startScheduledTimer(delayMinutes);
        } else {
            isScheduledStart = false;
            executeSurveyStart();
        }
        notifyStateChanged();
        return true;
    }

    private void startScheduledTimer(int delayMinutes) {
        surveyTimer = new Timer(true);
        surveyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                executeSurveyStart();
            }
        }, (long) delayMinutes * 60 * 1000);
    }

    /**
     * הפעלה מיידית של סקר מושהה/מתוזמן בלחיצה על כפתור התפעול
     */
    public synchronized void forceStartSurveyNow() {
        if (currentActiveSurvey != null && currentActiveSurvey.getSecondsUntilStart() > 0) {
            if (surveyTimer != null) {
                surveyTimer.cancel();
            }
            isScheduledStart = false;
            executeSurveyStart();
        }
    }

    private synchronized void executeSurveyStart() {
        if (currentActiveSurvey == null) return;
        currentActiveSurvey.startNow();

        if (botHandler != null) {
            botHandler.broadcastSurveyStart(currentActiveSurvey);
        }

        if (isScheduledStart) {
            isScheduledStart = false;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "🚀 זמן ההשהיה הסתיים! הסקר הופעל ונשלח בהצלחה לכל חברי הקהילה בטלגרם.",
                        "הודעת מערכת - הסקר המושהה הופעל",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        }

        if (surveyTimer != null) surveyTimer.cancel();
        surveyTimer = new Timer(true);

        // תזכורת כעבור 3 דקות
        surveyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAndSendReminders();
            }
        }, 3 * 60 * 1000);

        // סגירה כעבור 5 דקות
        surveyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeActiveSurvey();
            }
        }, 5 * 60 * 1000);

        notifyStateChanged();
    }

    private synchronized void checkAndSendReminders() {
        if (currentActiveSurvey != null && currentActiveSurvey.isActive() && !currentActiveSurvey.isReminderSent()) {
            currentActiveSurvey.setReminderSent(true);
            if (botHandler != null) {
                botHandler.sendRemindersToPendingUsers(currentActiveSurvey);
            }
        }
    }

    public synchronized void recordAnswerFromUser(long chatId, int questionIndex, int optionIndex) {
        if (currentActiveSurvey != null && currentActiveSurvey.isActive()) {
            boolean recorded = currentActiveSurvey.recordAnswer(chatId, questionIndex, optionIndex);
            if (recorded) {
                notifyStateChanged();
                if (currentActiveSurvey.isCompletedByAll()) {
                    closeActiveSurvey();
                }
            }
        }
    }

    public synchronized void closeActiveSurvey() {
        if (currentActiveSurvey != null && currentActiveSurvey.isActive()) {
            currentActiveSurvey.close();
            if (surveyTimer != null) {
                surveyTimer.cancel();
            }
            if (botHandler != null) {
                botHandler.notifySurveyClosed(currentActiveSurvey);
            }
            for (SurveyStateListener listener : listeners) {
                listener.onSurveyEnded(currentActiveSurvey);
            }
            notifyStateChanged();
        }
    }

    public void notifyStateChanged() {
        for (SurveyStateListener listener : listeners) {
            listener.onSurveyUpdated(currentActiveSurvey);
        }
    }

    public Survey getCurrentActiveSurvey() { return currentActiveSurvey; }
}