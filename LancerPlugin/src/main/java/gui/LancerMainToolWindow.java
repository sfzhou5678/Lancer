package gui;

import com.intellij.openapi.wm.ToolWindow;
import component.SnippetJTable;
import component.TextAreaEditor;
import component.TextAreaRenderer;
import http.LancerHttpClient;
import javafx.util.Pair;
import slp.core.infos.MethodInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LancerMainToolWindow {
    private JPanel mainPanel;

    private JTabbedPane tabbedPane;

    private JScrollPane scroll;
    private JLabel labelWelcomeImage;
    private JTextField someSlogansTextField;

    String ID_MAIN_COLUMN = "Relevant Snippets";
    Object[] columnIdentifiers = new Object[]{ID_MAIN_COLUMN};
    private DefaultTableModel tableModel;
    private SnippetJTable table;

    public LancerMainToolWindow(ToolWindow toolWindow) {
        this.initView();
    }

    public void initView() {
        tableModel = new DefaultTableModel();
        initData();
        try {
            BufferedImage myPicture = ImageIO.read(this.getClass().getResource("/images/welcome-bg.jpg"));
//            BufferedImage myPicture = ImageIO.read(new File("C:\\Users\\hasee\\Desktop\\aaaa\\timg.jpg"));
            labelWelcomeImage.setIcon(new ImageIcon(myPicture));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateView(List<Pair<MethodInfo, Double>> methodInfoList) {
        if (methodInfoList == null || methodInfoList.size() == 0) {
            // do something
        } else {
            List<String> texts = new ArrayList<>();
            for (int i = 0; i < methodInfoList.size(); i++) {
                MethodInfo methodInfo = methodInfoList.get(i).getKey();
                Double score = methodInfoList.get(i).getValue();
                try {
                    StringBuilder stringBuilder = new StringBuilder();
//                    if (!methodInfo.getDocComments().isEmpty()) {
//                        stringBuilder.append("\t/**" + methodInfo.getDocComments() + "*/\n");
//                    }
//                    stringBuilder.append("Score:" + String.valueOf(score) + "\n");  // FIXME: integrate this into GUI component
                    stringBuilder.append(String.join("\n", methodInfo.getLineCodes()));
                    texts.add(stringBuilder.toString());
                } catch (Exception e) {
                }
            }
            Object[][] dataVector = new Object[texts.size()][1];
            for (int i = 0; i < texts.size(); i++) {
                dataVector[i][0] = texts.get(i);
            }
            tableModel.setDataVector(dataVector, columnIdentifiers);

            table = new SnippetJTable(tableModel);
            table.getColumn(ID_MAIN_COLUMN).setCellRenderer(new TextAreaRenderer());
            table.getColumn(ID_MAIN_COLUMN).setCellEditor(new TextAreaEditor());

            scroll.setViewportView(table);
        }
    }

    private LancerHttpClient httpClient;

    @Deprecated
    private void updateData() {
//  codes for BCB eval
//        if (httpClient == null) {
//            httpClient = new LancerHttpClient("localhost", 58362);
//        }
//        List<MethodInfo> methodInfoList = httpClient.showNextExample();
//        updateView(methodInfoList);
    }

    private void initData() {
        // TODO: 2019/4/24 welcome page or something else?
//        String tokens = "private void clickButtonAt(Point point) {\n" +
//                "        int index = jlist.locationToIndex(point);\n" +
//                "        PanelItem item = (PanelItem) jlist.getModel().getElementAt(index);\n" +
//                "        item.getButton().doClick();\n" +
//                "    }";
//        Object[][] dataVector = new Object[][]{{tokens}, {tokens}, {tokens}, {tokens}};

        String tokens = "";
        Object[][] dataVector = new Object[][]{{tokens}};

        tableModel.setDataVector(dataVector, columnIdentifiers);

        table = new SnippetJTable(tableModel);
        table.getColumn(ID_MAIN_COLUMN).setCellRenderer(new TextAreaRenderer());
        table.getColumn(ID_MAIN_COLUMN).setCellEditor(new TextAreaEditor());

        scroll.setViewportView(table);
    }


    public JComponent getContent() {
        return mainPanel;
    }

}
