// Client chỉ được truy cập 2 package dưới, không truy cập đến các package có làm việc với file dữ liệu.
import OrderMethod.GetPortFromXmlFile;
import OrderMethod.*;
import Objects.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Client {

    // Các thông tin kết nối đến server.
    private static final String address = "localhost";
    private static Socket socket;
    private static ObjectInputStream in = null;
    private static ObjectOutputStream out = null;

    // Các String command dùng để đánh dấu giao tiếp giữa client và server cho biết client hoặc server muốn làm gì.
    private static final String getListSessionFromServer = "GET_LIST_SESSION";
    private static final String getListAreaOfSesionFromServer = "GET_LIST_AREA_OF_SESSION";
    private static final String getListSeatOfAreaFromServer = "GET_LIST_SEAT_FROM_AREA";
    private static final String getAccountFromServer = "GET_ACCOUNT_BY_INFORMATION";
    private static final String updateAccountToServer = "UPDATE_ACCOUNT_BY_INFORMATION";
    private static final String postAccountBookSeatToServer = "POST_ACCOUNT_BOOK_SEAT";
    private static final String postDeleteBookedSeatToServer = "POST_DELETE_BOOKED_SEAT";
    private static final String broadcastBookedSeatToAllClient = "BROADCAST_UPDATE_BOOKED_SEAT_TO_ALL_CLIENT";
    private static final String broadcastUpdateAddSessionToAllClient = "BROADCAST_UPDATE_SESSION_TO_ALL_CLIENT";
    private static final String broadcastUpdateDeleteSessionToAllClient = "BROADCAST_UPDATE_DELETE_SESSION_TO_ALL_CLIENT";
    private static final String broadcastUpdateAddAreaToAllClient = "BROADCAST_UPDATE_AREA_TO_ALL_CLIENT";
    private static final String broadcastUpdateDeleteAreaToAllClient = "BROADCAST_UPDATE_DELETE_AREA_TO_ALL_CLIENT";
    private static final String broadcastUpdateSeatToAllClient = "BROADCAST_UPDATE_SEAT_TO_ALL_CLIENT";

    // Danh sách các biến hiện thời để qunar lý hiển thị lên giao diện.
    private static Session currentSession = null; //Suất chiếu đang chọn
    private static Area currentArea = null; // Khu vực khán đài đang chọn
    private static Account currentAccount = null; // Account của người dùng.
    private static List<Session> listAllSession = null; // Tất cả suất sự kiện.
    private static List<Area> currentListAreaOfSession = null; // Các khán đài thuộc suất sự kiện đang được chọn.
    private static List<Seat> currentListSeatIsBookedOfArea = null; // Các vị trí đã đặt của khán đài được chọn.
    private static List<Seat> listSeatBooked = new ArrayList<>(); // Danh sách vị trí đã đặt.
    private static List<Area> listAreaBooked = new ArrayList<>(); // Danh sách khán đài ứng với vị trí đã đặt.
    private static List<Session> listSessionBooked = new ArrayList<>(); // Danh sách suất ứng với vị trí được đặt.
    private static final List<Seat> listSeatPrepareBook = new ArrayList<>(); // Danh sách vị trí người dùng chuẩn bị đặt.
    private static final List<Area> listAreaPrepareBook = new ArrayList<>();
    // Các biến giao diện swing (giá trị thay nhiều lần do giao tiếp với server hoặc
    // lý do khác mà không cần có tương tác từ giao diện, hoặc có móc nối với nhều giá trị ngoài).

    // Bảng danh sách suất.
    private static final DefaultTableModel modelListSession = new DefaultTableModel();
    private static final JTable listSessionTable = new JTable(modelListSession) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    // Bảng danh sách vị trí người dùng đã đặt.
    private static final DefaultTableModel modelListBought = new DefaultTableModel();
    private static final JTable listBoughtTable = new JTable(modelListBought) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private static final JComboBox<Area> listAreaCombobox = new JComboBox<>(); // Combobox chứa danh sách khán đài của suất chiếu đang chọn.
    private static final JButton updatebutton = new JButton(); // Chứa sự kiện update thông tin đặt chổ của client.
    private  static final JPanel mapSeatPanel = new JPanel(); // Chứa grid button hiển thị vị trí.

    // Vị trí hiển thị.
    private static int indexRow = 0;
    private static int indexColumn = 0;

    private static final String CONFIG_FILE_PATH = "./port.xml";
    private static final int port = GetPortFromXmlFile.getPortFromXmlFile(CONFIG_FILE_PATH);

    // TODO: Khởi động luồng giao tiếp với server để nhận và gửi thông tin.
    public Client() throws IOException {
        socket = new Socket(address, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        ListenFromServer listen = new ListenFromServer(socket);
        listen.start();
    }

    // TODO: Tạo luồng xử lý các reponse nhận từ server.
    public static class ListenFromServer extends Thread {

        private final Socket socket;

        public ListenFromServer(Socket socket) {
            this.socket = socket;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    out.writeObject(getListSessionFromServer); // Ban đầu khi mở lên sẽ lấy list suất chiếu.
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            executor.shutdown();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String command = (String) in.readObject(); // Nhận lệnh.

                    if (Objects.equals(command, getListSessionFromServer)) { // Nhận danh sách suất chiếu và hiển thị.
                        listAllSession = (List<Session>) in.readObject();
                        SwingUtilities.invokeLater(Client::setDataSessionTable);
                    } else if (Objects.equals(command, getListAreaOfSesionFromServer)) { // Nhận danh sách khán đài của suất chiếu đang chọn và hiên thị.
                        currentListAreaOfSession = (List<Area>) in.readObject();
                        SwingUtilities.invokeLater(Client::setListAreaCombobox);
                    } else if (Objects.equals(command, getListSeatOfAreaFromServer)) { // Nhận danh sách vị trí đã đặt của khán đài đang chọn.
                        currentListSeatIsBookedOfArea = (List<Seat>) in.readObject();
                        SwingUtilities.invokeLater(Client::setMapOfSeat);
                    } else if (Objects.equals(command, getAccountFromServer)) { // Nhận thông tin tài khoản chi tiết từ server.
                        Object response = in.readObject();
                        if (response == null) { // Nếu server gửi null, tức là tài khoản chưa tồn tại -> server vừa tạo mới -> chưa có dữ liệu đặt chổ.
                            JOptionPane.showMessageDialog(null, "Tạo tài khoản mới thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            listSeatBooked = new ArrayList<>();
                            listAreaBooked = new ArrayList<>();
                            listSessionBooked = new ArrayList<>();
                        } else { // Trường hợp đã có tài khoản thì nhận tiếp dữ liệu đặt chổ.
                            JOptionPane.showMessageDialog(null, "Tìm tài khoản thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            listSeatBooked = (List<Seat>) response; // Danh sách chổ đã đặt.
                            if (!listSeatBooked.isEmpty()) {// Đã từng dặt chổ.
                                listAreaBooked = (List<Area>) in.readObject(); // Lấy danh sách khán đài có chổ đã đặt.
                                for (Area a : listAreaBooked) {  // Lấy danh sách suất chiếu ứng với khán đài.
                                    listSessionBooked.add(OrderMethod.getSessionByIDInList(listAllSession, a.getSessionID()));
                                }
                            } else { // Khi có tài khoản nhưng chưa từng đặt chổ.
                                listSessionBooked = new ArrayList<>();
                                listAreaBooked = new ArrayList<>();
                            }
                        }
                        SwingUtilities.invokeLater(Client::setBookedSeatTable); // Hiển thị lên giao diện.
                    } else if (Objects.equals(command, postAccountBookSeatToServer)) { // Nhận hồi đáp từ server khi client đã yêu cầu đặt chổ.
                        boolean result = (boolean) in.readObject(); // Nhận kết quả.
                        if (result) { // Nếu đặt thành công thì load lại giao diện bảng đặt chổ.
                            ExecutorService ex = Executors.newSingleThreadExecutor();
                            ex.submit(() -> updatebutton.doClick());
                            ex.shutdown();
                        }
                    } else if (Objects.equals(command, postDeleteBookedSeatToServer)) { // Nhận hồi đáp từ server khi client đã yêu cầu hủy đặt chổ 1 vị trí.
                        boolean result = (boolean) in.readObject();
                        if (result) { // Nếu hủy thành công load lại giao diện bảng đặt chổ.
                            ExecutorService ex = Executors.newSingleThreadExecutor();
                            ex.submit(() -> updatebutton.doClick());
                            ex.shutdown();
                        }
                    } else if (Objects.equals(command, updateAccountToServer)) { // Nhận hồi đáp từ server khi client yêu cầu nhận lại thông tin tài khoản 1 lần nữa.
                        // TODO: Xử lý như cách làm với nhận thông tin tài khoản nhưng trường hợp này không cần hiển thị các thông báo.
                        Object response = in.readObject();
                        listSeatBooked = (List<Seat>) response;
                        if (!listSeatBooked.isEmpty()) {
                            listAreaBooked = (List<Area>) in.readObject();
                            for (Area a : listAreaBooked) {
                                listSessionBooked.add(OrderMethod.getSessionByIDInList(listAllSession, a.getSessionID()));
                            }
                        } else {
                            listSessionBooked = new ArrayList<>();
                            listAreaBooked = new ArrayList<>();
                        }
                        SwingUtilities.invokeLater(Client::setBookedSeatTable);
                    } else if (Objects.equals(command, broadcastBookedSeatToAllClient)) { // Nhận thông tin broadcast suất chiếu khi có 1 client nào đó vừa đặt chổ.
                        // TODO: Khi nhận thông tin có sự thay đổi về chổ đã đặt từ server, client gửi yêu cầu về các vị tri đã dặt 1 lần nữa.
                        in.readObject();
                        currentArea = (Area) listAreaCombobox.getSelectedItem();
                        if (currentArea != null) {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.submit(() -> { // Luồng gửi lại yêu cầu về vị trí đã đặt.
                                try {
                                    out.writeObject(getListSeatOfAreaFromServer);
                                    out.flush();
                                    out.writeObject(currentArea);
                                    out.flush();
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            });
                            executor.shutdown();
                        }
                    } else if (Objects.equals(command, broadcastUpdateAddSessionToAllClient)) { // Nhận thông tin broadcast về việc số suất chiếu đã thay đổi.
                        Session s = (Session) in.readObject();
                        Duration duration = Duration.between(LocalTime.MIDNIGHT, s.getTimeLong());
                        listAllSession.add(s);
                        modelListSession.addRow(new Object[]{s.getTimeStart().toString(), (s.getTimeStart().plus(duration)).toString()}); // Thêm vào bảng
                    } else if (Objects.equals(command, broadcastUpdateDeleteSessionToAllClient)) {  // Nhận thông tin có suất chiếu bị xóa đi từ server.
                        Session s = (Session) in.readObject();
                        int index = listAllSession.indexOf(s);
                        listAllSession.removeIf(ss -> s.getID() == ss.getID());
                        if (!listSeatPrepareBook.isEmpty()) {  // Xóa các vị trí định đặt thuộc suất đã xóa khỏi list.
                            for (int i = listSeatPrepareBook.size() - 1; i > -1; i++) {
                                if (listAreaPrepareBook.get(i).getSessionID() == s.getID()) {
                                    listAreaPrepareBook.remove(i);
                                }
                            }
                        }
                        if (currentSession != null && listSessionTable.getSelectedRow() > -1) { // Nếu list suất chiếu không phải null đã có suất đang chọn hiển thị.
                            if (currentSession.getID() == s.getID()) { // Nếu suất bị xóa trùng với suát đang chọn hiện thị.
                                modelListSession.removeRow(listSessionTable.getSelectedRow()); // Xóa toàn bộ đang hiện thị.
                                listAreaCombobox.removeAllItems();
                                mapSeatPanel.removeAll();
                                mapSeatPanel.repaint();
                                mapSeatPanel.revalidate();
                            } else {
                                modelListSession.removeRow(index); // Nếu suất bị xóa không trùng suất chiếu hiện tại -> chỉ xóa dòn có xuất chiếu.
                            }
                        } else { // Nếu list rỗng hoặc chưa chọn thì gọi hàm cập nhật lại toàn bảng.
                            SwingUtilities.invokeLater(Client::setDataSessionTable);
                        }
                        if (currentAccount != null) { // Update lại thông tin.
                            ExecutorService ex = Executors.newSingleThreadExecutor();
                            ex.submit(() -> updatebutton.doClick());
                            ex.shutdown();
                        }
                    } else if (Objects.equals(command, broadcastUpdateAddAreaToAllClient)) { // Khi server broadcast có khán đài mới được thêm vào.
                        Area tmp = (Area) in.readObject();
                        if (currentSession != null && tmp.getSessionID() == currentSession.getID()) { // Nếu khán đài thuộc session đang chọn hiển thị.
                            currentListAreaOfSession.add(tmp);
                            listAreaCombobox.addItem(tmp);
                        }
                    } else if (Objects.equals(command, broadcastUpdateDeleteAreaToAllClient)) { // Khi server broadcast có khán đài vừa bị xóa.
                        Area tmp = (Area) in.readObject();
                        currentListAreaOfSession.remove(tmp); // Xóa khán đài
                        if (!listSeatPrepareBook.isEmpty()) // Xóa các vị trí định đạt thuộc khán đài khỏi list.
                            listSeatPrepareBook.removeIf(s->s.getAreaID()==tmp.getID());
                        if (currentSession != null && currentSession.getID() == tmp.getSessionID()) { // Nếu Area định xóa thuộc Session đang hiển thị.
                            if (!currentListAreaOfSession.isEmpty()) { // Nếu sau khi vẫn còn còn Area khác.
                                if (Objects.equals(listAreaCombobox.getSelectedItem(), tmp)) { // Xóa đúng area đang hiển thị.
                                    SwingUtilities.invokeLater(Client::setListAreaCombobox);
                                    listAreaCombobox.setSelectedIndex(0);
                                    currentArea = (Area) listAreaCombobox.getSelectedItem();
                                    SwingUtilities.invokeLater(Client::setMapOfSeat);
                                } else { // Xóa ngay Area khác area đang chọn.
                                    listAreaCombobox.removeItem(tmp);
                                }
                            } else { // Nếu sau khi xóa không còn Area nào.
                                mapSeatPanel.removeAll();
                                mapSeatPanel.repaint();
                                mapSeatPanel.revalidate();
                                listAreaCombobox.removeAllItems();
                            }
                        }
                        if (currentAccount != null) { // Update cho trường hợp chổ đã đặt bị xóa cùng với area.
                            ExecutorService ex = Executors.newSingleThreadExecutor();
                            ex.submit(() -> updatebutton.doClick());
                            ex.shutdown();
                        }
                    } else if (Objects.equals(command, broadcastUpdateSeatToAllClient)) { // Trường hợp có chổ đã đặt của client khác đã hủy hoặc server tự hủy.
                        Seat s = (Seat) in.readObject();
                        if (currentArea != null && s.getAreaID() == currentArea.getID()) { // Kiểm tra chổ bị hủy có thuộc khán đài đang hiênt hị hay không.
                            currentListSeatIsBookedOfArea.removeIf(se -> se.getID() == s.getID()); // Nếu có thì xóa khỏi list và hiển thị lại.
                            SwingUtilities.invokeLater(Client::setMapOfSeat);
                        }
                        if (currentAccount != null) {
                            ExecutorService ex = Executors.newSingleThreadExecutor();
                            ex.submit(() -> updatebutton.doClick());
                            ex.shutdown();
                        }
                    }
                } catch (IOException e) {
                    try {
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
    }

    // TODO: Khởi tạo giao diện cho Client và gửi các request giao tiếp với server trong các sự kiện.
    private static JPanel ClientPanel() {

        // Lấy kích thước thiết bị
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        // Main panel với box layout.
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Title Panel
        JLabel title = new JLabel("QUẢN LÝ MUA VÉ");
        title.setFont(new Font(null, Font.BOLD, 20));
        JPanel titlePanel = new JPanel();
        titlePanel.add(title);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.setMaximumSize(new Dimension(screenWidth, 50));
        mainPanel.add(titlePanel);

        // Panel chứa nội dung.
        JPanel contentPanel = new JPanel();
        mainPanel.add(contentPanel);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

        // Panel thục thi các hành động đặt vé.
        JPanel leftPanel = getBuyTicketPanel();
        leftPanel.setPreferredSize(new Dimension(400, screenHeight));
        leftPanel.setMaximumSize(new Dimension(400, screenHeight));
        leftPanel.setBackground(Color.LIGHT_GRAY);

        // Panel chứa hiển thị chổ.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setMaximumSize(new Dimension(screenWidth-400, screenHeight));

        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(leftPanel);
        contentPanel.add(Box.createHorizontalStrut(10));
        contentPanel.add(rightPanel);
        contentPanel.add(Box.createHorizontalStrut(10));

        listAreaCombobox.setMaximumSize(new Dimension(screenWidth-600, 25));
        rightPanel.add(listAreaCombobox);

        // Căn giữa cho phần tử bảng.
        listAreaCombobox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        // Sự kiện chọn Area trên combobox.
        listAreaCombobox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                indexRow = 0; // Đặt lại iến vị trí hiển thị.
                indexColumn = 0;
                currentArea = (Area) listAreaCombobox.getSelectedItem(); // Đặt lại khán đài chọn.
                mapSeatPanel.removeAll(); // Xóa panel hiển thị vị trí.
                mapSeatPanel.repaint();
                mapSeatPanel.revalidate();
                if (currentArea != null) { // Gửi request yêu cầu thông tin về các vị trí được đặt trong aream hiện tại.
                    ExecutorService executor1 = Executors.newSingleThreadExecutor();
                    executor1.submit(()->{
                        try {
                            out.writeObject(getListSeatOfAreaFromServer);
                            out.flush();
                            out.writeObject(currentArea);
                            out.flush();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    executor1.shutdown();
                }
            }
        });

        // Chứa danh sách vị trí hiển thị.
        JPanel mapPanel = new JPanel();
        mapPanel.setLayout(new BorderLayout());
        mapPanel.setMaximumSize(new Dimension(screenWidth-600, screenHeight));

        // Đặt các biến vị trí hiển thị.
        mapPanel.add(mapSeatPanel, BorderLayout.CENTER);
        JButton topButton = new JButton("^");
        topButton.setBackground(Color.lightGray);
        JButton bottomButton = new JButton("v");
        bottomButton.setBackground(Color.lightGray);
        JButton leftButton = new JButton("<");
        leftButton.setMargin(new Insets(0, 7, 0, 7));
        leftButton.setBackground(Color.lightGray);
        JButton rightButton = new JButton(">");
        rightButton.setBackground(Color.lightGray);
        rightButton.setMargin(new Insets(0, 7, 0, 7));

        mapPanel.add(topButton, BorderLayout.PAGE_START);
        mapPanel.add(bottomButton, BorderLayout.PAGE_END);
        mapPanel.add(leftButton, BorderLayout.LINE_START);
        mapPanel.add(rightButton, BorderLayout.LINE_END);
        rightPanel.add(mapPanel);

        // Các sự kiện nhấn làm thay đổi biến vị trí hiển thị.
        topButton.addActionListener(e -> {
            // Xử lý sự kiện khi button được nhấn
            if (currentArea != null && indexRow > 0){
                indexRow--;
                setMapOfSeat();
            }
        });
        bottomButton.addActionListener(e -> {
            // Xử lý sự kiện khi button được nhấn
            if (currentArea != null && (indexRow < currentArea.getNumberRow()/10 - 1 || (indexRow == currentArea.getNumberRow()/10 - 1 && currentArea.getNumberRow()%10 != 0))) {
                indexRow++;
                setMapOfSeat();
            }
        });
        leftButton.addActionListener(e -> {
            // Xử lý sự kiện khi button được nhấn
            if (currentArea != null && indexColumn > 0){
                indexColumn--;
                setMapOfSeat();
            }
        });

        rightButton.addActionListener(e -> {
            // Xử lý sự kiện khi button được nhấn
            if (currentArea != null && (indexColumn < currentArea.getNumberColumn()/10 - 1 || (indexColumn == currentArea.getNumberColumn()/10 - 1 && currentArea.getNumberColumn()%10 != 0))) {
                indexColumn++;
                setMapOfSeat();
            }

        });

        return mainPanel;
    }

    // Bảng suất chiếu.
    private static JScrollPane getSessionScrollPane() {
        if (listSessionTable.getColumnCount() == 0) {
            modelListSession.addColumn("Bắt đầu");
            modelListSession.addColumn("Kết thúc");
            listSessionTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Cột ID
            listSessionTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Cột First NamelistTable.getColumnModel().getColumn(2).setPreferredWidth(50); // Cột Last Nam
            JScrollPane listScrollPane = new JScrollPane(listSessionTable);
            listScrollPane.setPreferredSize(new Dimension(500, 300));
            listScrollPane.setMaximumSize(new Dimension(500, 500));
            return listScrollPane;
        } else {
            return null;
        }
    }

    // Bảng danh sách vị trí đã đặt.
    private static JScrollPane getBoughtScrollPane() {
        if (listBoughtTable.getColumnCount() == 0) {
            modelListBought.addColumn("Suất chiếu");
            modelListBought.addColumn("Khán đài");
            modelListBought.addColumn("Ghế");
            modelListBought.addColumn("Giá");
            listBoughtTable.getColumnModel().getColumn(0).setPreferredWidth(100);
            listBoughtTable.getColumnModel().getColumn(1).setPreferredWidth(100);
            listBoughtTable.getColumnModel().getColumn(2).setPreferredWidth(100);
            listBoughtTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            JScrollPane listScrollPane = new JScrollPane(listBoughtTable);
            listScrollPane.setPreferredSize(new Dimension(500, 300));
            listScrollPane.setMaximumSize(new Dimension(500, 500));
            return listScrollPane;
        } else {
            return null;
        }
    }

    // Panel quản lý đặt vé.
    private static JPanel getBuyTicketPanel()  {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(Box.createVerticalStrut(10));

        JLabel listSession = new JLabel("        Danh sách suất chiếu");
        listSession.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        listSession.setHorizontalAlignment(SwingConstants.CENTER);
        listSession.setFont(new Font(null, Font.BOLD, 16));
        pane.add(listSession);
        pane.add(Box.createVerticalStrut(10));

        // Thêm bảng chứa list session.
        listSessionTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane listSessionScrollPane = getSessionScrollPane();
        pane.add(listSessionScrollPane);
        pane.add(Box.createVerticalStrut(10));
        JLabel listBought = new JLabel("        Danh sách đã mua");
        listBought.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        listBought.setHorizontalAlignment(SwingConstants.CENTER);
        listBought.setFont(new Font(null, Font.BOLD, 16));
        pane.add(listBought);
        pane.add(Box.createVerticalStrut(10));

        listBoughtTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane listBoughtScrollPane = getBoughtScrollPane();
        pane.add(listBoughtScrollPane);
        pane.add(Box.createVerticalStrut(5));

        JButton deleteBookedButton = new JButton("Hủy đặt vé");
        deleteBookedButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        deleteBookedButton.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(deleteBookedButton);
        pane.add(Box.createVerticalStrut(10));

        JLabel inputNameLabel = new JLabel("Nhập họ và tên: ");
        inputNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        inputNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(inputNameLabel);
        pane.add(Box.createVerticalStrut(5));

        JTextField inputNameTextfield = new JTextField();
        inputNameTextfield.setPreferredSize(new Dimension(400, 25));
        inputNameTextfield.setMaximumSize(new Dimension(400, 25));
        pane.add(inputNameTextfield);
        pane.add(Box.createVerticalStrut(5));

        JLabel inputPhonenumberLabel = new JLabel("Nhập số điện thoại: ");
        inputPhonenumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Căn giữa theo chiều ngang
        inputPhonenumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pane.add(inputPhonenumberLabel);
        pane.add(Box.createVerticalStrut(5));

        NumericTextField inputPhonenumberTextfield = new NumericTextField(40);
        inputPhonenumberTextfield.setMaximumSize(new Dimension(400, 25));
        pane.add(inputPhonenumberTextfield);
        pane.add(Box.createVerticalStrut(10));

        JButton findAccountButton = new JButton("Định danh tài khoản");
        JPanel boughtPanel = new JPanel();
        boughtPanel.add(findAccountButton);
        JButton bookedButton = new JButton("Xác nhận đặt vé");
        bookedButton.setEnabled(false);
        boughtPanel.add(bookedButton);
        boughtPanel.setBackground(Color.lightGray);
        pane.add(boughtPanel);
        pane.add(Box.createVerticalStrut(10));

        // Căn giữa trong bảng.
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < listSessionTable.getColumnCount(); i++) {
            listSessionTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Căn giữa trong bảng.
        DefaultTableCellRenderer _centerRenderer = new DefaultTableCellRenderer();
        _centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < listBoughtTable.getColumnCount(); i++) {
            listBoughtTable.getColumnModel().getColumn(i).setCellRenderer(_centerRenderer);
        }

        // TODO: Xử lý khi chọn sesion trong table để hiẹn thị.
        listSessionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = listSessionTable.getSelectedRow();
                if (selectedRow != -1) {
                    mapSeatPanel.removeAll();
                    mapSeatPanel.revalidate();
                    mapSeatPanel.repaint();

                    // Lấy dữ liệu từ bảng.
                    Object dataStart = listSessionTable.getValueAt(selectedRow, 0);
                    Object dataEnd = listSessionTable.getValueAt(selectedRow, 1);

                    // Convert thành LocalTime.
                    LocalTime startTime = OrderMethod.convertToLocalTime(dataStart.toString());
                    LocalTime endTime = OrderMethod.convertToLocalTime(dataEnd.toString());
                    currentSession = OrderMethod.getSessionByTimeInList(listAllSession, startTime, endTime);
                    currentArea = (Area) listAreaCombobox.getSelectedItem();

                    // Gửi broadcast cho toàn bộ client.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(()->{
                        try {
                            out.writeObject(getListAreaOfSesionFromServer);
                            out.flush();
                            out.writeObject(currentSession);
                            out.flush();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    executor.shutdown();
                    listAreaCombobox.setSelectedIndex(listAreaCombobox.getSelectedIndex());
                }
            }
        });

        // Nhấn vào button định danh.
        findAccountButton.addActionListener(e -> {
            // Xử lý các lỗi.
            if (inputNameTextfield.getText().replace(" ", "").replace("\n", "").isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập tên của bạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else if (inputPhonenumberTextfield.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập số điện thoại của bạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else { // Khi xác ssinhj không có lỗi nhập liệu.
                String name = inputNameTextfield.getText();
                String numberphone = inputPhonenumberTextfield.getText();
                bookedButton.setEnabled(true); // Cho phép nhấn nút đặt vé.
                currentAccount = new Account(name, numberphone); // Tạo tài khoản.

                // Lấy thông tin về tài khoản từ server.
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->{
                    try {
                        out.writeObject(getAccountFromServer);
                        out.flush();
                        out.writeObject(currentAccount);
                        out.flush();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                executor.shutdown();
            }
        });

        // Update thông tin đặt vé mới từ người dùng -> gửi yêu cầu để lấy dữ liệu.
        updatebutton.addActionListener(e -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(()->{
                try {
                    out.writeObject(updateAccountToServer);
                    out.flush();
                    out.writeObject(currentAccount);
                    out.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            executor.shutdown();
        });

        // TODO: Xử lý sự kiện nhấn nút.
        bookedButton.addActionListener(e -> {
            List<Seat> tmp = new ArrayList<>(listSeatPrepareBook); // Lấy list đăng ký.
            listSeatPrepareBook.clear(); // reset lại list.
            listAreaPrepareBook.clear();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(()->{ // Gửi yêu cầu cho server.
                try {
                    out.writeObject(postAccountBookSeatToServer);
                    out.flush();
                    out.writeObject(currentAccount);
                    out.flush();
                    out.writeObject(tmp);
                    out.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            executor.shutdown();
        });

        // Xóa đặt chổ.
        deleteBookedButton.addActionListener(e -> {
            int selectedRow = listBoughtTable.getSelectedRow();
            if (selectedRow < 0) { // Chắc chắn người dùng đa chọn vị trí xóa.
                JOptionPane.showMessageDialog(null, "Vui lòng chọn vị trí bạn muốn hủy đặt vé.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                Seat se = (Seat) listBoughtTable.getValueAt(selectedRow, 2);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->{ // Gửi yeu cầu cho server.
                    try {
                        out.writeObject(postDeleteBookedSeatToServer);
                        out.flush();
                        out.writeObject(se);
                        out.flush();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                executor.shutdown();
            }
        });

        return pane;
    }

    // Đặt data hiển thị cho bảng suất chiếu.
    public static void setDataSessionTable(){
        modelListSession.setRowCount(0);
        for (Session session : listAllSession) {
            Duration duration = Duration.between(LocalTime.MIDNIGHT, session.getTimeLong());
            modelListSession.addRow(new Object[]{session.getTimeStart().toString(), (session.getTimeStart().plus(duration)).toString()});
        }
    }

    // Đặt dữ liệu cho combobox chứa danh sách khán đài.
    public static void setListAreaCombobox(){
        listAreaCombobox.removeAllItems();
        for (Area a : currentListAreaOfSession) {
            listAreaCombobox.addItem(a);
        }
    }

    // Đặt dữ liệu cho bảng chứa danh sách chỗ người dùng đã đặt.
    public static void setBookedSeatTable() {
        modelListBought.setRowCount(0);
        if (listSeatBooked != null && !listSeatBooked.isEmpty()) {
            for (int i = 0; i < listSeatBooked.size(); i++) {
                Session ss = listSessionBooked.get(i);
                Area aa = listAreaBooked.get(i);
                Seat se = listSeatBooked.get(i);
                modelListBought.addRow(new Object[]{ss, aa.getName(), se, aa.getPrice()});
            }
        }
    }

    // Hiển thị danh sách vị trí lên giao diện.
    public static void setMapOfSeat() {
        mapSeatPanel.removeAll();
        mapSeatPanel.repaint();
        mapSeatPanel.revalidate();
        mapSeatPanel.setLayout(new GridLayout(0, 10));

        if (currentArea != null && currentListAreaOfSession != null && currentArea.getSessionID() == currentSession.getID()) {
            for (int i = indexRow * 10; i < (indexRow  + 1) * 10 ; i++) {
                for (int j = indexColumn * 10; j < (indexColumn + 1) * 10; j++) {
                    if (i < currentArea.getNumberRow() && j < currentArea.getNumberColumn()){ // Trường hợp vị trí thuộc curent area.
                        // Trường hợp vị trí đã được đặt.
                 (OrderMethod.isBookedSeatAtRowAndColumn(currentListSeatIsBookedOfArea,i+1, j+1)){
                            JButton b = new JButton("<html>&#160;&#160;(" + (i + 1) + ", " + (j + 1) + ")<br>(đã đặt)</html>");
                            b.setMargin(new Insets(5, 0, 5, 0));
                            b.setPreferredSize(new Dimension(10, 10));
                            b.setBackground(Color.red);
                            b.setEnabled(false);
                            if                mapSeatPanel.add(b);
                        } else {  // Trường hợp vị trí chưa được đặt.
                            JButton b = new JButton("(" + (i + 1) + ", " + (j + 1) + ")");
                            b.setPreferredSize(new Dimension(10, 10));
                            b.setMargin(new Insets(5, 0, 5, 0));
                            b.addActionListener(bookSeat);
                            mapSeatPanel.add(b);
                            if (listSeatPrepareBook.contains(new Seat(-1, -1, currentArea.getID(), i + 1, j + 1))) {
                                b.setBackground(Color.green);
                            }
                        }
                    } else { // Trường hợp vị trí không thuộc curent area.
                        JButton b = new JButton();
                        b.setMaximumSize(new Dimension(20, 20));
                        b.setBackground(Color.black);
                        b.setEnabled(false);
                        mapSeatPanel.add(b);
                    }
                }
            }
        }
    }

    // Sự kiện cho việc nhấn vào vị trí trống để đặt chổ.
    public static ActionListener bookSeat = e -> {
        JButton b = (JButton) e.getSource();
        String text = b.getText();
        text = text.replace("(", "").replace(")", ""); // Loại bỏ dấu ngoặc
        String[] numbers = text.split(", "); // Tách chuỗi để lấy các số

        // Chuyển đổi chuỗi thành số nguyên
        int row = Integer.parseInt(numbers[0]);
        int column = Integer.parseInt(numbers[1]);

        if (b.getBackground() == Color.green) { // Khi đã nhấn trước đó.
            b.setBackground(null);
            listSeatPrepareBook.remove(new Seat(-1, -1, currentArea.getID(), row, column));
            listAreaPrepareBook.remove(currentArea);
        } else { // Khi chưa từng được nhấn trước đó.
            b.setBackground(Color.green);
            listSeatPrepareBook.add(new Seat(-1, -1, currentArea.getID(), row, column));
            listAreaPrepareBook.add(currentArea);
        }
    };

    // TODO: Tạo giao diện hiển thị và luồng để lắng nghe server.
    private static void CreateAndShowGUI() throws IOException, ClassNotFoundException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                new Client();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        executor.shutdown();
        JFrame frame = new JFrame();
        frame.setSize(1200, 700);
        frame.setTitle(" Quản Lý Mua Vé");
        frame.setContentPane(ClientPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        // TODO: Thực hiện đóng các kết nối socket khi tắt màn hình chính giao diện.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->{
                    try {
                        out.writeObject("QUIT");
                        if (in != null)
                            in.close();
                        if (out != null)
                            out.close();
                        if (socket != null)
                            socket.close();
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
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
