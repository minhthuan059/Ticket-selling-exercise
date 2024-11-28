package OrderMethod;

import Objects.Area;
import Objects.Seat;
import Objects.Session;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

// TODO: 1. Class chứa các phương thức để xử lý tiện lợi hơn.
//       2. Chứa các hàm tìm kiếm Object tự định nghĩa trong list khi đưa list tìm kiếm và key vào,
//       dùng cho client, bởi chỉ server truy cập và tìm kiếm được trong file dữ liệu.
public class OrderMethod {

    // TODO: Convert LocalTime thanh String.
    public static LocalTime convertToLocalTime(String timeString) {
        String[] timeParts = timeString.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return LocalTime.of(hour, minute);
    }

    // TODO: Kiểm tra vị trí đó có được đặt hay chưa bằng cách tìm vị trí đó trong danh sách các vị trí được đặt.
    //  (Không tìm toàn bộ danh sách vị trí đặt, chỉ tìm trong list truyền vào để tiết kiệm thời gian)
    public static boolean isBookedSeatAtRowAndColumn(List<Seat> _seats, int row, int column)
    {
        if (_seats != null)
        {
            for (Seat s : _seats){
                if (s.getRow() == row && s.getColumn() == column)
                    return true;
            }
        }
        return false;
    }


    public static Seat getSeatByRowColumnInList(List<Seat> seats, int row, int column){
        for (Seat s : seats){
            if (s.getRow() == row && s.getColumn() == column)
                return s;
        }
        return null;
    }

    // TODO: Tìm suất theo thời gian khi truyền vào start và end time.
    public static Session getSessionByTimeInList(List<Session> sessions, LocalTime startTime, LocalTime endTime) {
        for (Session s : sessions){
            LocalTime sessionStartTime = s.getTimeStart();
            Duration duration = Duration.between(LocalTime.MIDNIGHT, s.getTimeLong());
            LocalTime sessionEndTime = sessionStartTime.plus(duration); // end time = start time + long time.

            if (sessionStartTime.equals(startTime) && sessionEndTime.equals(endTime)) {
                return s;
            }
        }
        return null;
    }

    // TODO: Tìm list khán đài của suất chiếu.
    public static List<Area> getAreasBySessionID(List<Area> areas, int sessionID) {
        return areas.stream()
                .filter(area -> area.getSessionID() == sessionID)
                .collect(Collectors.toList());
    }

    // TODO: Tìm list suất chiếu theo ID.
    public static Session getSessionByIDInList(List<Session> sessions, int ID) {
        for (Session s : sessions){
            if (s.getID() == ID) {
                return s;
            }
        }
        return null;
    }
}
