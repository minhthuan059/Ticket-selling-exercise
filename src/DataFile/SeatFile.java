package DataFile;

import Objects.Seat;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// TODO: Các thao tác với file chứa dữ liệu vị trí được đặt (Seat).
public class SeatFile {

    // TODO: Ghi file.
    public static void writeSeatFile(List<Seat> seats, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            if (!f.exists()) {
                f.createNewFile();
            }
            for (Seat seat : seats) {
                writer.write(seat.getID() + "," + seat.getAccountID() + "," + seat.getAreaID() + "," + seat.getRow() + "," + seat.getColumn());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Đọc file.
    public static List<Seat> readSeatFile(String fileName) {
        List<Seat> seats = new ArrayList<>();
        File f = new File(fileName);
        if (!f.exists())
        {
            return seats;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    int id = Integer.parseInt(parts[0]);
                    int accountID = Integer.parseInt(parts[1]);
                    int areaID = Integer.parseInt(parts[2]);
                    int row = Integer.parseInt(parts[3]);
                    int column = Integer.parseInt(parts[4]);
                    seats.add(new Seat(id, accountID, areaID, row, column));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return seats;
    }

    // TODO: Thêm 1 dòng (1 Account) vào file.
    public static void appendSeatToFile(Seat seat, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            if (!f.exists()) {
                f.createNewFile();
            }
            writer.write(seat.getID() + "," + seat.getAccountID() + "," + seat.getAreaID() + "," + seat.getRow() + "," + seat.getColumn());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
