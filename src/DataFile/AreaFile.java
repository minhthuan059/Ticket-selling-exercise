package DataFile;

import Objects.Area;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// TODO: Các thao tác với file chứa dữ liệu khán đài (Area).
public class AreaFile {

    // TODO: Ghi file.
    public static void writeAreaFile(List<Area> areas, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            if (!f.exists()) {
                f.createNewFile();
            }
            for (Area area : areas) {
                writer.write(area.getID() + "," + area.getSessionID() + "," + area.getName() + "," + area.getPrice() + "," + area.getNumberRow() + "," + area.getNumberColumn());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Đọc file.
    public static List<Area> readAreaFile(String fileName) {
        List<Area> areas = new ArrayList<>();
        File f = new File(fileName);
        if (!f.exists()) {
            return areas;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    int id = Integer.parseInt(parts[0]);
                    int sessionID = Integer.parseInt(parts[1]);
                    String name = parts[2];
                    int price = Integer.parseInt(parts[3]);
                    int numberRow = Integer.parseInt(parts[4]);
                    int numberColumn = Integer.parseInt(parts[5]);
                    areas.add(new Area(id, sessionID, name, price, numberRow, numberColumn));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return areas;
    }

    // TODO: Thêm 1 dòng (1 Account) vào file.
    public static void appendAreaToFile(Area area, String fileName) {
        File file = new File(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            if (!file.exists()) {
                file.createNewFile();
            }
            writer.write(area.getID() + "," + area.getSessionID() + "," + area.getName() + "," + area.getPrice() + "," + area.getNumberRow() + "," + area.getNumberColumn());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
