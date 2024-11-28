package Objects;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

// TODO: Class chứa thông tin chổ ngồi được đặt gồm chỗ thuộc khán đài (Area) nào, người (Account) nào đăng ký và vị trí.
public class Seat implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int ID;
    private int AccountID;
    private int AreaID;
    private int Row;
    private int Column;

    public Seat() {
    }

    public Seat(int ID, int accountID, int areaID, int row, int column) {
        this.ID = ID;
        AccountID = accountID;
        AreaID = areaID;
        Row = row;
        Column = column;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getAccountID() {
        return AccountID;
    }

    public void setAccountID(int accountID) {
        AccountID = accountID;
    }

    public int getAreaID() {
        return AreaID;
    }

    public void setAreaID(int areaID) {
        AreaID = areaID;
    }

    public int getRow() {
        return Row;
    }

    public void setRow(int row) {
        Row = row;
    }

    public int getColumn() {
        return Column;
    }

    public void setColumn(int column) {
        Column = column;
    }

    @Override
    public String toString() {
        return "(" + getRow() + ", " + getColumn() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return AreaID == seat.AreaID && Row == seat.Row && Column == seat.Column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(AreaID, Row, Column);
    }
}
