package org.example;

import javax.swing.*;

public class MainFrame extends JFrame {
    private final JTabbedPane tabbedPane;

    public MainFrame(CommunityManager communityManager, SurveyManager surveyManager, TelegramBotHandler botHandler) {
        setTitle("מערכת ניהול סקרים קהילתית - Telegram Bot");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // חיבור ה-BotHandler ל-SurveyManager
        if (botHandler != null) {
            surveyManager.setBotHandler(botHandler);
        }

        tabbedPane = new JTabbedPane();

        CommunityPanel communityPanel = new CommunityPanel(communityManager);
        CreateSurveyPanel createSurveyPanel = new CreateSurveyPanel(communityManager, surveyManager, new ChatGPTService(), this);
        ActiveSurveyPanel activeSurveyPanel = new ActiveSurveyPanel(surveyManager);
        ResultsPanel resultsPanel = new ResultsPanel(surveyManager);

        tabbedPane.addTab("👥 קהילה", communityPanel);
        tabbedPane.addTab("📝 יצירת סקר", createSurveyPanel);
        tabbedPane.addTab("⏳ סקר פעיל", activeSurveyPanel);
        tabbedPane.addTab("📊 תוצאות", resultsPanel);

        // מאזין לאירועי סקר: ברגע שסקר מסתיים, מעביר אוטומטית ללשונית התוצאות (אינדקס 3)
        surveyManager.addListener(new SurveyManager.SurveyStateListener() {
            @Override
            public void onSurveyUpdated(Survey survey) {
                // אין צורך בפעולה כאן
            }

            @Override
            public void onSurveyEnded(Survey survey) {
                SwingUtilities.invokeLater(() -> switchToTab(3));
            }
        });

        add(tabbedPane);
    }

    public void switchToTab(int tabIndex) {
        tabbedPane.setSelectedIndex(tabIndex);
    }
}