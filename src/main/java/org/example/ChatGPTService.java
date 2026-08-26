package org.example;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatGPTService {

    private static final String API_BASE_URL = "https://shaitest-production-3066.up.railway.app/api-request";
    private static final String API_TOKEN = "PJzOlBMvU4XdkFfuloRJaTCKpZekZASb5JmOCp9rkKCydmxZEfLhrmBP2SuuspcG";

    public List<Question> generateSurveyQuestions(String topic, List<Integer> optionsPerQuestion) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("צור סקר בעברית בנושא: '").append(topic).append("'.\n");
        promptBuilder.append("חובה ליצור בדיוק ").append(optionsPerQuestion.size()).append(" שאלות.\n");

        for (int i = 0; i < optionsPerQuestion.size(); i++) {
            promptBuilder.append("שאלה ").append(i + 1).append(" חייבת להכיל בדיוק ").append(optionsPerQuestion.get(i)).append(" תשובות.\n");
        }

        promptBuilder.append("רשום כל שאלה בשורה המתחילה ב-Q: וכל תשובה בשורה המתחילה ב-A:\n");
        promptBuilder.append("אל תרשום שום טקסט נוסף מעבר לכך.");

        String encodedToken = URLEncoder.encode(API_TOKEN, StandardCharsets.UTF_8);
        String encodedText = URLEncoder.encode(promptBuilder.toString(), StandardCharsets.UTF_8);

        String fullUrl = API_BASE_URL + "?token=" + encodedToken + "&text=" + encodedText;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("הפנייה ל-API נכשלה. קוד סטטוס: " + response.statusCode());
        }

        List<Question> parsedQuestions = parseQuestionsFromResponse(response.body());

        return enforceExactStructure(parsedQuestions, optionsPerQuestion);
    }

    private List<Question> parseQuestionsFromResponse(String responseBody) {
        List<Question> questions = new ArrayList<>();
        String content = responseBody;

        // חילוץ התוכן מתוך שדות JSON אפשריים
        if (content.contains("\"value\":")) {
            int start = content.indexOf("\"value\":") + 8;
            content = content.substring(start);
        } else if (content.contains("\"response\":")) {
            int start = content.indexOf("\"response\":") + 11;
            content = content.substring(start);
        } else if (content.contains("\"result\":")) {
            int start = content.indexOf("\"result\":") + 9;
            content = content.substring(start);
        }

        // ניקוי מעטפות JSON, מטא-דאטה וסוגרים מסולסלים
        content = content.replaceAll("\\{\"error\":.*?\"value\":\"", "");
        content = content.replaceAll("\\{\"[a-zA-Z0-9_]+\"\\}:?", "");
        content = content.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\\"", "\"");

        String[] lines = content.split("\n");
        String currentQ = null;
        List<String> currentOpts = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // הסרת שאריות JSON בקצוות השורה (גרשיים וסוגריים מסולסלים)
            line = line.replaceAll("\"\\}\\s*$", "").replaceAll("^\"|\"$", "").trim();
            if (line.isEmpty()) continue;

            // זיהוי שורת תשובה (A:, A1:, -, •, *)
            boolean isOption = line.matches("(?i)^(A\\d*[:.]?|•|-|\\*)\\s*.*");

            // זיהוי שורת שאלה (Q:, Q1:, שאלה..., מספר בתחילת שורה, או סימן שאלה)
            boolean isQuestion = !isOption && (
                    line.matches("(?i)^(Q\\d*[:.]?|שאלה\\s*\\d*[:.]?|\\d+[.)]).*") || line.contains("?")
            );

            if (isQuestion) {
                if (currentQ != null && !currentOpts.isEmpty()) {
                    questions.add(new Question(currentQ, new ArrayList<>(currentOpts)));
                    currentOpts.clear();
                }
                currentQ = line.replaceAll("(?i)^(Q\\d*[:.]?|שאלה\\s*\\d*[:.]?|\\d+[.)])", "").trim();
            } else if (currentQ != null) {
                String opt = line.replaceAll("(?i)^(A\\d*[:.]?|•|-|\\*|\\d+[.)])", "").trim();
                if (!opt.isEmpty()) {
                    currentOpts.add(opt);
                }
            }
        }

        if (currentQ != null && !currentOpts.isEmpty()) {
            questions.add(new Question(currentQ, new ArrayList<>(currentOpts)));
        }

        return questions;
    }

    private List<Question> enforceExactStructure(List<Question> rawQuestions, List<Integer> optionsPerQuestion) {
        List<Question> finalQuestions = new ArrayList<>();
        int targetQuestionCount = optionsPerQuestion.size();

        for (int i = 0; i < targetQuestionCount; i++) {
            int targetOptionCount = optionsPerQuestion.get(i);
            String title;
            List<String> options = new ArrayList<>();

            if (i < rawQuestions.size()) {
                Question rawQ = rawQuestions.get(i);
                title = rawQ.getText();
                List<String> rawOpts = rawQ.getOptions();

                for (int j = 0; j < Math.min(rawOpts.size(), targetOptionCount); j++) {
                    options.add(rawOpts.get(j));
                }
                while (options.size() < targetOptionCount) {
                    options.add("תשובה נוספת " + (options.size() + 1));
                }
            } else {
                title = "שאלה נוספת בנושא הסקר";
                for (int j = 1; j <= targetOptionCount; j++) {
                    options.add("אפשרות תשובה " + j);
                }
            }

            finalQuestions.add(new Question(title, options));
        }

        return finalQuestions;
    }
}