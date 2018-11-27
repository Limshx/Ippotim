package com.limshx.ippotim;

import Kernel.Adapter;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
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

    public static void main(String[] args) {
        Main drawRect = new Main("The Ippotim Programming Language");
        try {
            drawRect.setIconImage(ImageIO.read(drawRect.getClass().getResource("ippotim.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        homeDirectory = System.getProperty("user.dir") + "/";

        // macOS下菜单栏放到全局菜单栏
        if (System.getProperty("os.name").contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

//        if (System.getProperty("os.name").contains("Win")) {
//            homeDirectory = System.getProperty("user.dir") + "/";
//        }

        DrawTable drawTable = new DrawTable();
        drawRect.add(drawTable);
//        Container cont = drawRect.getContentPane();
//        drawTable.setPreferredSize(new Dimension(drawTable.windowSize, drawTable.windowSize));
//        JScrollPane scr1 = new JScrollPane(drawTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//        cont.add(scr1);

        JMenuBar jMenuBar = new JMenuBar();
        JMenu jMenu;
        JMenuItem[] jMenuItems = new JMenuItem[6];
        jMenu = new JMenu("File");
        jMenuItems[0] = new JMenuItem("Import");
        jMenuItems[0].addActionListener(actionEvent -> {
            JFileChooser jFileChooser = new JFileChooser(new File(homeDirectory));
            jFileChooser.showOpenDialog(null);
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
            String fileName = JOptionPane.showInputDialog("Input a file name :", null != openedFile ? openedFile.getName() : null);
            if (null != fileName && !fileName.equals("")) {
                if (new File(homeDirectory + fileName).exists()) {
                    int result = JOptionPane.showConfirmDialog(null, "File \"" + fileName + "\" exists, overwrite it?");
                    if (JOptionPane.YES_OPTION != result) {
                        return;
                    }
                }
                drawTable.adapter.setCodeToXml(new File(homeDirectory + fileName));
            }
        });
        jMenuItems[2] = new JMenuItem("Clear");
        jMenuItems[2].addActionListener(actionEvent -> {
            openedFile = null;
            drawTable.adapter = new Adapter(drawTable, drawTable.getWidth() / 2, drawTable.getHeight() / 2, 1);
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
        JScrollPane jScrollPane = new JScrollPane(drawTable.jTextArea);
        drawTable.jTextArea.setEditable(false);
        JFrame jFrame = new JFrame("Output");
        jFrame.add(jScrollPane);
        int windowSize = drawTable.windowSize / 2;
        jFrame.setSize(windowSize, windowSize);
        drawTable.setWindowCenter(jFrame);
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
        jMenu.add(jMenuItems[0]);
        jMenu.add(jMenuItems[1]);
        jMenu.add(jMenuItems[2]);
        jMenu.add(jMenuItems[3]);
        jMenu.add(jMenuItems[4]);
        jMenu.add(jMenuItems[5]);
        jMenuBar.add(jMenu);
//        jMenu = new JMenu("New");
//        JMenuItem jMenuItem = new JMenuItem("Function");
//        jMenuItem.addActionListener(actionEvent -> drawTable.create("Function"));
//        jMenu.add(jMenuItem);
//        jMenuBar.add(jMenu);
        drawRect.setJMenuBar(jMenuBar);

        drawRect.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        drawRect.setSize(drawTable.windowSize, drawTable.windowSize);
        drawTable.setWindowCenter(drawRect);
        drawRect.setVisible(true);
    }
}