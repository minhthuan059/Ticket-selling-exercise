package OrderMethod;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: Lấy giá trị port từ file xml.
public class GetPortFromXmlFile {

    public static int getPortFromXmlFile(String filePath) {
        int port = -1;
        try {
            // Tạo đối tượng DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Tạo đối tượng DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Phân tích file XML và lấy đối tượng Document
            Document document = builder.parse(filePath);
            // Chuẩn hóa tài liệu XML
            document.getDocumentElement().normalize();
            // Lấy phần tử gốc
            Element root = document.getDocumentElement();
            // Lấy giá trị của phần tử <port>
            port = Integer.parseInt(root.getElementsByTagName("port").item(0).getTextContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return port;
    }

}
