package DataFile;

import Objects.Session;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// TODO: Các thao tác với file chứa dữ liệu suất sự kiện (Session).
public class SessionFile {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    // TODO: Ghi file.
    public static void writeSessionFile(List<Session> sessions, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            if (!f.exists()) {
                f.createNewFile();
            }
            for (Session session : sessions) {
                writer.write(session.getID() + "," + session.getTimeStart().format(formatter) + "," + session.getTimeLong().format(formatter));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Đọc file.
    public static List<Session> readSessionFile(String fileName) {
        List<Session> sessions = new ArrayList<>();
        File f = new File(fileName);
        if (!f.exists()) {
            return sessions;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int id = Integer.parseInt(parts[0]);
                    LocalTime timeStart = LocalTime.parse(parts[1], formatter);
                    LocalTime timeLong = LocalTime.parse(parts[2], formatter);
                    sessions.add(new Session(id, timeStart, timeLong));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    // TODO: Thêm 1 dòng (1 Account) vào file.
    public static void appendSessionToFile(Session session, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            if (!f.exists()) {
                f.createNewFile();
            }
            writer.write(session.getID() + "," + session.getTimeStart().format(formatter) + "," + session.getTimeLong().format(formatter));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
