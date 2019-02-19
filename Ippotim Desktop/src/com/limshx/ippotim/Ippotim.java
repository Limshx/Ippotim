package com.limshx.ippotim;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

//import com.apple.eawt.Application;

class Ippotim extends JFrame {
    static String homeDirectory = System.getProperty("user.dir") + "/";
    private static File openedFile;
    private static int screenWidth, screenHeight;

    private Ippotim(String s) {
        super(s);
    }

    private static void initScreenSize() {
        screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    static void setWindowCenter(JFrame jFrame) {
        jFrame.setLocation(screenWidth / 2 - jFrame.getWidth() / 2, screenHeight / 2 - jFrame.getHeight() / 2);
    }

    private static boolean isSystemFile(String fileName) {
        return fileName.equals("ippotim.properties") || fileName.equals("ippotim.output");
    }

    public static void main(String[] args) {
        String osName = System.getProperty("os.name");
        File opengl = new File(System.getProperty("user.dir") + "/OpenGL");
        if (osName.contains("Linux")) {
            if (opengl.exists()) {
                // 开启硬件加速，启动参数里加-Dsun.java2d.opengl=true也行，但是当然还是代码里加好。
                // 考虑到Linux下开启加速可能会出现各种问题，而Windows和macOS下应该是由于图形界面接口比较统一稳定所以JDK对绘图进行了默认加速，Windows下像这样开启加速反而也会出问题，故代码里去掉加速设置，Linux下可按需通过启动参数开启加速。
                if (!osName.contains("Windows")) {
                    System.setProperty("sun.java2d.opengl", "true");
                }
            }
        }
        initScreenSize();
        Ippotim ippotim = new Ippotim("The Ippotim Programming Language");
        Image image = null;
        try {
            image = ImageIO.read(ippotim.getClass().getResource("ippotim.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ippotim.setIconImage(image);
        // macOS下菜单栏放到全局菜单栏
        if (osName.contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
//            Application.getApplication().setDockIconImage(image);
        }
        DrawTable drawTable = new DrawTable();
        JMenuBar jMenuBar = new JMenuBar();
        JMenu jMenu;
        JMenuItem[] jMenuItems = new JMenuItem[7];
        jMenu = new JMenu("项目");
        jMenuItems[0] = new JMenuItem("导入");
        jMenuItems[0].addActionListener(actionEvent -> {
            JFileChooser jFileChooser = new JFileChooser(homeDirectory);
            jFileChooser.setSelectedFile(openedFile);
            int result = jFileChooser.showOpenDialog(null);
            if (JOptionPane.YES_OPTION != result) {
                return;
            }
            File file = jFileChooser.getSelectedFile();
            if (null != file) {
                openedFile = file;
                homeDirectory = openedFile.getParent() + "/";
                drawTable.showMessage("已导入 \"" + openedFile.getName() + "\"");
                drawTable.adapter.getCodeFromXml(openedFile);
                drawTable.doRepaint();
            }
        });
        jMenuItems[1] = new JMenuItem("导出");
        jMenuItems[1].addActionListener(actionEvent -> {
            JFileChooser jFileChooser = new JFileChooser(homeDirectory);
            jFileChooser.setSelectedFile(openedFile);
            int result = jFileChooser.showOpenDialog(null);
            if (JOptionPane.YES_OPTION != result) {
                return;
            }
            File file = jFileChooser.getSelectedFile();
            if (null != file) {
                if (isSystemFile(file.getName())) {
                    drawTable.showMessage("不能导出到系统文件！");
                    return;
                }
                openedFile = file;
                homeDirectory = openedFile.getParent() + "/";
                if (openedFile.exists()) {
                    result = JOptionPane.showConfirmDialog(null, "项目 \"" + openedFile.getName() + "\" 已存在，覆盖？");
                    if (JOptionPane.YES_OPTION != result) {
                        return;
                    }
                }
                drawTable.adapter.setCodeToXml(openedFile);
            }
        });
        jMenuItems[2] = new JMenuItem("清空");
        jMenuItems[2].addActionListener(actionEvent -> {
            int result = JOptionPane.showConfirmDialog(null, "清空工作区？");
            if (JOptionPane.YES_OPTION != result) {
                return;
            }
            drawTable.adapter.clear();
            drawTable.doRepaint();
        });
        jMenuItems[3] = new JMenuItem("设置");
        jMenuItems[3].addActionListener(actionEvent -> {
            String[] defaultKeywords = drawTable.adapter.getDefaultKeywords();
            String[] currentKeywords = drawTable.adapter.getCurrentKeywords();
            JTextField[] jTextFields = new JTextField[currentKeywords.length];
            JFrame jFrame = new JFrame();
            jFrame.setLayout(new GridLayout(currentKeywords.length + 1, 1));
            for (int i = 0; i < currentKeywords.length; i++) {
                JPanel jPanel = new JPanel();
                jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
                JLabel jLabel = new JLabel(defaultKeywords[i] + " -> ");
                jPanel.add(jLabel);
                jTextFields[i] = new JTextField(currentKeywords[i], 8);
                jPanel.add(jTextFields[i]);
                jFrame.add(jPanel);
            }
            JButton[] jButtons = new JButton[2];
            jButtons[0] = new JButton("确定");
            jButtons[1] = new JButton("取消");
            jButtons[0].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LinkedList<String> linkedList = new LinkedList<>();
                    for (int i = 0; i < currentKeywords.length; i++) {
                        String keyword = jTextFields[i].getText().replace(" ", "");
                        if (!linkedList.contains(keyword)) {
                            linkedList.add(keyword);
                        }
                        currentKeywords[i] = keyword;
                    }
                    if (currentKeywords.length == linkedList.size()) {
                        int result = JOptionPane.showConfirmDialog(null, "将这些关键字置为默认？");
                        boolean done = drawTable.adapter.setCurrentKeywords(currentKeywords, JOptionPane.YES_OPTION == result);
                        if (!done) {
                            return;
                        }
                    } else {
                        drawTable.showMessage("关键字必须互不相同！");
                        return;
                    }
                    jFrame.setVisible(false);
                }
            });
            jButtons[1].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jFrame.setVisible(false);
                }
            });
//            jFrame.add(jButtons[0]);
//            jFrame.add(jButtons[1]);
            // 必须放在一起，否则会两行显示。
            JPanel jPanel = new JPanel();
            jPanel.add(jButtons[0]);
            jPanel.add(jButtons[1]);
            jFrame.add(jPanel);
            setWindowCenter(jFrame);
//            jFrame.setAlwaysOnTop(true);
            // 先显示再pack()就可以不设定大小了之根据内部组件自适应
            jFrame.setVisible(true);
            jFrame.pack();
        });
        jMenu.add(jMenuItems[0]);
        jMenu.add(jMenuItems[1]);
        jMenu.add(jMenuItems[2]);
        jMenu.add(jMenuItems[3]);
        jMenuBar.add(jMenu);
        jMenu = new JMenu("代码");
        jMenuItems[0] = new JMenuItem("运行");
        jMenuItems[1] = new JMenuItem("插入");
        jMenuItems[2] = new JMenuItem("修改");
        jMenuItems[3] = new JMenuItem("复制");
        jMenuItems[4] = new JMenuItem("粘贴");
        jMenuItems[5] = new JMenuItem("移除");
        jMenuItems[6] = new JMenuItem("整理");
        JScrollPane jScrollPane = new JScrollPane(drawTable.jTextArea);
        drawTable.jTextArea.setEditable(false);
        JFrame jFrame = new JFrame();
        jFrame.add(jScrollPane);
        jFrame.setSize(screenHeight / 2, screenHeight / 2);
        setWindowCenter(jFrame);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                drawTable.adapter.stop();
            }
        });
        jMenuItems[0].addActionListener(actionEvent -> new Thread(() -> {
            drawTable.jTextArea.setText("");
            drawTable.stringBuilder = new StringBuilder();
            jFrame.setVisible(true);
            drawTable.adapter.run();
        }).start());
        jMenuItems[1].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.create("Member");
            } else {
                drawTable.showMessage("请先选中一条语句！");
            }
        });
        jMenuItems[2].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.create("Modify");
            } else {
                drawTable.showMessage("请先选中一条语句！");
            }
        });
        jMenuItems[3].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.copy();
            } else {
                drawTable.showMessage("请先选中一条语句！");
            }
        });
        jMenuItems[4].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.paste();
                drawTable.doRepaint();
            } else {
                drawTable.showMessage("请先选中一条语句！");
            }
        });
        jMenuItems[5].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.remove();
                drawTable.doRepaint();
            } else {
                drawTable.showMessage("请先选中一条语句！");
            }
        });
        jMenuItems[6].addActionListener(actionEvent -> {
            try {
                int capacity = Integer.parseInt(JOptionPane.showInputDialog("输入容量：", "0"));
                drawTable.adapter.sort(capacity);
                drawTable.doRepaint();
            } catch (NumberFormatException e) {
                drawTable.showMessage("请输入整数！");
            }
        });
        jMenu.add(jMenuItems[0]);
        jMenu.add(jMenuItems[1]);
        jMenu.add(jMenuItems[2]);
        jMenu.add(jMenuItems[3]);
        jMenu.add(jMenuItems[4]);
        jMenu.add(jMenuItems[5]);
        jMenu.add(jMenuItems[6]);
        jMenuBar.add(jMenu);
        if (osName.contains("Linux")) {
            jMenu = new JMenu("工具");
            JCheckBoxMenuItem jCheckBoxMenuItem = new JCheckBoxMenuItem("启用硬件加速");
            if (opengl.exists()) {
                jCheckBoxMenuItem.setSelected(true);
            }
            jCheckBoxMenuItem.addItemListener(e -> {
                int state = e.getStateChange();
                if (ItemEvent.SELECTED == state) {
                    if (!opengl.exists()) {
                        try {
                            if (opengl.createNewFile()) {
                                drawTable.showMessage("重启生效！");
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    if (opengl.exists()) {
                        if (opengl.delete()) {
                            drawTable.showMessage("重启生效！");
                        }
                    }
                }
            });
            jMenu.add(jCheckBoxMenuItem);
            jMenuBar.add(jMenu);
        }
        ippotim.setJMenuBar(jMenuBar);
        ippotim.add(drawTable);
        ippotim.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        ippotim.setSize(screenWidth / 2, screenWidth / 2);
        setWindowCenter(ippotim);
        ippotim.setVisible(true);
    }
}