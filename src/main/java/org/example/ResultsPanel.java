package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ResultsPanel extends JPanel implements SurveyManager.SurveyStateListener {

    private final SurveyManager surveyManager;
    private final JPanel containerPanel;

    // פלטת צבעים מודרנית לפלחי העוגה
    private static final Color[] SLICE_COLORS = {
            new Color(41, 98, 255),   // כחול
            new Color(229, 57, 53),   // אדום
            new Color(255, 143, 0),   // כתום
            new Color(46, 125, 50),   // ירוק
            new Color(142, 36, 170),  // סגול
            new Color(0, 151, 167),   // טורקיז
            new Color(216, 27, 96),   // ורוד
            new Color(104, 159, 56)   // ירוק בהיר
    };

    public ResultsPanel(SurveyManager surveyManager) {
        this.surveyManager = surveyManager;
        this.surveyManager.addListener(this);

        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // כותרת ראשית
        JLabel headerLabel = new JLabel("📊 תוצאות הסקר (ממוינות בסדר יורד)", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        add(headerLabel, BorderLayout.NORTH);

        // פאנל המכיל את כרטיסי השאלות
        containerPanel = new JPanel();
        containerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JScrollPane scrollPane = new JScrollPane(containerPanel);
        scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        refreshUI();
    }

    public void refreshUI() {
        containerPanel.removeAll();
        Survey activeSurvey = surveyManager.getCurrentActiveSurvey();

        if (activeSurvey == null) {
            showEmptyMessage("אין תוצאות להצגה כרגע.");
            return;
        }

        List<Question> questions = activeSurvey.getQuestions();
        if (questions == null || questions.isEmpty()) {
            showEmptyMessage("אין שאלות בסקר זה.");
            return;
        }

        int questionCount = questions.size();
        containerPanel.setLayout(new GridLayout(questionCount, 1, 0, 15));

        for (int i = 0; i < questionCount; i++) {
            Question question = questions.get(i);
            containerPanel.add(new SingleQuestionResultCard(i + 1, question));
        }

        containerPanel.revalidate();
        containerPanel.repaint();
    }

    private void showEmptyMessage(String message) {
        containerPanel.setLayout(new GridBagLayout());
        JLabel emptyLabel = new JLabel(message);
        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        emptyLabel.setForeground(Color.GRAY);
        containerPanel.add(emptyLabel);
        containerPanel.revalidate();
        containerPanel.repaint();
    }

    @Override
    public void onSurveyUpdated(Survey survey) {
        SwingUtilities.invokeLater(this::refreshUI);
    }

    @Override
    public void onSurveyEnded(Survey survey) {
        SwingUtilities.invokeLater(this::refreshUI);
    }

    // מחלקת עזר לייצוג תוצאת אפשרות תשובה לצורך מיון
    private static class OptionResult {
        final String text;
        final int votes;

        public OptionResult(String text, int votes) {
            this.text = text;
            this.votes = votes;
        }
    }

    // =========================================================================
    // כרטיס תצוגה עבור שאלה בודדת
    // =========================================================================
    private static class SingleQuestionResultCard extends JPanel {

        public SingleQuestionResultCard(int questionNum, Question question) {
            setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(225, 228, 232), 1, true),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)
            ));

            // כותרת השאלה
            JLabel titleLabel = new JLabel("שאלה " + questionNum + ": " + question.getText());
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(titleLabel, BorderLayout.NORTH);

            // מיון האפשרויות לפי כמות ההצבעות בסדר יורד
            List<OptionResult> sortedOptions = getSortedOptions(question);

            // פאנל תוכן (מקרא מימין, גרף משמאל)
            JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            contentPanel.setOpaque(false);
            contentPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            JPanel legendPanel = createLegendPanel(question, sortedOptions);
            PieChartCanvas chartCanvas = new PieChartCanvas(question, sortedOptions);

            contentPanel.add(legendPanel);
            contentPanel.add(chartCanvas);

            add(contentPanel, BorderLayout.CENTER);
        }

        private List<OptionResult> getSortedOptions(Question question) {
            List<String> rawOptions = question.getOptions();
            List<OptionResult> list = new ArrayList<>();
            for (int i = 0; i < rawOptions.size(); i++) {
                list.add(new OptionResult(rawOptions.get(i), question.getVotesForOption(i)));
            }
            // מיון בסדר יורד: הכי הרבה הצבעות בראש הרשימה
            list.sort((a, b) -> Integer.compare(b.votes, a.votes));
            return list;
        }

        private JPanel createLegendPanel(Question question, List<OptionResult> sortedOptions) {
            JPanel legendPanel = new JPanel();
            legendPanel.setOpaque(false);
            legendPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));

            int totalVotes = question.getTotalVotes();
            legendPanel.add(Box.createVerticalGlue());

            for (int i = 0; i < sortedOptions.size(); i++) {
                OptionResult opt = sortedOptions.get(i);
                double pct = (totalVotes > 0) ? ((double) opt.votes / totalVotes) * 100.0 : 0.0;

                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
                itemPanel.setOpaque(false);
                itemPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

                Color color = SLICE_COLORS[i % SLICE_COLORS.length];
                JLabel dotLabel = new JLabel(new ColorDotIcon(color, 14));

                String text = String.format("%s: %d הצבעות (%.1f%%)", opt.text, opt.votes, pct);
                JLabel textLabel = new JLabel(text);
                textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                itemPanel.add(dotLabel);
                itemPanel.add(textLabel);
                legendPanel.add(itemPanel);
            }

            legendPanel.add(Box.createVerticalGlue());
            return legendPanel;
        }
    }

    // =========================================================================
    // רכיב ציור דיאגרמת העוגה (Pie Chart)
    // =========================================================================
    private static class PieChartCanvas extends JPanel {
        private final Question question;
        private final List<OptionResult> sortedOptions;

        public PieChartCanvas(Question question, List<OptionResult> sortedOptions) {
            this.question = question;
            this.sortedOptions = sortedOptions;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int totalVotes = question.getTotalVotes();
            int padding = 10;
            int size = Math.min(getWidth() - padding * 2, getHeight() - padding * 2);
            if (size <= 0) {
                g2.dispose();
                return;
            }

            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            if (totalVotes == 0) {
                g2.setColor(new Color(235, 237, 240));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String msg = "אין הצבעות";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, x + (size - fm.stringWidth(msg)) / 2, y + (size / 2) + 5);
                g2.dispose();
                return;
            }

            double currentAngle = 90.0;
            for (int i = 0; i < sortedOptions.size(); i++) {
                OptionResult opt = sortedOptions.get(i);
                if (opt.votes == 0) continue;

                double sweepAngle = ((double) opt.votes / totalVotes) * 360.0;
                g2.setColor(SLICE_COLORS[i % SLICE_COLORS.length]);
                g2.fillArc(x, y, size, size, (int) Math.round(currentAngle), (int) Math.round(-sweepAngle));

                if (sweepAngle > 15) {
                    double midAngle = Math.toRadians(currentAngle - (sweepAngle / 2.0));
                    int radius = size / 3;
                    int textX = (int) (x + (size / 2.0) + radius * Math.cos(midAngle));
                    int textY = (int) (y + (size / 2.0) - radius * Math.sin(midAngle));

                    String pctStr = String.format("%.1f%%", ((double) opt.votes / totalVotes) * 100.0);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(pctStr, textX - (fm.stringWidth(pctStr) / 2), textY + (fm.getAscent() / 3));
                }

                currentAngle -= sweepAngle;
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // אייקון נקודה צבעונית עבור המקרא
    // =========================================================================
    private static class ColorDotIcon implements Icon {
        private final Color color;
        private final int size;

        public ColorDotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return size; }
        @Override
        public int getIconHeight() { return size; }
    }
}