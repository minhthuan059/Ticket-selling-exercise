package Objects;

import java.io.Serial;
import java.io.Serializable;


// TODO: Class chức thông tin người đặt vé gồm tên và số điện thoại.
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int ID;
    private String Name;
    private String PhoneNumber;

    public Account() {
    }

    public Account(String name, String phoneNumber) {
        Name = name;
        PhoneNumber = phoneNumber;
    }

    public Account(int ID, String name, String phoneNumber) {
        this.ID = ID;
        Name = name;
        PhoneNumber = phoneNumber;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        PhoneNumber = phoneNumber;
    }
}
