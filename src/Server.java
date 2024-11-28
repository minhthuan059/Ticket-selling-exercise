import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import DataFile.*;
import Objects.*;
import Method.*;
import OrderMethod.*;

public class Server {


    // Các String command dùng để đánh dấu giao tiếp giữa client và server cho biết client hoặc server muốn làm gì.
    private static final String getAccountFromServer = "GET_ACCOUNT_BY_INFORMATION";
    private static final String getListSessionFromServer = "GET_LIST_SESSION";
    private static final String getListAreaOfSesionFromServer = "GET_LIST_AREA_OF_SESSION";
    private static final String getListSeatOfAreaFromServer = "GET_LIST_SEAT_FROM_AREA";
    private static final String updateAccountToServer = "UPDATE_ACCOUNT_BY_INFORMATION";
    private static final String postAccountBookSeatToServer = "POST_ACCOUNT_BOOK_SEAT";
    private static final String postDeleteBookedSeatToServer = "POST_DELETE_BOOKED_SEAT";
    private static final String broadcastBookedSeatToAllClient = "BROADCAST_UPDATE_BOOKED_SEAT_TO_ALL_CLIENT";
    private static final String broadcastUpdateSessionToAllClient = "BROADCAST_UPDATE_SESSION_TO_ALL_CLIENT";
    private static final String broadcastUpdateDeleteSessionToAllClient = "BROADCAST_UPDATE_DELETE_SESSION_TO_ALL_CLIENT";
    private static final String broadcastUpdateAreaToAllClient = "BROADCAST_UPDATE_AREA_TO_ALL_CLIENT";
    private static final String broadcastUpdateDeleteAreaToAllClient = "BROADCAST_UPDATE_DELETE_AREA_TO_ALL_CLIENT";
    private static final String broadcastUpdateSeatToAllClient = "BROADCAST_UPDATE_SEAT_TO_ALL_CLIENT";


    // Danh sách các socket và stream dùng cho cho việc broadcast và close.
    private static final List<Socket> listSocketConnection = new ArrayList<>();
    private static final List<ObjectInputStream> listObjectInputStream = new ArrayList<>();
    private static final List<ObjectOutputStream> listObjectOutputStream = new ArrayList<>();

    // File name chứa dữ liệu ban đầu.
    private static final String CONFIG_FILE_PATH = "./port.xml";
    private static final String sessionFileName = "./DataFolder/SessionData.txt";
    private static final String areaFileName = "./DataFolder/AreaData.txt";
    private static final String seatFileName = "./DataFolder/SeatData.txt";
    private static final String accountFileName = "./DataFolder/AccountData.txt";

    // Danh sách các biến hiện thời để qunar lý hiển thị lên giao diện.
    private static Session currentSession = null; // Suất đang được chọn.
    private static Area currentArea = null; // Khán đài đang được chọn.
    private static List<Area> currentListAreaOfSession = new ArrayList<>(); // List khán đài thuộc suất đang chọn.
    private static List<Seat> currentListSeatIsBookedOfArea = new ArrayList<>(); // List vị trí được đặt theo khán đài đnag chọn.

    // Các biến method chứa phương thức và dữ liệu toàn bộ các đối tượng.
    private static final SessionMethod sessionMethod = new  SessionMethod(sessionFileName);
    private static final AreaMethod areaMethod = new AreaMethod(areaFileName);
    private static final SeatMethod seatMethod = new SeatMethod(seatFileName);
    private static final AccountMethod accountMethod = new AccountMethod(accountFileName);


    private static final int port = GetPortFromXmlFile.getPortFromXmlFile(CONFIG_FILE_PATH);

    // Các biến giao diện swing (giá trị thay nhiều lần do giao tiếp với client hoặc
    // lý do khác mà không cần có tương tác từ giao diện, hoặc có móc nối với nhều giá trị ngoài).

    // Bảng chứa danh sách suất sự kiện.
    private static final DefaultTableModel modelListSession = new DefaultTableModel();
    private static final JTable listSessionTable = new JTable(modelListSession) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // Bảng chứa danh sách khán đài của suất chiếu đang chọn.
    private static final DefaultTableModel modelListArea = new DefaultTableModel();
    private static final JTable listAreaTable = new JTable(modelListArea) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // Panel chứa danh sách các vị trí.
    private  static final JPanel mapSeatPanel = new JPanel();

    // Vị trí hiển thị.
    private static int indexRow = 0;
    private static int indexColumn = 0;


