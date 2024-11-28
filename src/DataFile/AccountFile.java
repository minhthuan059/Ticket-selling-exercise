package DataFile;

import Objects.Account;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

// TODO: Các thao tác với file chứa dữ liệu tài khoản (Account).
public class AccountFile {
    // TODO: Ghi file.
    public static void writeAccountFile(List<Account> persons, String fileName) {
        File f = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            if (!f.exists()) { // Nếu file không tồn tại thì tạo file
                f.createNewFile();
            }
            for (Account person : persons) {
                writer.write(person.getID() + "," + person.getName() + "," + person.getPhoneNumber());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Đọc file.
    public static List<Account> readAccountFile(String fileName) {
        List<Account> persons = new ArrayList<>();
        File f = new File(fileName);
        if (!f.exists()) {
            return persons;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String phoneNumber = parts[2];
                    persons.add(new Account(id, name, phoneNumber));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return persons;
    }

    // TODO: Thêm 1 dòng (1 Account) vào file.
    public static void appendAccountToFile(Account a, String fileName) {
        File file = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            if (!file.exists()) { // Nếu file không tồn tại thì tạo file
                file.createNewFile();
            }
            writer.write(a.getID() + "," + a.getName() + "," + a.getPhoneNumber());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
