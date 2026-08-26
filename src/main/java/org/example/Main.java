package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        CommunityManager communityManager = new CommunityManager();
        SurveyManager surveyManager = new SurveyManager(communityManager);
        TelegramBotHandler botHandler = new TelegramBotHandler(communityManager, surveyManager);

        surveyManager.setBotHandler(botHandler);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(botHandler);
            System.out.println("🤖 בוט הטלגרם הופעל בהצלחה!");
        } catch (Exception e) {
            System.err.println("שגיאה בהפעלת הבוט מול טלגרם: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(communityManager, surveyManager, botHandler);
            mainFrame.setVisible(true);
        });
    }
}