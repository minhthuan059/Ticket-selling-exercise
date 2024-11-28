package Method;

import Objects.Area;
import DataFile.AreaFile;

import java.util.List;
import java.util.stream.Collectors;

//  TODO: Chứa các phương thức với toàn bộ dữ liệu về khán đài (Area).
public class AreaMethod {
    private List<Area> areas = null;

    // TODO: Đọc file để khởi tạo.
    public AreaMethod(String filename) {
        areas = AreaFile.readAreaFile(filename);
    }

    public List<Area> getAreas() {
        return areas;
    }

    // TODO: Lấy list khán đài theo suất sự kiện.
    public List<Area> getAreasBySessionID(int sessionID) {
        return areas.stream()
                .filter(area -> area.getSessionID() == sessionID)
                .collect(Collectors.toList());
    }

    // TODO: Lấy khán đài theo id.
    public Area getAreasByID(int areaID) {
        for (Area area : areas) {
            if (area.getID() == areaID) {
                return area;
            }
        }
        return null;
    }

    // TODO: Lấy khán đài theo tên và giá.
    public Area getAreaByNameAndPrice(String name, int price) {
        for (Area area : areas) {
            if (area.getName().equals(name) && area.getPrice() == price) {
                return area;
            }
        }
        return null;
    }

    // TODO: Tạo 1 ID mới.
    public int getNewID() {
        int maxID = 0;
        for (Area area : areas) {
            if (area.getID() > maxID) {
                maxID = area.getID();
            }
        }
        return maxID + 1;
    }

    // TODO: Thêm khán đài.
    public void addAreaToSession(Area a) {
        areas.add(a);
    }

    // TODO: Xóa khán đài theo suất sự kiện.
    public void deleteAreaBySessionID(int id) {
        areas.removeIf(a -> a.getSessionID() == id);
    }

    // TODO: Xóa khán đài theo id.
    public void deleteAreaByID(int id) {
        areas.removeIf(a -> a.getID() == id);
    }
}
