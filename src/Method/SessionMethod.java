package Method;

import DataFile.SessionFile;
import Objects.Session;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;


//  TODO: Chứa các phương thức với toàn bộ dữ liệu về suất sự kiện (Session).
public class SessionMethod {

    private List<Session> sessions = null;

    // TODO: Đọc file để khởi tạo.
    public SessionMethod(String filename) {
        sessions = SessionFile.readSessionFile(filename);
    }

    public List<Session> getSessions() {
        return sessions;
    }

    // TODO: Thêm 1 suất sự kiện.
    public void addNewSession(Session s) {
        sessions.add(s);
    }

    // TODO: Lấy suất sự kiện theo thời gian.
    public Session getSessionByTime(LocalTime startTime, LocalTime endTime) {
        for (Session session : sessions) {
            LocalTime sessionStartTime = session.getTimeStart();
            Duration duration = Duration.between(LocalTime.MIDNIGHT, session.getTimeLong());
            LocalTime sessionEndTime = sessionStartTime.plus(duration);

            if (sessionStartTime.equals(startTime) && sessionEndTime.equals(endTime)) {
                return session;
            }
        }

        return null;
    }

    public Session getSessionsByID(int sessionID) {
        for (Session session : sessions) {
            if (session.getID() == sessionID) {
                return session;
            }
        }
        return null;
    }

    // TODO: Tạo 1 ID mới.
    public int getNewID() {
        int maxID = 0;
        for (Session session : sessions) {
            if (session.getID() > maxID) {
                maxID = session.getID();
            }
        }
        return maxID + 1;
    }

    // TODO: Xóa suất sự kiện.
    public void deleteSessionByID(int id) {
        sessions.removeIf(s -> s.getID() == id);
    }
}
