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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;


class Main extends JFrame {
    private Main(String s) {
        super(s);
    }

    static String homeDirectory;
    private static File openedFile;
    private static int screenWidth, screenHeight;

    private static void initScreenSize() {
        screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    static void setWindowCenter(JFrame jFrame) {
        jFrame.setLocation(screenWidth / 2 - jFrame.getWidth() / 2, screenHeight / 2 - jFrame.getHeight() / 2);
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
        Main main = new Main("The Ippotim Programming Language");
        try {
            main.setIconImage(ImageIO.read(main.getClass().getResource("ippotim.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        homeDirectory = System.getProperty("user.dir") + "/";

        // macOS下菜单栏放到全局菜单栏
        if (osName.contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

//        if (System.getProperty("os.name").contains("Win")) {
//            homeDirectory = System.getProperty("user.dir") + "/";
//        }

        DrawTable drawTable = new DrawTable();
//        Container cont = drawRect.getContentPane();
//        drawTable.setPreferredSize(new Dimension(drawTable.windowSize, drawTable.windowSize));
//        JScrollPane scr1 = new JScrollPane(drawTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//        cont.add(scr1);
        JMenuBar jMenuBar = new JMenuBar();
        JMenu jMenu;
        JMenuItem[] jMenuItems = new JMenuItem[7];
        jMenu = new JMenu("File");
        jMenuItems[0] = new JMenuItem("Import");
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
                drawTable.showMessage("Imported \"" + openedFile.getName() + "\"");
                drawTable.adapter.getCodeFromXml(openedFile);
                drawTable.doRepaint();
            }
        });
        jMenuItems[1] = new JMenuItem("Export");
        jMenuItems[1].addActionListener(actionEvent -> {
            JFileChooser jFileChooser = new JFileChooser(homeDirectory);
//            jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jFileChooser.setSelectedFile(openedFile);
            int result = jFileChooser.showOpenDialog(null);
            if (JOptionPane.YES_OPTION != result) {
                return;
            }
            File file = jFileChooser.getSelectedFile();
            if (null != file) {
                openedFile = file;
                homeDirectory = openedFile.getParent() + "/";
                if (openedFile.exists()) {
                    result = JOptionPane.showConfirmDialog(null, "File \"" + openedFile.getName() + "\" exists, overwrite it?");
                    if (JOptionPane.YES_OPTION != result) {
                        return;
                    }
                }
                drawTable.adapter.setCodeToXml(openedFile);
            }
        });
        jMenuItems[2] = new JMenuItem("Clear");
        jMenuItems[2].addActionListener(actionEvent -> {
            openedFile = null;
            drawTable.adapter.clear();
            drawTable.doRepaint();
        });
        jMenuItems[3] = new JMenuItem("Settings");
        jMenuItems[3].addActionListener(actionEvent -> {
            String[] defaultKeywords = drawTable.adapter.getDefaultKeywords();
            String[] currentKeywords = drawTable.adapter.getCurrentKeywords();
            JTextField[] jTextFields = new JTextField[currentKeywords.length];
            JFrame jFrame = new JFrame("Settings");
            jFrame.setLayout(new FlowLayout(FlowLayout.RIGHT));
            for (int i = 0; i < currentKeywords.length; i++) {
                JPanel jPanel = new JPanel();
                JLabel jLabel = new JLabel(defaultKeywords[i] + " -> ");
                jPanel.add(jLabel);
                jTextFields[i] = new JTextField(currentKeywords[i], 8);
                jPanel.add(jTextFields[i]);
                jFrame.add(jPanel);
            }
            JButton[] jButtons = new JButton[2];
            jButtons[0] = new JButton("   OK   ");
            jButtons[1] = new JButton("Cancel");
            jButtons[0].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jFrame.setVisible(false);
                    LinkedList<String> linkedList = new LinkedList<>();
                    for (int i = 0; i < currentKeywords.length; i++) {
                        String keyword = jTextFields[i].getText().replace(" ", "");
                        if (!linkedList.contains(keyword)) {
                            linkedList.add(keyword);
                        }
                        currentKeywords[i] = keyword;
                    }
                    if (currentKeywords.length == linkedList.size()) {
                        int result = JOptionPane.showConfirmDialog(null, "Make the keywords default?");
                        drawTable.adapter.setCurrentKeywords(currentKeywords, JOptionPane.YES_OPTION == result);
                    } else {
                        drawTable.showMessage("A keyword must be different from the others!");
                    }
                }
            });
            jButtons[1].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    jFrame.setVisible(false);
                }
            });
            jFrame.add(jButtons[0]);
            jFrame.add(jButtons[1]);
            jFrame.setSize(screenHeight / 5, (int) (screenHeight / 2.9));
            setWindowCenter(jFrame);
            jFrame.setAlwaysOnTop(true);
            jFrame.setVisible(true);
        });
        jMenu.add(jMenuItems[0]);
        jMenu.add(jMenuItems[1]);
        jMenu.add(jMenuItems[2]);
        jMenu.add(jMenuItems[3]);
        jMenuBar.add(jMenu);
        jMenu = new JMenu("Code");
        jMenuItems[0] = new JMenuItem("Run");
        jMenuItems[1] = new JMenuItem("Insert");
        jMenuItems[2] = new JMenuItem("Modify");
        jMenuItems[3] = new JMenuItem("Copy");
        jMenuItems[4] = new JMenuItem("Paste");
        jMenuItems[5] = new JMenuItem("Remove");
        jMenuItems[6] = new JMenuItem("Sort");
        JScrollPane jScrollPane = new JScrollPane(drawTable.jTextArea);
        drawTable.jTextArea.setEditable(false);
        JFrame jFrame = new JFrame("Output");
        jFrame.add(jScrollPane);
        jFrame.setSize(screenHeight / 2, screenHeight / 2);
        setWindowCenter(jFrame);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                drawTable.adapter.stop();
            }
        });
        jMenuItems[0].addActionListener(actionEvent -> {
            drawTable.jTextArea.setText("");
            drawTable.stringBuilder = new StringBuilder();
            jFrame.setVisible(true);
            new Thread(drawTable.adapter::run).start();
        });
        jMenuItems[1].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.create("Member");
            } else {
                drawTable.showMessage("Please select a rectangle first!");
            }
        });
        jMenuItems[2].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.create("Modify");
            } else {
                drawTable.showMessage("Please select a rectangle first!");
            }
        });
        jMenuItems[3].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.copy();
            } else {
                drawTable.showMessage("Please select a rectangle first!");
            }
        });
        jMenuItems[4].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.paste();
                drawTable.doRepaint();
            } else {
                drawTable.showMessage("Please select a rectangle first!");
            }
        });
        jMenuItems[5].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.remove();
                drawTable.doRepaint();
            } else {
                drawTable.showMessage("Please select a rectangle first!");
            }
        });
        jMenuItems[6].addActionListener(actionEvent -> {
            try {
                int capacity = Integer.parseInt(JOptionPane.showInputDialog("Input capacity :", "0"));
                drawTable.adapter.sort(capacity);
                drawTable.doRepaint();
            } catch (NumberFormatException e) {
                drawTable.showMessage("Not an integer!");
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
            jMenu = new JMenu("Tools");
            JCheckBoxMenuItem jCheckBoxMenuItem = new JCheckBoxMenuItem("Enable OpenGL");
            if (opengl.exists()) {
                jCheckBoxMenuItem.setSelected(true);
            }
            jCheckBoxMenuItem.addItemListener(e -> {
                int state = e.getStateChange();
                if (ItemEvent.SELECTED == state) {
                    if (!opengl.exists()) {
                        try {
                            if (opengl.createNewFile()) {
                                drawTable.showMessage("Restart to take effect!");
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    if (opengl.exists()) {
                        if (opengl.delete()) {
                            drawTable.showMessage("Restart to take effect!");
                        }
                    }
                }
            });
            jMenu.add(jCheckBoxMenuItem);
            jMenuBar.add(jMenu);
        }
        main.setJMenuBar(jMenuBar);
        main.add(drawTable);

        main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        main.setSize(screenWidth / 2, screenWidth / 2);
        setWindowCenter(main);
        main.setVisible(true);
    }
}