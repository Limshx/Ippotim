package com.limshx.ippotim;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;


class Main extends JFrame {
    private Main(String s) {
        super(s);
    }
    private static File openedFile;
    private static String homeDirectory;
    private static int screenWidth, screenHeight;

    private static void initScreenSize() {
        screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    private static void setWindowCenter(JFrame jFrame) {
        jFrame.setLocation(screenWidth / 2 - jFrame.getWidth() / 2, screenHeight / 2 - jFrame.getHeight() / 2);
    }

    public static void main(String[] args) {
        String osName = System.getProperty("os.name");
        // 开启硬件加速，启动参数里加-Dsun.java2d.opengl=true也行，但是当然还是代码里加好。
        if (!osName.contains("Windows")) {
            System.setProperty("sun.java2d.opengl", "true");
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
            int result = jFileChooser.showOpenDialog(null);
            if (JOptionPane.YES_OPTION != result) {
                return;
            }
            File file = jFileChooser.getSelectedFile();
            if (null != file) {
                openedFile = file;
                homeDirectory = openedFile.getParent() + "/";
                JOptionPane.showMessageDialog(null,"Imported \"" + openedFile.getName() + "\"");
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
        jMenu.add(jMenuItems[0]);
        jMenu.add(jMenuItems[1]);
        jMenu.add(jMenuItems[2]);
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
                JOptionPane.showMessageDialog(null, "Please select a rectangle first!");
            }
        });
        jMenuItems[2].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.create("Modify");
            } else {
                JOptionPane.showMessageDialog(null, "Please select a rectangle first!");
            }
        });
        jMenuItems[3].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.copy();
            } else {
                JOptionPane.showMessageDialog(null, "Please select a rectangle first!");
            }
        });
        jMenuItems[4].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.paste();
                drawTable.doRepaint();
            } else {
                JOptionPane.showMessageDialog(null, "Please select a rectangle first!");
            }
        });
        jMenuItems[5].addActionListener(actionEvent -> {
            if (drawTable.adapter.hasSelectedTreeNode()) {
                drawTable.adapter.remove();
                drawTable.doRepaint();
            } else {
                JOptionPane.showMessageDialog(null, "Please select a rectangle first!");
            }
        });
        jMenuItems[6].addActionListener(actionEvent -> {
            try {
                int capacity = Integer.parseInt(JOptionPane.showInputDialog("Input capacity :", "0"));
                drawTable.adapter.sort(capacity);
                drawTable.doRepaint();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Not an integer!");
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
        main.setJMenuBar(jMenuBar);
        main.add(drawTable);

        main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        main.setSize(screenWidth / 2, screenWidth / 2);
        setWindowCenter(main);
        main.setVisible(true);
    }
}