    // TODO: Khởi tạo server chờ kết nối từ nhiều client.
    public Server() throws IOException {
        String folder = "DataFolder";
        File directory = new File(folder);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        while (true) {
            try  (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server waiting...");
                Socket socket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                listObjectOutputStream.add(out); // Thêm stream vào danh sách.
                listObjectInputStream.add(in);
                listSocketConnection.add(socket);
                // Mở thread mới cho việc giao tiếp với client kết nối đến.
                HandleCommunicationWithClient t = new HandleCommunicationWithClient(socket, in, out);
                t.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // TODO: Gửi danh sách vị trí được đăng ký mới đến toàn bộ client.
    private static void BroadcastBookedList(List<Seat> s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastBookedSeatToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Gửi danh sách suất chiếu mới đến toàn bộ client.
    private static void BroadcastUpdateSession(Session s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastUpdateSessionToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Gửi danh sách suất chiếu mới đến toàn bộ client.
    private static void BroadcastUpdateDeleteSession(Session s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastUpdateDeleteSessionToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Gửi danh sách khán đài mới update đến toàn bộ client.
    private static void BroadcastUpdateArea(Area s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastUpdateAreaToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Gửi danh sách khán đài mới sau khi xóa đến toàn bộ client.
    private static void BroadcastUpdateDeleteArea(Area s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastUpdateDeleteAreaToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Gửi danh sách các vị trí vừa bị hủy đặt trước đến toàn bộ client.
    private static void BroadcastUpdateSeat(Seat s) throws IOException {
        for(ObjectOutputStream o : listObjectOutputStream){
            synchronized (o) {
                o.writeObject(broadcastUpdateSeatToAllClient);
                o.flush();
                o.writeObject(s);
                o.flush();
            }
        }
    }

    // TODO: Class thread làm nhiệm vụ giao tiếp với client.
     private static class HandleCommunicationWithClient extends Thread {
        private final Socket socket;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;

        // Nhận các biến giao tiếp với 1 client.
        public HandleCommunicationWithClient(Socket socket, ObjectInputStream in, ObjectOutputStream out ) throws IOException {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                while (true) // Vòng lặp để chờ client gửi requset.
                {
                    String command = (String) in.readObject(); // Nhận lệnh.

                    if (Objects.equals(command, getListSessionFromServer)) { // Client yêu cầu list các suất sự kiện.
                        out.writeObject(getListSessionFromServer); // Gửi lại command.
                        out.flush();
                        out.writeObject(sessionMethod.getSessions()); // Gửi list suất sự kiện.
                        out.flush();
                    } else if (Objects.equals(command, getListAreaOfSesionFromServer)){ // Client yêu cầu list các khán đài của suất.
                        out.writeObject(getListAreaOfSesionFromServer); // Gửi lại command.
                        out.flush();
                        Session s = (Session) in.readObject(); //Lấy suất.
                        List<Area> list = areaMethod.getAreasBySessionID(s.getID()); // Lấy đăng sách suất
                        out.writeObject(list);  // Gửi data client yêu cầu.
                        out.flush();
                    } else if (Objects.equals(command, getListSeatOfAreaFromServer)){ // Client yêu cầu list vị trí đã đặt của khán đài.
                        out.writeObject(getListSeatOfAreaFromServer); // Gửi lại command.
                        out.flush();
                        Area currArea = (Area) in.readObject(); // Lấy khán đài.
                        List<Seat> list = seatMethod.getSeatByAreaID(currArea.getID()); // Lấy danh sách vị trí được đặt của khán đài.
                        out.writeObject(list); // Gửi data client yêu cầu.
                        out.flush();
                    } else if (Objects.equals(command, getAccountFromServer)){ // Client yêu cầu thông tin chi tiết tài khoản theo Tài khoản.
                        out.writeObject(getAccountFromServer); // Gửi lại command.
                        out.flush();
                        Account a = (Account) in.readObject();  // Nhận tài khoản client muốn lấy thông tin.
                        Account find = accountMethod.getAccountByNameAndNumberphone(a.getName(), a.getPhoneNumber()); // Tìm tài khoản.
                        // TODO: Tạo tài khoản nếu chưa có tài khoản.
                        if (find == null) {
                            Account tmp = new Account(accountMethod.getNewID(), a.getName(), a.getPhoneNumber());
                            accountMethod.addNewAccount(tmp);
                            AccountFile.appendAccountToFile(tmp, accountFileName);
                            out.writeObject(null);
                            out.flush();
                        } else { // TODO: Lấy thông tin chi tiết của tài khoản.
                            List<Seat> seatOfAccout = seatMethod.getSeatByAccountID(find.getID()); // Lấy list vị trí đã đặt của tài khoản.
                            // TODO: Nếu đã từng đặt chổ thì trả về danh sách đặt chổ.
                            if (!seatOfAccout.isEmpty()) {
                                List<Area> areaOfSeat = new ArrayList<>();
                                for (Seat s : seatOfAccout){  // Tìm khu vực của các vị trí đã đặt.
                                    Area tmp = areaMethod.getAreasByID(s.getAreaID());
                                    areaOfSeat.add(tmp);
                                }
                                out.writeObject(seatOfAccout); // Gửi thông tin chi tiết cho client.
                                out.flush();
                                out.writeObject(areaOfSeat);
                                out.flush();
                            } else { // TODO: Nếu chỉ có tài khoản mà không có dữ liệu đặt chổ thì trả về tài khoản đó.
                                out.writeObject(seatOfAccout);
                                out.flush();
                            }

                        }
                    } else if (Objects.equals(command, postAccountBookSeatToServer)) { // Client yêu cầu đặt vị trí.
                        out.writeObject(postAccountBookSeatToServer); // Gửi lại lệnh.
                        out.flush();
                        Account a = (Account) in.readObject();  // Lấy thông tin tài khoản đặt chổ.
                        Account find = accountMethod.getAccountByNameAndNumberphone(a.getName(), a.getPhoneNumber()); // Tìm tài khoản trong list.
                        List <Seat> bookSeats = (List<Seat>) in.readObject(); // Lấy danh sách vị trí đặt.
                        // TODO: Xử lý đặt chổ.
                        for (Seat s : bookSeats) {
                            Seat newBookSeat = new Seat(seatMethod.getNewID(), find.getID(), s.getAreaID(), s.getRow(), s.getColumn()); // Tạo vị trí đặt với thông tin người dùng gửi.
                            synchronized (newBookSeat) {  // Đồng bộ hóa để tránh tình trạng 1 vị trí bị đặt cùng lúc.
                                if (!seatMethod.getSeats().contains(newBookSeat)) {  // Kiểm tra vị trí đã được đặt chưa.
                                    seatMethod.addBookedSeat(newBookSeat);  // Thêm vào list.
                                    SeatFile.appendSeatToFile(newBookSeat, seatFileName); // Thêm vào file.
                                }
                            }
                            // TODO: Nếu màn hình đang hiển thị có danh sách đặt chổ thì vẽ lại.
                            if (currentArea != null && currentArea.getID() == newBookSeat.getAreaID()) {
                                // TODO: Thay đổi biến hiển thị trên server.
                                if (currentListSeatIsBookedOfArea == null)
                                    currentListSeatIsBookedOfArea = new ArrayList<>();
                                currentListSeatIsBookedOfArea.add(newBookSeat);
                                SwingUtilities.invokeLater(Server::setMapOfSeat);
                            }
                        }
                        out.writeObject(true); // Gửi thông tin báo không có lỗi khi thục hiện đến bước hiện tại.
                        out.flush();
                        // TODO: Tạo luồng gửi broadcast thông báo về sự thay đổi các vị trí trống.
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(()->{
                            try {
                                BroadcastBookedList(bookSeats);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        executor.shutdown();
                    } else if (Objects.equals(command, postDeleteBookedSeatToServer)) { // Client yêu cầu xóa vị trí đã đặt.
                        out.writeObject(postDeleteBookedSeatToServer); // Gửi lại command.
                        out.flush();
                        Seat s = (Seat) in.readObject();  // Lấy thông tin vị trí bị hủy.
                        seatMethod.deleteSeatByID(s.getID()); // Xóa khỏi list.
                        SeatFile.writeSeatFile(seatMethod.getSeats(), seatFileName); // Ghi lại file.
                        out.writeObject(true); // Gửi kết quả.
                        out.flush();
                        // TODO: Kiểm tra vị trí bị hủy có đang hiển thị lên màn hình không, nếu có thì vẽ lại.
                        if (currentArea != null && s.getAreaID() == currentArea.getID()) {
                            currentListSeatIsBookedOfArea.remove(s);
                            SwingUtilities.invokeLater(Server::setMapOfSeat);
                        }
                        // TODO: Tạo luồng gửi broadcast thông báo về sự thay đổi các vị trí trống.
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                BroadcastUpdateSeat(s);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                        executor.shutdown();
                    } else if (Objects.equals(command, updateAccountToServer)) {  // Client yêu cầu gửi lại thông tin tài khoản đã update khi đặt/hủy chổ.
                        // TODO: Làm giống như tìm thông tin chi tiết tài khoản ở trên,
                        //  nhưng không cần xét trường hợp tồn tại do ở bước này đảm bảo tài khoản đã định danh.
                        out.writeObject(updateAccountToServer);
                        out.flush();
                        Account a = (Account) in.readObject();
                        Account find = accountMethod.getAccountByNameAndNumberphone(a.getName(), a.getPhoneNumber());
                        List<Seat> seatOfAccout = seatMethod.getSeatByAccountID(find.getID());
                        if (!seatOfAccout.isEmpty()) {
                            List<Area> areaOfSeat = new ArrayList<>();
                            for (Seat s : seatOfAccout){
                                Area tmp = areaMethod.getAreasByID(s.getAreaID());
                                areaOfSeat.add(tmp);
                            }
                            out.writeObject(seatOfAccout);
                            out.flush();
                            out.writeObject(areaOfSeat);
                            out.flush();
                        } else {
                            out.writeObject(seatOfAccout);
                            out.flush();
                        }

                    } else if (Objects.equals(command, "QUIT")){ // Client yêu cầu hủy kết nối
                        // TODO: Close toàn bộ các stream và socket, xóa khỏi danh sách broadcast.
                        listSocketConnection.remove(socket);
                        listObjectInputStream.remove(in);
                        listObjectOutputStream.remove(out);
                        socket.close();
                        out.close();
                        in.close();
                    }

                }

            } catch (IOException e) { // Đóng toàn bộ khi xảy ra lỗi.
                try {
                    listSocketConnection.remove(socket);
                    listObjectInputStream.remove(in);
                    listObjectOutputStream.remove(out);
                    socket.close();
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // TODO: Tạo UI cho màn hình chính.
    private static JPanel ServerPanel()
    {
        // Lấy kích thước thiết bị
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        // Sử dụng box layout.
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Title Panel
        JLabel title = new JLabel("QUẢN LÝ BÁN VÉ");
        title.setFont(new Font(null, Font.BOLD, 20));
        JPanel titlePanel = new JPanel();
        titlePanel.add(title);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.setMaximumSize(new Dimension(screenWidth, 50));
        mainPanel.add(titlePanel);

        // Conten panel chứa nội dung chính.
        JPanel contentPanel = new JPanel();
        mainPanel.add(contentPanel);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

        // Left panel chứa danh sách suất và xóa/thêm suất.
        JPanel leftPanel = getSessionListPanel();
        leftPanel.setPreferredSize(new Dimension(300, screenHeight));
        leftPanel.setMaximumSize(new Dimension(300, screenHeight));
        leftPanel.setBackground(Color.LIGHT_GRAY);

        // Center panel chứa danh sách khán đài và thêm/xóa khán đài.
        JPanel centerPanel = getAreaListPanel();
        centerPanel.setPreferredSize(new Dimension(300, screenHeight));
        centerPanel.setMaximumSize(new Dimension(300, screenHeight));
        centerPanel.setBackground(Color.LIGHT_GRAY);

        // Right panel chứa hiển thị danh sách vị trí.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setMaximumSize(new Dimension(screenWidth-600, screenHeight));
        rightPanel.setBackground(Color.LIGHT_GRAY);
        rightPanel.add(mapSeatPanel, BorderLayout.CENTER);

        // Các button chuyển hiển thị đầy đủ chổ (màn hình chỉ hiển thị 10x10 vị trí)
        // để hiển thị thêm các vị trí khác cùng Area nhấn các nút định hướng dưới.
        JButton topButton = new JButton("^");
        topButton.setBackground(Color.lightGray);
        JButton bottomButton = new JButton("v");
        bottomButton.setBackground(Color.lightGray);
        JButton leftButton = new JButton("<");
        leftButton.setBackground(Color.lightGray);
        leftButton.setMargin(new Insets(0, 7, 0, 7));
        JButton rightButton = new JButton(">");
        rightButton.setBackground(Color.lightGray);
        rightButton.setMargin(new Insets(0, 7, 0, 7));


        rightPanel.add(topButton, BorderLayout.PAGE_START);
        rightPanel.add(bottomButton, BorderLayout.PAGE_END);
        rightPanel.add(leftButton, BorderLayout.LINE_START);
        rightPanel.add(rightButton, BorderLayout.LINE_END);

        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(leftPanel);
        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(centerPanel);
        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(rightPanel);
        contentPanel.add(Box.createHorizontalStrut(10));

        // TODO: Xử lý nhấn các nút chuyển vùng hiển thị trong Area.
        topButton.addActionListener(e -> {
            if (currentArea != null && indexRow > 0){
                indexRow--;
                setMapOfSeat();
            }
        });
        bottomButton.addActionListener(e -> {
            if (currentArea != null && (indexRow < currentArea.getNumberRow()/10 - 1 || (indexRow == currentArea.getNumberRow()/10 - 1 && currentArea.getNumberRow()%10 != 0))) {
                indexRow++;
                setMapOfSeat();
            }
        });
        leftButton.addActionListener(e -> {
            if (currentArea != null && indexColumn > 0){
                indexColumn--;
                setMapOfSeat();
            }
        });
        rightButton.addActionListener(e -> {
            if (currentArea != null && (indexColumn < currentArea.getNumberColumn()/10 - 1 || (indexColumn == currentArea.getNumberColumn()/10 - 1 && currentArea.getNumberColumn()%10 != 0))) {
                indexColumn++;
                setMapOfSeat();
            }

        });

        return mainPanel;
    }

    /**
     * @param modelList truyền vào model thiết lập bảng.
     * @param listTable truyền vào bảng
     * @param isSessionTable cho biết loại bảng muốn nhận
     * @return hiển thị ScrollPane chứa bảng trắng chưa có data cho session hoặc area.
     * */
    private static JScrollPane getScrollPane(DefaultTableModel modelList, JTable listTable, boolean isSessionTable) {

        if (listTable.getColumnCount() == 0) {
            if(isSessionTable) {
                modelList.addColumn("Bắt đầu");
                modelList.addColumn("Kết thúc");
                listTable.getColumnModel().getColumn(0).setPreferredWidth(150);
                listTable.getColumnModel().getColumn(1).setPreferredWidth(150);
            }
            else {
                modelList.addColumn("Tên khu");
                modelList.addColumn("Giá vé");
                modelList.addColumn("Số ghế (hàng, cột)");
                listTable.getColumnModel().getColumn(0).setPreferredWidth(100);
                listTable.getColumnModel().getColumn(1).setPreferredWidth(80);
                listTable.getColumnModel().getColumn(2).setPreferredWidth(120);

            }

            JScrollPane listScrollPane = new JScrollPane(listTable);
            listScrollPane.setPreferredSize(new Dimension(300, 150));
            listScrollPane.setMaximumSize(new Dimension(300, 500));
            return listScrollPane;
        }
        else {
            return null;
        }
    }

    // TODO: Thiết lập dữ liệu cho bảng suất sự kiện (Session).
    public static void setDataSessionTable(){
        modelListSession.setRowCount(0);
        for (Session session : sessionMethod.getSessions()) {
            Duration duration = Duration.between(LocalTime.MIDNIGHT, session.getTimeLong());
            modelListSession.addRow(new Object[]{session.getTimeStart().toString(), (session.getTimeStart().plus(duration)).toString()});
        }
    }

    // TODO: Thiết lập dữ liệu cho bảng khán đài (Area).
    public static void setDataAreaTable(){
        modelListArea.setRowCount(0);
        if (currentListAreaOfSession != null) {
            for (Area area: currentListAreaOfSession) {
                modelListArea.addRow(new Object[]{area.getName(), area.getPrice(), "(" + area.getNumberRow() + ", " + area.getNumberColumn() + ")"});
            }
        }

    }

    // TODO: Thiết lập giao diện bảng tương tác với suất sự kiện (Session).
    private static JPanel getSessionListPanel() {
        // Dùng box layout.
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(10));

        // Bảng suất chiếu.
        JLabel listSession = new JLabel("Danh sách suất chiếu");
        listSession.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        listSession.setHorizontalAlignment(SwingConstants.CENTER);

        // Đặt font chữ cho JLabel
        listSession.setFont(new Font(null, Font.BOLD, 16));
        pane.add(listSession);
        pane.add(Box.createVerticalStrut(10));
        listSessionTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane listSessionScrollPane = getScrollPane(modelListSession, listSessionTable, true);
        pane.add(listSessionScrollPane); // Add bảng sesion

        // Hiển thị bảng căn giữa
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < listSessionTable.getColumnCount(); i++) {
            listSessionTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Button xóa suất.
        pane.add(Box.createVerticalStrut(10));
        JButton deleteSessionButton = new JButton("Xóa suất");
        deleteSessionButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        deleteSessionButton.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(deleteSessionButton);

        // TODO: Tạo combobox nhập thời gian bắt đầu sự kiện và độ dài sự kiện.
        pane.add(Box.createVerticalStrut(10));
        JLabel startTime = new JLabel("Thời gian bắt đầu: ");
        startTime.setFont(new Font(null, Font.BOLD, 14));
        // Tạo ComboBox cho giờ (0-23)
        JComboBox<Integer> hourStartComboBox = new JComboBox<>();
        for (int i = 0; i < 24; i++) {
            hourStartComboBox.addItem(i);
        }
        // Tạo ComboBox cho phút (0-59)
        JComboBox<Integer> minuteStartComboBox = new JComboBox<>();
        for (int i = 0; i < 60; i++) {
            minuteStartComboBox.addItem(i);
        }
        JPanel startTimePanel = new JPanel();
        startTimePanel.setMaximumSize(new Dimension(300, 50));
        startTimePanel.setBackground(Color.lightGray);
        startTimePanel.add(startTime);
        startTimePanel.add(hourStartComboBox);
        startTimePanel.add(new JLabel(" giờ "));
        startTimePanel.add(minuteStartComboBox);
        startTimePanel.add(new JLabel(" phút "));
        pane.add(startTimePanel);

        JLabel longTimeLabel = new JLabel("Độ dài sự kiện:    ");
        longTimeLabel.setFont(new Font(null, Font.BOLD, 14));
        // Tạo textfield cho giờ (chỉ cho phép nhập số).
        NumericTextField hourLongTextfield = new NumericTextField(4);
        // Tạo ComboBox cho phút (0-59)
        JComboBox<Integer> minuteLongCombobox = new JComboBox<>();
        for (int i = 0; i < 60; i++) {
            minuteLongCombobox.addItem(i);
        }
        JPanel endTimePanel = new JPanel();
        endTimePanel.setMaximumSize(new Dimension(300, 50));
        endTimePanel.setBackground(Color.lightGray);
        endTimePanel.add(longTimeLabel);
        endTimePanel.add(hourLongTextfield);
        endTimePanel.add(new JLabel(" giờ "));
        endTimePanel.add(minuteLongCombobox);
        endTimePanel.add(new JLabel(" phút "));
        pane.add(endTimePanel);
        pane.add(Box.createVerticalStrut(10));

        // Button thêm suất sự kiện.
        JButton addSessionButton = new JButton("Thêm suất");
        addSessionButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        addSessionButton.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(addSessionButton);
        pane.add(Box.createVerticalStrut(20));
        setDataSessionTable(); // Gọi hàm hiển thị dữ liệu.

        // TODO: Khi nhấn vào row nào trong bảng suất chiếu thì hiển thị danh sách khán đài của suất chiếu đó.
        listSessionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = listSessionTable.getSelectedRow();
                if (selectedRow != -1) {

                    // Xóa danh sách vị trí do chưa đổi suất chưa chọn khán đài.
                    mapSeatPanel.removeAll();
                    mapSeatPanel.revalidate();
                    mapSeatPanel.repaint();

                    // Lấy thời gian bắt đầu và kết thúc.
                    Object dataStart = listSessionTable.getValueAt(selectedRow, 0);
                    Object dataEnd = listSessionTable.getValueAt(selectedRow, 1);

                    // Chuyển thời gian từ String -> LocalTime.
                    LocalTime startTime1 = OrderMethod.convertToLocalTime(dataStart.toString());
                    LocalTime endTime = OrderMethod.convertToLocalTime(dataEnd.toString());

                    // Đặt lại list khán đài và hiển thị.
                    currentSession = sessionMethod.getSessionByTime(startTime1, endTime);
                    currentListAreaOfSession = areaMethod.getAreasBySessionID(currentSession.getID());
                    setDataAreaTable();
                }
            }
        });

        // TODO: Xử lý khi thêm 1 suất sự kiện.
        addSessionButton.addActionListener(e -> {

            // Lấy data người dùng nhập.
            String hStart = Objects.requireNonNull(hourStartComboBox.getSelectedItem()).toString();
            String mStart = Objects.requireNonNull(minuteStartComboBox.getSelectedItem()).toString();
            String hLong = hourLongTextfield.getText();
            String mLong = Objects.requireNonNull(minuteLongCombobox.getSelectedItem()).toString();

            if (Objects.equals(hLong, "")) { // Thông báo lỗi khi thiếu thông tin.
                JOptionPane.showMessageDialog(null, "Vui lòng chọn số giờ độ dài sự kiện.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {  // Xử lý khi đủ thông tin.
                LocalTime start = OrderMethod.convertToLocalTime(String.format("%s:%s", hStart, mStart));
                LocalTime longtime = OrderMethod.convertToLocalTime(String.format("%s:%s", hLong, mLong));
                Session s = new Session(sessionMethod.getNewID(), start, longtime); // Tạo suất sự kiện mới.
                if (sessionMethod.getSessions().contains(s)) { // Kiểm tra sự kiện đã tồn tại hay chưa.
                    JOptionPane.showMessageDialog(null, "Suất sự kiện đã tồn tại, vui lòng nhập lại thông tin thời gian.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else { // Nếu chưa tồn tại thì tiến hành thêm vào.
                    // Xử lý thêm ở phía server.
                    sessionMethod.addNewSession(s);
                    SessionFile.appendSessionToFile(s, sessionFileName);
                    Duration duration = Duration.between(LocalTime.MIDNIGHT, s.getTimeLong());
                    modelListSession.addRow(new Object[]{s.getTimeStart().toString(), (s.getTimeStart().plus(duration)).toString()});
                    // Broadcast cho toàn bộ client.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            BroadcastUpdateSession(s);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    executor.shutdown();
                }
            }
        });

        // TODO: Xóa 1 suất sự kiện khỏi list.
        deleteSessionButton.addActionListener(e -> {

            // Thông báo lỗi nếu chưa chọn suất xóa.
            if (currentSession == null || listSessionTable.getSelectedRow() < 0) {
                JOptionPane.showMessageDialog(null, "Vui lòng chọn suất bạn muốn xóa khỏi sự kiện.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                int response = JOptionPane.showConfirmDialog(null, "Nếu bạn muốn xóa suất, toàn bộ thông tin đặt chổ và khán đài cũng sẽ bị xóa, bạn muốn tiếp tục chứ?", "Xóa suất", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) { // Xác nhận lại.

                    // TODO: Tiến hành xóa theo thứ tự vị trí đặt (trong suất) -> khán đài (tron suất) -> suất.
                    List<Area> tmp = areaMethod.getAreasBySessionID(currentSession.getID());
                    for (Area a : tmp) {
                        seatMethod.deleteSeatByAreaID(a.getID());
                    }
                    SeatFile.writeSeatFile(seatMethod.getSeats(), seatFileName);
                    areaMethod.deleteAreaBySessionID(currentSession.getID());
                    AreaFile.writeAreaFile(areaMethod.getAreas(), areaFileName);
                    sessionMethod.deleteSessionByID(currentSession.getID());
                    SessionFile.writeSessionFile(sessionMethod.getSessions(), sessionFileName);

                    // Đặt lại dữ liệu ban đầu.
                    currentListSeatIsBookedOfArea = new ArrayList<>();
                    currentListAreaOfSession = new ArrayList<>();
                    setDataSessionTable();
                    setDataAreaTable();
                    mapSeatPanel.removeAll();
                    mapSeatPanel.revalidate();
                    mapSeatPanel.repaint();

                    // Broadcast đến toàn bộ server.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            BroadcastUpdateDeleteSession(currentSession);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    executor.shutdown();
                }

            }
        });

        return pane;
    }

    // TODO: Thiết lập giao diện bảng chứa list khán đài.
    private static JPanel getAreaListPanel() {

        // Panel chính dùng box layout.
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(10));

        // Tiêu đề bảng.
        JLabel listSessionLabel = new JLabel("Danh sách khu khán đài");
        listSessionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        listSessionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        listSessionLabel.setFont(new Font(null, Font.BOLD, 16));
        pane.add(listSessionLabel);
        pane.add(Box.createVerticalStrut(10));

        // Bảng chứa list khán đài.
        listAreaTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane listAreaScrollPane = getScrollPane(modelListArea, listAreaTable, false);
        pane.add(listAreaScrollPane);
        pane.add(Box.createVerticalStrut(10));

        // Button xóa.
        JButton deleteAreaButton = new JButton("Xóa khu khán đài");
        deleteAreaButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        deleteAreaButton.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(deleteAreaButton);
        pane.add(Box.createVerticalStrut(10));

        // Các textfield nhập liêuj cho việc thêm.
        JLabel ConfigNameLabel = new JLabel("Tên khán đài:              ");
        JTextField ConfigNameTextfield = new JTextField();
        ConfigNameTextfield.setPreferredSize(new Dimension(155, 25));
        JPanel ConfigNamePanel = new JPanel();
        ConfigNamePanel.setBackground(Color.lightGray);
        ConfigNamePanel.setMaximumSize(new Dimension(300, 30));
        ConfigNamePanel.add(ConfigNameLabel);
        ConfigNamePanel.add(ConfigNameTextfield);
        pane.add(ConfigNamePanel);

        JLabel ConfigPriceLabel = new JLabel("Giá vé (VNĐ):              ");
        NumericTextField ConfigPriceTextfield = new NumericTextField(15);
        ConfigPriceTextfield.setPreferredSize(new Dimension(155, 25));
        JPanel ConfigPricePanel = new JPanel();
        ConfigPricePanel.setBackground(Color.lightGray);
        ConfigPricePanel.setMaximumSize(new Dimension(300, 30));
        ConfigPricePanel.add(ConfigPriceLabel);
        ConfigPricePanel.add(ConfigPriceTextfield);
        pane.add(ConfigPricePanel);

        JLabel ConfigRowLabel = new JLabel("Số hàng:                      ");
        JLabel ConfigColumnLabel = new JLabel("Số cột ghế mỗi hàng: ");
        NumericTextField ConfigColumnTextfield = new NumericTextField(15);
        ConfigColumnTextfield.setPreferredSize(new Dimension(200, 25));
        NumericTextField ConfigRowTextfield = new NumericTextField(15);
        ConfigRowTextfield.setPreferredSize(new Dimension(200, 25));
        JPanel ConfigAreaPanel = new JPanel();
        ConfigAreaPanel.setBackground(Color.lightGray);
        ConfigAreaPanel.setMaximumSize(new Dimension(300, 80));

        ConfigAreaPanel.add(ConfigRowLabel);
        ConfigAreaPanel.add(ConfigRowTextfield);
        ConfigAreaPanel.add(ConfigColumnLabel);
        ConfigAreaPanel.add(ConfigColumnTextfield);
        pane.add(ConfigAreaPanel);

        // Button xử lý việc thêm khán đài.
        JButton addAreaButton = new JButton("Thêm khu khán đài");
        addAreaButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        addAreaButton.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(addAreaButton);
        pane.add(Box.createVerticalStrut(20));

        // Thiết lập căn giữa cho phần tử bảng.
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < listAreaTable.getColumnCount(); i++) {
            listAreaTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // TODO: Xử lý sự kiện chọn phần tử Area để hiện thị danh sách vị trí của khán đài.
        listAreaTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                indexRow = 0; // Đặt lại vị trí hiển thị.
                indexColumn = 0;
                int selectedRow = listAreaTable.getSelectedRow();
                if (selectedRow != -1) {
                    // Lấy dữ liệu.
                    Object dataName = listAreaTable.getValueAt(selectedRow, 0);
                    Object dataPrice = listAreaTable.getValueAt(selectedRow, 1);

                    // Lấy danh sách chổ đã đặt.
                    currentArea = areaMethod.getAreaByNameAndPrice(dataName.toString(), Integer.parseInt(dataPrice.toString()));
                    currentListSeatIsBookedOfArea = seatMethod.getSeatByAreaID(currentArea.getID());
                    setMapOfSeat(); // Hiển thị giao diện.
                }
            }
        });

        // TODO: Xử lý sự kiện nhấn nút thên khán đài vào suất chiếu hiện tại.
        addAreaButton.addActionListener(e -> {

            // Xử lý các lỗi thiếu dữ liệu.
            if (currentSession == null){
                JOptionPane.showMessageDialog(null, "Vui lòng suất sự kiện.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                if (Objects.equals(ConfigNameTextfield.getText(), "")){
                    JOptionPane.showMessageDialog(null, "Vui lòng nhập tên khu khán đài.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else if (Objects.equals(ConfigPriceTextfield.getText(), "")) {
                    JOptionPane.showMessageDialog(null, "Vui lòng nhập giá vé khu khán đài.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else if (Objects.equals(ConfigRowTextfield.getText(), "")) {
                    JOptionPane.showMessageDialog(null, "Vui lòng nhập số hàng ghế khu khán đài.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else if (Objects.equals(ConfigColumnTextfield.getText(), "")) {
                    JOptionPane.showMessageDialog(null, "Vui lòng nhập số ghế mỗi hàng khu khán đài.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Lấy dữ liệu.
                    String name = ConfigNameTextfield.getText();
                    int price = Integer.parseInt(ConfigPriceTextfield.getText());
                    int nRow = Integer.parseInt(ConfigRowTextfield.getText());
                    int nColumn = Integer.parseInt(ConfigColumnTextfield.getText());

                    // Tạo khán đài mới.
                    Area a = new Area(areaMethod.getNewID(), currentSession.getID(), name, price, nRow, nColumn);
                    if (currentListAreaOfSession.contains(a)) { // Kiểm tra đã tồn tại hay chưa.
                        JOptionPane.showMessageDialog(null, "Khán đài đã dùng trong suất này, vui lòng nhập lại tên khán đài.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Thêm khán đài.
                        areaMethod.addAreaToSession(a);
                        currentListAreaOfSession.add(a);
                        AreaFile.appendAreaToFile(a, areaFileName);
                        modelListArea.addRow(new Object[]{a.getName(), a.getPrice(), "(" + a.getNumberRow() + ", " + a.getNumberColumn() + ")"});
                        // Broadcast cho toàn bộ client.
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                BroadcastUpdateArea(a);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                        executor.shutdown();
                    }
                }

            }
        });

        // TODO: Xử lý sự kiện xóa một khu khán đài.
        deleteAreaButton.addActionListener(e -> {
            if (currentArea == null || listAreaTable.getSelectedRow() < 0) {
                JOptionPane.showMessageDialog(null, "Vui lòng chọn khán đài bạn muốn xóa khỏi suất sự kiện.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                int response = JOptionPane.showConfirmDialog(null,
                        "Nếu bạn muốn xóa khán đài, toàn bộ thông tin đặt chổ của khán đài cũng sẽ bị xóa, bạn muốn tiếp tục chứ?","Xóa khán đài", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) { // Xác nhận lại.

                    // TODO: Xóa theo thứ tự danh sách chổ đặt của khán đài -> khán đài.
                    seatMethod.deleteSeatByAreaID(currentArea.getID());
                    SeatFile.writeSeatFile(seatMethod.getSeats(), seatFileName);
                    areaMethod.deleteAreaByID(currentArea.getID());
                    AreaFile.writeAreaFile(areaMethod.getAreas(), areaFileName);
                    Area tmp = new Area(currentArea.getID(), currentArea.getSessionID(), currentArea.getName(), currentArea.getPrice(), currentArea.getNumberRow(), currentArea.getNumberColumn());
                    currentListSeatIsBookedOfArea = new ArrayList<>();
                    currentListAreaOfSession.removeIf(a -> a.getID() == currentArea.getID());

                    // Vẽ lại giao diện bảng.
                    setDataAreaTable();
                    mapSeatPanel.removeAll();
                    mapSeatPanel.revalidate();
                    mapSeatPanel.repaint();

                    // Broadcast đến client.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            BroadcastUpdateDeleteArea(tmp);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    executor.shutdown();
                }

            }
        });

        return pane;
    }

    // TODO: Xử lý hiện thị bảng danh sách chổ ngồi của 1 khán đài.
    public static void setMapOfSeat()
    {
        mapSeatPanel.removeAll();
        mapSeatPanel.revalidate();
        mapSeatPanel.repaint();
        mapSeatPanel.setLayout(new GridLayout(0, 10)); // Dừng grid layout mỗi hàng 10 vị trí được hiển thị.

        for (int i = indexRow * 10; i < (indexRow  + 1) * 10 ; i++) {
            for (int j = indexColumn * 10; j < (indexColumn + 1) * 10; j++) {
                if (i < currentArea.getNumberRow() && j < currentArea.getNumberColumn()) { // Các vị trí vẫn thuộc kích thuóc khán đài.
                    if (OrderMethod.isBookedSeatAtRowAndColumn(currentListSeatIsBookedOfArea,i+1, j+1)) { // Các vị trí đã được đặt.
                        JButton b = new JButton("<html>&#160;&#160;(" + (i + 1) + ", " + (j + 1) + ")<br>(đã đặt)</html>");
                        b.setMargin(new Insets(5, 0, 5, 0));
                        b.setBackground(Color.RED);
                        b.addActionListener(showInformationBookedSeat);
                        mapSeatPanel.add(b);
                    }
                    else { // Vị trí chưa bị đặt.
                        JButton b = new JButton("<html>(" + (i + 1) + ", " + (j + 1) + ")</html>");
                        b.setMargin(new Insets(5, 0, 5, 0));
                        b.addActionListener(showNotBooked);
                        mapSeatPanel.add(b);
                    }
                }
                else { // Các vị trí không thuộc kích thước khán đài.
                    JButton b = new JButton();
                    b.setMaximumSize(new Dimension(20, 20));
                    b.setBackground(Color.black);
                    b.setEnabled(false);
                    mapSeatPanel.add(b);
                }

            }
        }
    }

    // TODO: Sự kiện khi nhấn vào vị trí đã đặt -> hiển thị dữ liệu người đặt.
    public static ActionListener showInformationBookedSeat = e -> {
        // Lấy text button.
        JButton button = (JButton) e.getSource();
        String buttonText = button.getText();

        // Biểu thức chính quy để tìm các số trong dấu ngoặc
        Pattern pattern = Pattern.compile("\\((\\d+),\\s*(\\d+)\\)");
        Matcher matcher = pattern.matcher(buttonText);

        if (matcher.find()) { // Lấy ra các thông tin từ chuỗi -> lấy thông tin đó tìm tài khoản -> hiển thị.
            int row = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            Seat s = seatMethod.getSeatAtAreaInRowAndColumn(currentArea.getID(), row, column);
            Account a = accountMethod.getAccountByID(s.getAccountID());
            int response = JOptionPane.showConfirmDialog(null,
                    "<html>Vị trí đã được đặt bởi: " + a.getName() + "<br>" +
                            "Số điện thoại: " + a.getPhoneNumber()  + "<br><br>" +
                            "<p style=\"color: red;\">Bạn có muốn đặt lại thành vị trí trống không?</p></html>","Xóa khán đài", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                seatMethod.deleteSeatByID(s.getID());
                SeatFile.writeSeatFile(seatMethod.getSeats(), seatFileName);
                button.setBackground(null);
                currentListSeatIsBookedOfArea.remove(s);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        BroadcastUpdateSeat(s);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                executor.shutdown();
            }
        }
    };

    // TODO: Sự kiện nhấn button chưa được đặt.
    public static ActionListener showNotBooked = e -> JOptionPane.showMessageDialog(null, "Vị trí chưa được đặt", "Thông báo", JOptionPane.INFORMATION_MESSAGE);

    // TODO: Tạo và hiển thị giao diện.
    private static void CreateAndShowGUI() throws ParserConfigurationException {
        // Tạo luồng cho server giao tiếp client.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(()->{
            try {
                new Server();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        executor.shutdown();

        // Cài đặt frame hiển thị.
        JFrame frame = new JFrame();
        frame.setSize(1200, 700);
        frame.setTitle(" Quản Lý Bán Vé");
        frame.setContentPane(ServerPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        // TODO: Thực hiện đóng các kết nối socket khi tắt màn hình chính giao diện.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->{
                    try {
                        for (int i = 0; i < listSocketConnection.size(); i++) {
                            listSocketConnection.get(i).close();
                            listObjectInputStream.get(i).close();
                            listObjectOutputStream.get(i).close();
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                });
                executor.shutdown();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {

            try {
                CreateAndShowGUI();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }

        });
    }
}
