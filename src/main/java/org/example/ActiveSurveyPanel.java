package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ActiveSurveyPanel extends JPanel {

    private final SurveyManager surveyManager;
    private final JLabel statusLabel;
    private final JLabel statsLabel;
    private final JTable participantsTable;
    private final DefaultTableModel tableModel;
    private final JButton btnActionButton;

    public ActiveSurveyPanel(SurveyManager surveyManager) {
        this.surveyManager = surveyManager;

        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- חלק עליון: סטטוס וסטטיסטיקה ---
        JPanel topPanel = new JPanel();
        topPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        statusLabel = new JLabel("אין סקר פעיל כרגע.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        statsLabel = new JLabel("משתתפים: 0 | השלימו: 0 | טרם השלימו: 0", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(statsLabel);

        // --- חלק מרכזי: טבלת משתתפים ---
        String[] columnNames = {"שם המשתמש", "התקדמות", "סטטוס"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        participantsTable = new JTable(tableModel);
        participantsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        participantsTable.setRowHeight(25);
        participantsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(participantsTable);
        scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // --- חלק תחתון: כפתור פעולה דינמי ---
        btnActionButton = new JButton();
        btnActionButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnActionButton.setVisible(false);

        btnActionButton.addActionListener(e -> handleActionButtonClick());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        bottomPanel.add(btnActionButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // טיימר לרענון תצוגת ה-UI בכל שנייה
        Timer timer = new Timer(1000, e -> refreshUI());
        timer.start();
    }

    public void refreshUI() {
        Survey activeSurvey = surveyManager.getCurrentActiveSurvey();

        if (activeSurvey == null || activeSurvey.isFinished()) {
            // מצב 1: אין סקר או שהסקר הסתיים
            statusLabel.setText("אין סקר פעיל כרגע.");
            statusLabel.setForeground(Color.RED);
            statsLabel.setText("משתתפים: 0 | השלימו: 0 | טרם השלימו: 0");
            btnActionButton.setVisible(false);
            tableModel.setRowCount(0);
            return;
        }

        // טעינת רשימת המשתתפים מתוך activeSurvey.getParticipants().values()
        tableModel.setRowCount(0);
        int totalParticipants = activeSurvey.getParticipants().size();
        int completedCount = activeSurvey.getCompletedCount();
        int pendingCount = totalParticipants - completedCount;
        int totalQuestions = activeSurvey.getQuestions().size();

        for (SurveyParticipant participant : activeSurvey.getParticipants().values()) {
            User user = participant.getUser();
            String displayName = (user.getFirstName() != null && !user.getFirstName().isEmpty())
                    ? user.getFirstName()
                    : user.getUsername();

            int answeredCount = participant.getQuestionsAnswered();
            String progressStr = answeredCount + "/" + totalQuestions;
            String statusStr = participant.getStatusString();

            tableModel.addRow(new Object[]{displayName, progressStr, statusStr});
        }

        statsLabel.setText(String.format("משתתפים: %d | השלימו: %d | טרם השלימו: %d",
                totalParticipants, completedCount, pendingCount));

        if (activeSurvey.getSecondsUntilStart() > 0) {
            // מצב 2: סקר מושהה / בהמתנה לתחילתו
            long seconds = activeSurvey.getSecondsUntilStart();
            long minutes = seconds / 60;
            long secs = seconds % 60;

            statusLabel.setText(String.format("הסקר מושהה (ייפתח בעוד %02d:%02d)", minutes, secs));
            statusLabel.setForeground(new Color(204, 102, 0)); // כתום

            btnActionButton.setText("▶️ הפעל את הסקר כעת");
            btnActionButton.setVisible(true);
        } else if (activeSurvey.isActive()) {
            // מצב 3: סקר פעיל - ספירה לאחור מ-getRemainingSeconds()
            long remainingSec = activeSurvey.getRemainingSeconds();
            long minutes = remainingSec / 60;
            long secs = remainingSec % 60;

            statusLabel.setText(String.format("⏳ הסקר פעיל! זמן נותר לסיום: %02d:%02d", minutes, secs));
            statusLabel.setForeground(new Color(180, 0, 0)); // אדום בולט

            btnActionButton.setText("🛑 סגור סקר כעת");
            btnActionButton.setVisible(true);
        }
    }

    private void handleActionButtonClick() {
        Survey currentSurvey = surveyManager.getCurrentActiveSurvey();
        if (currentSurvey == null) return;

        if (currentSurvey.getSecondsUntilStart() > 0) {
            // מעבר ממצב מושהה למצב פעיל מיידי
            surveyManager.forceStartSurveyNow();
            JOptionPane.showMessageDialog(this, "הסקר הועבר למצב פעיל בהצלחה!", "הפעלת סקר", JOptionPane.INFORMATION_MESSAGE);
            refreshUI();
        } else if (currentSurvey.isActive()) {
            // סגירת סקר פעיל
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "האם אתה בטוח שברצונך לסגור את הסקר הפעיל כעת?",
                    "אישור סגירת סקר",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                surveyManager.closeActiveSurvey();
                JOptionPane.showMessageDialog(this, "הסקר נסגר בהצלחה.", "סגירת סקר", JOptionPane.INFORMATION_MESSAGE);
                refreshUI();
            }
        }
    }
}