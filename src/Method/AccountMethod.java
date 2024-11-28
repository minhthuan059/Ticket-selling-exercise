package Method;

import DataFile.AccountFile;
import Objects.Account;

import java.util.List;
import java.util.Objects;

//  TODO: Chứa các phương thức với toàn bộ dữ liệu về tài khoản (Acount).
public class AccountMethod {
    private List<Account> accounts = null;

    // TODO: Đọc file để khởi tạo.
    public AccountMethod(String filename) {
        accounts = AccountFile.readAccountFile(filename);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    // TODO: Lấy Account theo thông tin Tên và Số điện thoại.
    public Account getAccountByNameAndNumberphone(String name, String numberphone){
        for (Account a : accounts){
            if (Objects.equals(a.getName(), name) && Objects.equals(a.getPhoneNumber(), numberphone)){
                return a;
            }
        }
        return null;
    }

    // TODO: Lấy Account theo ID.
    public Account getAccountByID(int ID) {
        for (Account a : accounts) {
            if (a.getID() == ID) {
                return a;
            }
        }
        return null;
    }

    // TODO: Thêm 1 account.
    public void addNewAccount(Account s) {
        accounts.add(s);
    }

    // TODO: Tạo 1 ID mới.
    public int getNewID() {
        int maxID = 0;
        for (Account account : accounts) {
            if (account.getID() > maxID) {
                maxID = account.getID();
            }
        }
        return maxID + 1;
    }

}
