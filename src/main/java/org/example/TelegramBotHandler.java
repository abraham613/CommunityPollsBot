package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class TelegramBotHandler extends TelegramLongPollingBot {

    private final CommunityManager communityManager;
    private final SurveyManager surveyManager;

    public TelegramBotHandler(CommunityManager communityManager, SurveyManager surveyManager) {
        this.communityManager = communityManager;
        this.surveyManager = surveyManager;
    }

    @Override
    public String getBotUsername() { return BotConfig.BOT_USERNAME; }

    @Override
    public String getBotToken() { return BotConfig.BOT_TOKEN; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getFrom().getFirstName();
            String username = update.getMessage().getFrom().getUserName();

            if (messageText.equals("/start") || messageText.equals("היי") || messageText.equalsIgnoreCase("Hi")) {
                handleUserJoin(chatId, firstName, username);
            } else {
                sendMessage(chatId, "פקודה לא מוכרת. כדי להצטרף לקהילה שלח 'היי', 'Hi' או לחץ על /start.");
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleUserJoin(long chatId, String firstName, String username) {
        boolean isNewMember = communityManager.addUser(chatId, firstName, username);

        if (isNewMember) {
            int currentSize = communityManager.getCommunitySize();
            sendMessage(chatId, "הצטרפת בהצלחה לקהילה שלנו! 🥳\nכעת אנחנו " + currentSize + " חברים.");
            notifyCommunityAboutNewMember(chatId, firstName, currentSize);
        } else {
            sendMessage(chatId, "אתה כבר חבר בקהילה! ברגע שיתחיל סקר, תקבל עליו הודעה.");
        }
    }

    private void notifyCommunityAboutNewMember(long newMemberChatId, String newMemberName, int currentSize) {
        String notification = "👋 חבר חדש הצטרף לקהילה: " + newMemberName + "!\nגודל הקהילה כעת: " + currentSize + " חברים.";
        for (User user : communityManager.getAllMembers()) {
            if (user.getChatId() != newMemberChatId) {
                sendMessage(user.getChatId(), notification);
            }
        }
    }

    public void broadcastSurveyStart(Survey survey) {
        for (SurveyParticipant participant : survey.getParticipants().values()) {
            sendQuestionToUser(participant.getUser().getChatId(), survey, 0);
        }
    }

    public void sendQuestionToUser(long chatId, Survey survey, int questionIndex) {
        if (questionIndex >= survey.getQuestions().size()) {
            sendMessage(chatId, "🎉 תודה! השלמת את הסקר בהצלחה.");
            return;
        }

        Question q = survey.getQuestions().get(questionIndex);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("שאלה " + (questionIndex + 1) + " מתוך " + survey.getQuestions().size() + ":\n" + q.getText());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < q.getOptions().size(); i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(q.getOptions().get(i));
            button.setCallbackData("VOTE:" + questionIndex + ":" + i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("שגיאה בשליחת שאלה ל-" + chatId + ": " + e.getMessage());
        }
    }

    private void handleCallbackQuery(Update update) {
        // שליחת AnswerCallbackQuery מידית לעצירת הטענת שעון החול על הכפתור בטלגרם
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
        try {
            execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            System.err.println("שגיאה במענה ל-CallbackQuery: " + e.getMessage());
        }

        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String data = update.getCallbackQuery().getData();

        if (data.startsWith("VOTE:")) {
            String[] parts = data.split(":");
            int qIndex = Integer.parseInt(parts[1]);
            int optIndex = Integer.parseInt(parts[2]);

            Survey activeSurvey = surveyManager.getCurrentActiveSurvey();
            if (activeSurvey == null || !activeSurvey.isActive()) {
                sendMessage(chatId, "הסקר כבר אינו פעיל.");
                return;
            }

            surveyManager.recordAnswerFromUser(chatId, qIndex, optIndex);

            // חילוץ נתוני השאלה לבניית תצוגת התשובה הנעולה
            Question question = activeSurvey.getQuestions().get(qIndex);
            List<String> options = question.getOptions();
            int totalQuestions = activeSurvey.getQuestions().size();

            StringBuilder sb = new StringBuilder();
            sb.append("❓ *שאלה ").append(qIndex + 1).append(" מתוך ").append(totalQuestions).append(":\n*");
            sb.append("*").append(question.getText()).append("*\n\n");

            for (int i = 0; i < options.size(); i++) {
                if (i == optIndex) {
                    sb.append("✔️ *").append(options.get(i)).append("*\n");
                } else {
                    sb.append("⚪ ").append(options.get(i)).append("\n");
                }
            }

            // עדכון ההודעה בטלגרם והסרת הכפתורים
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText(sb.toString());
            edit.setParseMode("Markdown");
            edit.setReplyMarkup(null); // הסרת הלחצנים למניעת בחירה חוזרת

            try {
                execute(edit);
            } catch (TelegramApiException e) {
                System.err.println("שגיאה בעדכון הודעה ל-" + chatId + ": " + e.getMessage());
            }

            // שליחת השאלה הבאה בסקר
            sendQuestionToUser(chatId, activeSurvey, qIndex + 1);
        }
    }

    public void sendRemindersToPendingUsers(Survey survey) {
        for (SurveyParticipant participant : survey.getParticipants().values()) {
            if (!participant.isHasCompleted()) {
                sendMessage(participant.getUser().getChatId(), "⏰ תזכורת: נותרו 2 דקות בלבד לסיום הסקר! אנא השלם את תשובותיך.");
            }
        }
    }

    // הודעה על סגירת הסקר ושליחת תוצאות הסקר לכל המשתתפים
    public void notifySurveyClosed(Survey survey) {
        String resultsText = buildSurveyResultsSummary(survey);
        String fullNotification = "🔒 *הסקר נסגר. תודה לכל המשתתפים!*\n\n" + resultsText;

        for (SurveyParticipant participant : survey.getParticipants().values()) {
            sendMessageWithMarkdown(participant.getUser().getChatId(), fullNotification);
        }
    }

    // פונקציית עזר לבניית טקסט תוצאות הסקר (ממוין בסדר יורד מהכי הרבה הצבעות להכי מעט)
    private String buildSurveyResultsSummary(Survey survey) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *תוצאות הסקר הסופיות:*\n\n");

        List<Question> questions = survey.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("❓ *שאלה ").append(i + 1).append(": ").append(q.getText()).append("*\n");

            int totalVotes = q.getTotalVotes();
            List<String> options = q.getOptions();

            // יצירת רשימה לצורך מיון לפי מספר ההצבעות
            List<OptionStat> optionStats = new ArrayList<>();
            for (int j = 0; j < options.size(); j++) {
                optionStats.add(new OptionStat(options.get(j), q.getVotesForOption(j)));
            }

            // מיון בסדר יורד (הכי הרבה הצבעות למעלה)
            optionStats.sort((a, b) -> Integer.compare(b.votes, a.votes));

            for (OptionStat stat : optionStats) {
                double pct = (totalVotes > 0) ? ((double) stat.votes / totalVotes) * 100.0 : 0.0;
                sb.append(String.format("🔹 %s: %d הצבעות (%.1f%%)\n", stat.optionText, stat.votes, pct));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // מחלקת עזר פנימית לייצוג נתוני אפשרות לצורך מיון
    private static class OptionStat {
        final String optionText;
        final int votes;

        OptionStat(String optionText, int votes) {
            this.optionText = optionText;
            this.votes = votes;
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("שגיאה בשליחת הודעה ל-" + chatId + ": " + e.getMessage());
        }
    }

    // שליחת הודעה מעוצבת בפורמט Markdown
    public void sendMessageWithMarkdown(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            // אם ניסיון השליחה עם Markdown נכשל, ננסה לשלוח כטקסט רגיל
            sendMessage(chatId, text.replace("*", ""));
        }
    }
}