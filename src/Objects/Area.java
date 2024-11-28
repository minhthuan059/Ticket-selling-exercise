package Objects;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

// TODO: Class chứa tông tin của khán đài gồm thuộc suất chiếu (Session) nào và thông tin về tên, giá, số hàng, cột.
public class Area implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int ID;
    private int SessionID;
    private String Name;
    private int Price;
    private int numberRow;
    private int numberColumn;


    public Area() {
    }

    public Area(int ID, int sessionID, String name, int price, int numberRow, int numberColumn) {
        this.ID = ID;
        SessionID = sessionID;
        Name = name;
        Price = price;
        this.numberRow = numberRow;
        this.numberColumn = numberColumn;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getSessionID() {
        return SessionID;
    }

    public void setSessionID(int sessionID) {
        SessionID = sessionID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getPrice() {
        return Price;
    }

    public void setPrice(int price) {
        Price = price;
    }

    public int getNumberRow() {
        return numberRow;
    }

    public void setNumberRow(int numberRow) {
        this.numberRow = numberRow;
    }

    public int getNumberColumn() {
        return numberColumn;
    }

    public void setNumberColumn(int numberColumn) {
        this.numberColumn = numberColumn;
    }

    @Override
    public String toString() {
        return "Khán đài: " + getName() + " - Giá: " + getPrice() + " VNĐ - Kích thước: " + getNumberRow() + "x" + getNumberColumn();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Area area = (Area) o;
        return SessionID == area.SessionID && Objects.equals(Name, area.Name);
    }

    @Override
    public int hashCode() {
        return Objects.hash( SessionID, Name);
    }
}
