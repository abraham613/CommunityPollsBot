package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CommunityPanel extends JPanel implements CommunityManager.CommunityChangeListener {
    private final CommunityManager communityManager;
    private final DefaultTableModel tableModel;
    private final JLabel totalMembersLabel;

    public CommunityPanel(CommunityManager communityManager) {
        this.communityManager = communityManager;
        this.communityManager.addListener(this);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // פאנל עליון ראשי (מכיל את הכותרות ואת הכפתור בצד ימין)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // כותרות מרכזיות
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel titleLabel = new JLabel("👥 ניהול חברי קהילה (עדכון בזמן אמת)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        totalMembersLabel = new JLabel("סך הכל חברי קהילה: 0", SwingConstants.CENTER);
        totalMembersLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        totalMembersLabel.setForeground(new Color(70, 130, 180));

        headerPanel.add(titleLabel);
        headerPanel.add(totalMembersLabel);

        // כפתור הסרת חבר - בצד ימין למעלה
        JButton removeButton = new JButton("🗑️ הסר חבר");
        removeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        removeButton.setBackground(new Color(220, 53, 69)); // אדום
        removeButton.setForeground(Color.WHITE);
        removeButton.setFocusPainted(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // אירוע לחיצה על כפתור הסרת חבר
        removeButton.addActionListener(e -> openRemoveMemberDialog());

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonWrapper.add(removeButton);

        topPanel.add(buttonWrapper, BorderLayout.EAST);
        topPanel.add(headerPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // טבלת הצגת חברי הקהילה
        String[] columnNames = {"שם המשתמש", "Telegram Username", "מועד הצטרפות", "Chat ID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable communityTable = new JTable(tableModel);
        communityTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        communityTable.setRowHeight(28);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < communityTable.getColumnCount(); i++) {
            communityTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        add(new JScrollPane(communityTable), BorderLayout.CENTER);
    }

    // פונקציה לפתיחת חלון לבחירת חבר להסרה מהקהילה
    private void openRemoveMemberDialog() {
        List<User> members = communityManager.getAllMembers();
        if (members.isEmpty()) {
            JOptionPane.showMessageDialog(this, "אין חברים בקהילה להסרה.", "הודעה", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // יצירת רשימת שמות מוצגת בחלון הבחירה
        String[] memberOptions = members.stream()
                .map(u -> u.getFirstName() + " (" + (u.getUsername() != null && !u.getUsername().equals("-") ? "@" + u.getUsername() : "Chat ID: " + u.getChatId()) + ")")
                .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "בחר את המשתמש שברצונך להסיר מהקהילה:",
                "הסרת חבר מהקהילה",
                JOptionPane.QUESTION_MESSAGE,
                null,
                memberOptions,
                memberOptions[0]
        );

        if (selected != null) {
            int selectedIndex = -1;
            for (int i = 0; i < memberOptions.length; i++) {
                if (memberOptions[i].equals(selected)) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex != -1) {
                User userToRemove = members.get(selectedIndex);

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "האם אתה בטוח שברצונך להסיר את " + userToRemove.getFirstName() + " מהקהילה?",
                        "אישור הסרה",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean removed = communityManager.removeUser(userToRemove.getChatId());
                    if (removed) {
                        JOptionPane.showMessageDialog(this, "המשתמש הוסר בהצלחה מהקהילה.", "הצלחה", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        }
    }

    @Override
    public void onCommunityUpdated(List<User> members) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (User user : members) {
                tableModel.addRow(new Object[]{
                        user.getFirstName(),
                        user.getUsername(),
                        user.getFormattedJoinTime(),
                        user.getChatId()
                });
            }
            totalMembersLabel.setText("סך הכל חברי קהילה: " + members.size());
        });
    }
}