package Objects;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

// TODO: Class chứa thông tin suất chiếu, gồm thời gian bắt đầu và độ dài suất chiếu.
public class Session implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int ID;
    private LocalTime timeStart;
    private LocalTime timeLong;

    public Session() {
    }

    public Session(int ID, LocalTime timeStart, LocalTime timeLong) {
        this.ID = ID;
        this.timeStart = timeStart;
        this.timeLong = timeLong;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public LocalTime getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(LocalTime timeStart) {
        this.timeStart = timeStart;
    }

    public LocalTime getTimeLong() {
        return timeLong;
    }

    public void setTimeLong(LocalTime timeLong) {
        this.timeLong = timeLong;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(timeStart, session.timeStart) && Objects.equals(timeLong, session.timeLong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeStart, timeLong);
    }

    @Override
    public String toString() {
        Duration duration = Duration.between(LocalTime.MIDNIGHT, getTimeLong());
        LocalTime sessionEndTime = getTimeStart().plus(duration);
        return "(" + timeStart + ", " + sessionEndTime + ")";
    }
}
