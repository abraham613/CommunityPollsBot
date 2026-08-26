package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CreateSurveyPanel extends JPanel {

    private final CommunityManager communityManager;
    private final SurveyManager surveyManager;
    private final ChatGPTService chatGPTService;
    private final MainFrame mainFrame;

    private final JComboBox<String> questionsCountCombo;
    private final JPanel dynamicQuestionsPanel;
    private final List<JComboBox<String>> answerCountCombos = new ArrayList<>();

    public CreateSurveyPanel(CommunityManager communityManager, SurveyManager surveyManager, ChatGPTService chatGPTService, MainFrame mainFrame) {
        this.communityManager = communityManager;
        this.surveyManager = surveyManager;
        this.chatGPTService = chatGPTService;
        this.mainFrame = mainFrame;

        // הגדרת כיוון מימין לשמאל לפאנל הראשי
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("📝 הגדרת סקר חדש", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        // --- חלק עליון: הגדרת מבנה הסקר ---
        JPanel topConfigPanel = new JPanel();
        topConfigPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        topConfigPanel.setLayout(new BoxLayout(topConfigPanel, BoxLayout.Y_AXIS));
        topConfigPanel.setBorder(BorderFactory.createTitledBorder("1. הגדרת כמות שאלות ותשובות"));

        JPanel qCountRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        qCountRow.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        qCountRow.add(new JLabel("בחר כמות שאלות בסקר:"));
        questionsCountCombo = new JComboBox<>(new String[]{"- בחר -", "1", "2", "3"});
        qCountRow.add(questionsCountCombo);

        dynamicQuestionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        dynamicQuestionsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        topConfigPanel.add(qCountRow);
        topConfigPanel.add(dynamicQuestionsPanel);

        // מאזין לשינוי כמות השאלות
        questionsCountCombo.addActionListener(e -> updateDynamicQuestionBoxes());

        // --- חלק מרכזי: כפתורי פעולה ---
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        centerPanel.setBorder(BorderFactory.createTitledBorder("2. בחירת אופן יצירת הסקר"));

        JButton btnManual = new JButton("✍️ יצירת סקר ידנית");
        JButton btnAI = new JButton("✨ יצירת סקר מהירה באמצעות AI");

        btnManual.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnAI.setFont(new Font("Segoe UI", Font.BOLD, 15));

        btnManual.addActionListener(e -> createManualSurvey());
        btnAI.addActionListener(e -> createAISurvey());

        centerPanel.add(new JLabel("לאחר הגדרת המבנה למעלה, בחר את דרך היצירה:", SwingConstants.CENTER));
        centerPanel.add(btnManual);
        centerPanel.add(btnAI);

        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mainContainer.add(topConfigPanel, BorderLayout.NORTH);
        mainContainer.add(centerPanel, BorderLayout.CENTER);

        add(titleLabel, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);
    }

    private void updateDynamicQuestionBoxes() {
        dynamicQuestionsPanel.removeAll();
        answerCountCombos.clear();

        int selectedIndex = questionsCountCombo.getSelectedIndex();
        if (selectedIndex > 0) {
            int qCount = selectedIndex; // 1, 2 או 3
            for (int i = 1; i <= qCount; i++) {
                JPanel box = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                box.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                box.setBorder(BorderFactory.createEtchedBorder());

                JLabel label = new JLabel("שאלה " + i + " (תשובות):");
                JComboBox<String> aCombo = new JComboBox<>(new String[]{"- בחר -", "2", "3", "4"});
                answerCountCombos.add(aCombo);

                box.add(label);
                box.add(aCombo);

                dynamicQuestionsPanel.add(box);
            }
        }

        dynamicQuestionsPanel.revalidate();
        dynamicQuestionsPanel.repaint();
    }

    private List<Integer> getSelectedStructure() {
        if (questionsCountCombo.getSelectedIndex() <= 0) return null;
        List<Integer> structure = new ArrayList<>();
        for (JComboBox<String> combo : answerCountCombos) {
            if (combo.getSelectedIndex() <= 0) {
                return null; // קיים שדה שלא נבחר
            }
            structure.add(combo.getSelectedIndex() + 1); // index 1 -> 2 options, 2 -> 3, 3 -> 4
        }
        return structure;
    }

    private boolean validateCanStartSurvey() {
        Survey active = surveyManager.getCurrentActiveSurvey();
        if (active != null && (active.isActive() || active.getSecondsUntilStart() > 0)) {
            JOptionPane.showMessageDialog(this,
                    "שגיאה: כבר קיים סקר פעיל או מושהה ברקע. יש להמתין לסיומו.",
                    "שגיאה", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (communityManager.getCommunitySize() < 3) {
            JOptionPane.showMessageDialog(this,
                    "לא ניתן ליצור סקר! נדרשים לפחות 3 חברים בקהילה (כרגע רשומים: " + communityManager.getCommunitySize() + ").",
                    "שגיאה - קהילה קטנה מדי", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        List<Integer> structure = getSelectedStructure();
        if (structure == null) {
            JOptionPane.showMessageDialog(this,
                    "שגיאה: עדיין לא בחרת כמות שאלות ותשובות תקינה בחלק העליון!",
                    "קלט חסר", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private void createManualSurvey() {
        if (!validateCanStartSurvey()) return;

        List<Integer> structure = getSelectedStructure();
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "הזנת תוכן הסקר הידני", true);
        dialog.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel();
        formPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<JTextField> questionFields = new ArrayList<>();
        List<List<JTextField>> optionFieldsGroup = new ArrayList<>();

        for (int i = 0; i < structure.size(); i++) {
            int optionCount = structure.get(i);
            JPanel qBox = new JPanel();
            qBox.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            qBox.setLayout(new BoxLayout(qBox, BoxLayout.Y_AXIS));
            qBox.setBorder(BorderFactory.createTitledBorder("שאלה " + (i + 1)));

            JTextField qField = new JTextField();
            qField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            questionFields.add(qField);
            qBox.add(new JLabel("נוסח השאלה:"));
            qBox.add(qField);

            List<JTextField> currentOptFields = new ArrayList<>();
            for (int j = 0; j < optionCount; j++) {
                JTextField optField = new JTextField();
                optField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                currentOptFields.add(optField);
                qBox.add(new JLabel("  • תשובה " + (j + 1) + ":"));
                qBox.add(optField);
            }
            optionFieldsGroup.add(currentOptFields);
            formPanel.add(qBox);
            formPanel.add(Box.createVerticalStrut(10));
        }

        JButton btnSubmit = new JButton("אישור והמשך לתזמון");
        btnSubmit.addActionListener(e -> {
            List<Question> questions = new ArrayList<>();
            for (int i = 0; i < questionFields.size(); i++) {
                String qText = questionFields.get(i).getText().trim();
                if (qText.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "יש למלא את כל נוסחי השאלות!", "שגיאה", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                List<String> opts = new ArrayList<>();
                for (JTextField optField : optionFieldsGroup.get(i)) {
                    String optText = optField.getText().trim();
                    if (optText.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "יש למלא את כל אפשרויות התשובה!", "שגיאה", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    opts.add(optText);
                }
                questions.add(new Question(qText, opts));
            }
            dialog.dispose();
            promptAndStartSurvey(questions);
        });

        dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        dialog.add(btnSubmit, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void createAISurvey() {
        if (!validateCanStartSurvey()) return;

        List<Integer> structure = getSelectedStructure();
        String topic = JOptionPane.showInputDialog(this, "הכנס נושא לסקר (למשל: ספורט, תכנות):");
        if (topic != null && !topic.trim().isEmpty()) {
            try {
                List<Question> questions = chatGPTService.generateSurveyQuestions(topic.trim(), structure);

                StringBuilder preview = new StringBuilder("התקבלו השאלות הבאות מ-ChatGPT (לפי המבנה שהגדרת):\n\n");
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    preview.append("שאלה ").append(i + 1).append(": ").append(q.getText()).append("\n");
                    for (String opt : q.getOptions()) {
                        preview.append("   • ").append(opt).append("\n");
                    }
                    preview.append("\n");
                }
                preview.append("האם אתה בטוח שברצונך להעלות ולשלוח סקר זה?");

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        preview.toString(),
                        "תצוגה מקדימה לסקר AI",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    promptAndStartSurvey(questions);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "שגיאה ביצירת סקר מול API: " + e.getMessage(), "שגיאה", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void promptAndStartSurvey(List<Question> questions) {
        int delayMinutes = 0;
        while (true) {
            String delayStr = JOptionPane.showInputDialog(this, "הזן עיכוב בשליחה בדקות (0 לשליחה מיידית):", "0");
            if (delayStr == null) return;

            try {
                delayMinutes = Integer.parseInt(delayStr.trim());
                if (delayMinutes >= 0) break;
                JOptionPane.showMessageDialog(this, "מספר הדקות חייב להיות 0 או מספר חיובי.", "קלט לא תקין", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "אנא הזן מספר שלם תקין.", "קלט לא תקין", JOptionPane.ERROR_MESSAGE);
            }
        }

        boolean success = surveyManager.startNewSurvey(questions, delayMinutes);

        if (success) {
            JOptionPane.showMessageDialog(this, "הסקר הוגדר בהצלחה!");
            mainFrame.switchToTab(2);
        } else {
            JOptionPane.showMessageDialog(this, "שגיאה בתחילת הסקר. ייתכן שקיים סקר פעיל או מושהה ברקע.");
        }
    }
}