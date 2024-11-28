package Method;

import Objects.Seat;
import DataFile.SeatFile;

import java.util.List;
import java.util.stream.Collectors;

//  TODO: Chứa các phương thức với toàn bộ dữ liệu về các vị trí được đặt (Seat).
public class SeatMethod {
    private List<Seat> seats = null;

    // TODO: Đọc file để khởi tạo.
    public SeatMethod(String filename) {
        seats = SeatFile.readSeatFile(filename);
    }

    public List<Seat> getSeats() {
        return seats;
    }

    // TODO: Lấy list vị trí đặt theo khán đài.
    public List<Seat> getSeatByAreaID(int areaID) {
        return seats.stream()
                .filter(seat -> seat.getAreaID() == areaID)
                .collect(Collectors.toList());
    }

    // TODO: Lấy lish vị trí đặt theo tài khoản đặt.
    public List<Seat> getSeatByAccountID(int accountID) {
        return seats.stream()
                .filter(seat -> seat.getAccountID() == accountID)
                .collect(Collectors.toList());
    }

    // TODO: Lấy vị trí đặt theo hàng, cột.
    public Seat getSeatAtAreaInRowAndColumn(int areaID, int row, int column)
    {
        for (Seat s : seats){
            if (s.getAreaID() == areaID && s.getRow() == row && s.getColumn() == column)
                return s;
        }
        return null;
    }

    // TODO: Tạo 1 ID mới.
    public int getNewID() {
        int maxID = 0;
        for (Seat seat : seats) {
            if (seat.getID() > maxID) {
                maxID = seat.getID();
            }
        }
        return maxID + 1;
    }

    // TODO: Thêm vị trí được đặt.
    public void addBookedSeat(Seat s) {
        seats.add(s);
    }

    // TODO: Xóa vị trí đã đặt theo khán đài.
    public void deleteSeatByAreaID(int id) {
        seats.removeIf(s -> s.getAreaID() == id);
    }

    // TODO: Xóa vị trí đã đặt theo id.
    public void deleteSeatByID(int id) {
        seats.removeIf(s -> s.getID() == id);
    }
}
