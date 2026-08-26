package org.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommunityManager {
    public interface CommunityChangeListener {
        void onCommunityUpdated(List<User> members);
    }

    private Map<Long, User> communityMembers = new LinkedHashMap<>();
    private final List<CommunityChangeListener> listeners = new CopyOnWriteArrayList<>();
    private static final String DATA_FILE = "community_data.dat"; // שם הקובץ שבו יישמרו הנתונים

    public CommunityManager() {
        loadCommunityFromFile(); // טעינת המשתמשים מיד כשהמערכת עולה
    }

    public void addListener(CommunityChangeListener listener) {
        listeners.add(listener);
        // עדכון המאזין החדש (כמו ה-GUI) בנתונים שנטענו
        listener.onCommunityUpdated(getAllMembers());
    }

    public synchronized boolean addUser(long chatId, String firstName, String username) {
        if (communityMembers.containsKey(chatId)) {
            return false;
        }
        User newUser = new User(chatId, firstName, username);
        communityMembers.put(chatId, newUser);

        saveCommunityToFile(); // שמירה לקובץ בכל פעם שמשתמש חדש מצטרף
        notifyListeners();

        return true;
    }

    // מתודה חדשה להסרת משתמש מהקהילה
    public synchronized boolean removeUser(long chatId) {
        if (communityMembers.containsKey(chatId)) {
            communityMembers.remove(chatId);
            saveCommunityToFile(); // שמירת השינוי לקובץ
            notifyListeners();    // עדכון ה-GUI בזמן אמת
            return true;
        }
        return false;
    }

    private void notifyListeners() {
        List<User> membersList = getAllMembers();
        for (CommunityChangeListener listener : listeners) {
            listener.onCommunityUpdated(membersList);
        }
    }

    public synchronized List<User> getAllMembers() {
        return new ArrayList<>(communityMembers.values());
    }

    public synchronized int getCommunitySize() {
        return communityMembers.size();
    }

    // --- פונקציות שמירה וטעינה ---

    private void saveCommunityToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(communityMembers);
        } catch (IOException e) {
            System.err.println("שגיאה בשמירת נתוני הקהילה: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCommunityFromFile() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                communityMembers = (Map<Long, User>) ois.readObject();
                System.out.println("✅ נתוני הקהילה נטענו בהצלחה מהקובץ (" + communityMembers.size() + " חברים).");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("שגיאה בטעינת נתוני הקהילה: " + e.getMessage());
            }
        }
    }
}