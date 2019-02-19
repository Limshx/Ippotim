package com.limshx.ippotim;

import com.limshx.ippotim.kernel.Adapter;
import com.limshx.ippotim.kernel.GraphicsOperations;
import com.limshx.ippotim.kernel.StatementType;
import com.limshx.ippotim.kernel.TreeNode;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

class DrawTable extends JPanel implements GraphicsOperations {
    Adapter adapter;
    private Graphics g;
    private Font font;
    private final int jTextFieldColumns = 10;
    JTextArea jTextArea = new JTextArea();
    private JButton[] jButtons;
    private LinkedList<JTextField> jTextFields;
    private JComboBox<String> jComboBox;
    private TreeNode treeNode;

    private void create(JFrame jFrame, String label, boolean insertOrModify) {
        jTextFields = new LinkedList<>();
        JFrame input = new JFrame();
        input.setLayout(new GridLayout(2, 1));
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout());
        jPanel.add(new JLabel(label));
        JTextField jTextField = new JTextField();
        jTextFields.add(jTextField);
        jTextField.setColumns(jTextFieldColumns);
        jPanel.add(jTextField);
        input.add(jPanel);
        jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout());
        JButton[] jButtons = new JButton[2];
        jButtons[0] = new JButton("确定");
        jButtons[0].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String statement = label + " " + jTextField.getText();
                create(statement, insertOrModify);
                input.setVisible(false);
                jFrame.setVisible(false);
            }
        });
        jButtons[1] = new JButton("取消");
        jButtons[1].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.setVisible(false);
            }
        });
        jPanel.add(jButtons[0]);
        jPanel.add(jButtons[1]);

        input.add(jPanel);
        input.pack();
        Ippotim.setWindowCenter(input);
        input.setVisible(true);
        input.setAlwaysOnTop(true);
    }

    private void create(String statement, boolean insertOrModify) {
        if (insertOrModify) {
            adapter.insert(statement.trim());
        } else {
            adapter.modify(statement.trim());
        }
        doRepaint();
    }

    private void create(boolean insertOrModify) {
        if (!insertOrModify) {
            treeNode = adapter.getTreeNode();
            if (StatementType.HEAD == treeNode.statementType) {
                s = treeNode.getContent();
                if (!s.isEmpty()) {
                    if (adapter.isFunctionOrStructure()) {
                        create("Function");
                    } else {
                        create("Structure");
                    }
                } else {
                    showMessage("不能修改空白头语句！");
                }
                return;
            }
        }
        JFrame jFrame = new JFrame();
        int length = 12;
        jFrame.setLayout(new GridLayout(length / 2, 2));
        jButtons = new JButton[length];
        jButtons[0] = new JButton(StatementType.DEFINE.name);
        jButtons[0].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jTextFields = new LinkedList<>();
                JFrame input = new JFrame();
                input.setLayout(new GridLayout(2, 1));
                JPanel[] jPanels = new JPanel[2];

                jPanels[0] = new JPanel();
                jPanels[0].setLayout(new GridLayout(1, 2));
                jComboBox = new JComboBox<>();
                Set<String> structures = adapter.getStructures();
                for (String structure : structures) {
                    jComboBox.addItem(structure);
                }
                jPanels[0].add(jComboBox);
                JTextField jTextField = new JTextField();
                jTextFields.add(jTextField);
                jTextField.setColumns(jTextFieldColumns);
                jPanels[0].add(jTextField);

                jPanels[1] = new JPanel();
                jPanels[1].setLayout(new FlowLayout(FlowLayout.CENTER));
                JButton[] jButtons = new JButton[2];
                jButtons[0] = new JButton("确定");
                jButtons[0].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String statement = jComboBox.getSelectedItem() + " " + jTextField.getText();
                        create(statement, insertOrModify);
                        input.setVisible(false);
                        jFrame.setVisible(false);
                    }
                });
                jButtons[1] = new JButton("取消");
                jButtons[1].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        input.setVisible(false);
                    }
                });
                jPanels[1].add(jButtons[0]);
                jPanels[1].add(jButtons[1]);

                input.add(jPanels[0]);
                input.add(jPanels[1]);
                input.pack();
                Ippotim.setWindowCenter(input);
                input.setVisible(true);
                input.setAlwaysOnTop(true);
            }
        });
        jButtons[1] = new JButton(StatementType.ASSIGN.name);
        jButtons[1].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jTextFields = new LinkedList<>();
                JFrame input = new JFrame();
                input.setLayout(new GridLayout(2, 1));
                JPanel jPanel = new JPanel();
                jPanel.setLayout(new FlowLayout());
                JTextField jTextField = new JTextField();
                jTextFields.add(jTextField);
                jPanel.add(jTextField);
                jTextField.setColumns(jTextFieldColumns);
                jPanel.add(new JLabel("="));
                jTextField = new JTextField();
                jTextFields.add(jTextField);
                jPanel.add(jTextField);
                jTextField.setColumns(jTextFieldColumns);
                input.add(jPanel);
                jPanel = new JPanel();
                jPanel.setLayout(new FlowLayout());
                JButton[] jButtons = new JButton[2];
                jButtons[0] = new JButton("确定");
                jButtons[0].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String statement = jTextFields.get(0).getText() + " = " + jTextFields.get(1).getText();
                        create(statement, insertOrModify);
                        input.setVisible(false);
                        jFrame.setVisible(false);
                    }
                });
                jButtons[1] = new JButton("取消");
                jButtons[1].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        input.setVisible(false);
                    }
                });
                jPanel.add(jButtons[0]);
                jPanel.add(jButtons[1]);

                input.add(jPanel);
                input.pack();
                Ippotim.setWindowCenter(input);
                input.setVisible(true);
                input.setAlwaysOnTop(true);
            }
        });
        jButtons[2] = new JButton(StatementType.INPUT.name);
        jButtons[2].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(jFrame, adapter.getCurrentKeywords()[7], insertOrModify);
            }
        });
        jButtons[3] = new JButton(StatementType.OUTPUT.name);
        jButtons[3].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(jFrame, adapter.getCurrentKeywords()[8], insertOrModify);
            }
        });
        jButtons[4] = new JButton(StatementType.IF.name);
        jButtons[4].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(jFrame, adapter.getCurrentKeywords()[1], insertOrModify);
            }
        });
        jButtons[5] = new JButton(StatementType.ELSE.name);
        jButtons[5].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String statement = adapter.getCurrentKeywords()[2];
                create(statement, insertOrModify);
                jFrame.setVisible(false);
            }
        });
        jButtons[6] = new JButton(StatementType.WHILE.name);
        jButtons[6].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(jFrame, adapter.getCurrentKeywords()[3], insertOrModify);
            }
        });
        jButtons[7] = new JButton(StatementType.COMMENT.name);
        jButtons[7].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                create(jFrame, "//", insertOrModify);
            }
        });
        jButtons[8] = new JButton(StatementType.BREAK.name);
        jButtons[8].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String statement = adapter.getCurrentKeywords()[4];
                create(statement, insertOrModify);
                jFrame.setVisible(false);
            }
        });
        jButtons[9] = new JButton(StatementType.CONTINUE.name);
        jButtons[9].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String statement = adapter.getCurrentKeywords()[5];
                create(statement, insertOrModify);
                jFrame.setVisible(false);
            }
        });
        jButtons[10] = new JButton(StatementType.CALL.name);
        jButtons[10].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jTextFields = new LinkedList<>();
                JFrame input = new JFrame();
                input.setLayout(new GridLayout(2, 1));
                JPanel[] jPanels = new JPanel[2];

                jPanels[0] = new JPanel();
                jPanels[0].setLayout(new GridLayout(1, 2));
                jComboBox = new JComboBox<>();
                JPanel jPanel = new JPanel();
                jPanel.add(new JLabel("{"));
                jPanel.add(jComboBox);
                jPanel.add(new JLabel("}"));
                jPanels[0].add(jPanel);
                jComboBox.addItemListener(e1 -> {
                    if (null != jComboBox.getSelectedItem()) {
                        jTextFields.clear();
                        jPanels[0].removeAll();
                        jPanels[0].add(jPanel);
                        int functionParametersCount = adapter.getFunctionParametersCount(jComboBox.getSelectedItem().toString());
                        for (int i = 0; i < functionParametersCount; i++) {
                            JTextField jTextField = new JTextField();
                            jTextField.setColumns(jTextFieldColumns);
                            jPanels[0].add(jTextField);
                            jTextFields.add(jTextField);
                        }
                        input.pack();
                    }
                });
                Set<String> functions = adapter.getFunctions();
                for (String function : functions) {
                    if (!function.isEmpty()) {
                        jComboBox.addItem(function);
                    }
                }

                jPanels[1] = new JPanel();
                jPanels[1].setLayout(new FlowLayout(FlowLayout.CENTER));
                JButton[] jButtons = new JButton[2];
                jButtons[0] = new JButton("确定");
                jButtons[0].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        StringBuilder stringBuilder = new StringBuilder("{" + jComboBox.getSelectedItem() + "}" + " ");
                        for (JTextField jTextField : jTextFields) {
                            stringBuilder.append(jTextField.getText()).append(" ");
                        }
                        String statement = stringBuilder.toString();
                        create(statement, insertOrModify);
                        input.setVisible(false);
                        jFrame.setVisible(false);
                    }
                });
                jButtons[1] = new JButton("取消");
                jButtons[1].addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        input.setVisible(false);
                    }
                });
                jPanels[1].add(jButtons[0]);
                jPanels[1].add(jButtons[1]);

                input.add(jPanels[0]);
                input.add(jPanels[1]);
                input.pack();
                Ippotim.setWindowCenter(input);
                input.setVisible(true);
            }
        });
        jButtons[11] = new JButton(StatementType.RETURN.name);
        jButtons[11].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String statement = adapter.getCurrentKeywords()[6];
                create(statement, insertOrModify);
                jFrame.setVisible(false);
            }
        });
        for (int i = 0; i < length; i++) {
            jFrame.add(jButtons[i]);
        }
        if (!adapter.canCreateElse() || !insertOrModify) {
            jButtons[5].setEnabled(false);
        }
        jFrame.pack();
        Ippotim.setWindowCenter(jFrame);
        jFrame.setVisible(true);
        if (!insertOrModify) {
            modify();
        }
    }

    private void modify() {
        StatementType statementType = treeNode.statementType;
        String[] strings = treeNode.getContent().split(" ", 2);
        ArrayList<String> elements = treeNode.elements;
        if (null != statementType) {
            jButtons[statementType.ordinal()].doClick();
            switch (statementType) {
                case DEFINE:
                    jComboBox.setSelectedItem(strings[0]);
                    jTextFields.get(0).setText(strings[1]);
                    break;
                case ASSIGN:
                    jTextFields.get(0).setText(elements.get(0));
                    jTextFields.get(1).setText(elements.get(2));
                    break;
                case CALL:
                    jComboBox.setSelectedItem(elements.get(0));
                    for (int i = 0; i < jTextFields.size(); i++) {
                        jTextFields.get(i).setText(elements.get(i + 1));
                    }
                    break;
                default:
                    if (2 == strings.length) {
                        jTextFields.get(0).setText(strings[1]);
                    }
                    break;
            }
        }
    }

    private String s;

    public void create(String type) {
        switch (type) {
            case "Function":
                s = JOptionPane.showInputDialog("输入函数头：", s);
                break;
            case "Structure":
                s = JOptionPane.showInputDialog("输入结构名：", s);
                break;
            case "Member":
                create(true);
                return;
            case "Modify":
                create(false);
                return;
            default:
                s = null;
                break;
        }
        if (null == s) {
            return;
        }
        // 去掉首尾空格符，这样就不可能调用到Ippotim语言里的main函数了。
        s = s.trim();
        if (!s.isEmpty()) {
            switch (type) {
                case "Function":
                    adapter.createFunction(s);
                    break;
                case "Structure":
                    adapter.createStructure(s);
                    break;
                default:
                    break;
            }
            doRepaint();
        }
    }

    @Override
    public void showMessage(String s) {
        JOptionPane.showMessageDialog(null, s);
    }

    @Override
    public void doRepaint() {
        repaint();
    }

    public void drawRect(int x, int y, int width, int height, int color) {
        g.setColor(new Color(color));
        g.drawRect(x, y, width, height);
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        g.setColor(new Color(color));
        g.fillRect(x, y, width, height);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        g.setColor(Color.black);
        g.drawLine(x1, y1, x2, y2);
    }

    public void drawString(String str, int x, int y, int color) {
        g.setFont(font);
        g.setColor(new Color(color));
        g.drawString(str, x, y);
    }

    StringBuilder stringBuilder;
    private boolean hasSoftWrap;

    private void getOutput(String s) {
        if (s.equals("\n")) {
            if (hasSoftWrap) {
                hasSoftWrap = false;
                return;
            }
            jTextArea.append("\n");
            stringBuilder = new StringBuilder();
        } else {
            int cachedStringLength = stringBuilder.length();
            int maxCachedStringLength = 1000;
            if (maxCachedStringLength <= cachedStringLength + s.length()) {
                hasSoftWrap = true;
                int freeSpace = maxCachedStringLength - cachedStringLength;
                jTextArea.append(s.substring(0, freeSpace));
                jTextArea.append("\n");
                stringBuilder = new StringBuilder();
                String remainingString = s.substring(freeSpace);
                if (!remainingString.equals("")) {
                    getOutput(s.substring(freeSpace));
                }
            } else {
                if (hasSoftWrap) {
                    hasSoftWrap = false;
                }
                jTextArea.append(s);
                stringBuilder.append(s);
            }
        }
    }

    @Override
    public void appendText(String s) {
        getOutput(s);
        jTextArea.setCaretPosition(jTextArea.getText().length());
    }

    @Override
    public int getPixelWidth(String s, int fontSize) {
        font = new Font("SERIF", Font.PLAIN, fontSize);
        g.setFont(font);
        return g.getFontMetrics().stringWidth(s);
    }

    private boolean inputted;
    private Object input;

    private void waitForInput() {
        while (!inputted) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inputted = false;
    }

    @Override
    public Object getInput() {
        JFrame jFrame = new JFrame();
        JLabel jLabel = new JLabel("输入一个值：");
        JTextField jTextField = new JTextField();
        jTextField.setColumns(jTextFieldColumns);
        JButton[] jButtons = new JButton[2];
        jButtons[0] = new JButton("字符串");
        jButtons[1] = new JButton("整数");
        jButtons[0].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                input = jTextField.getText();
                if (!((String) input).contains("\"")) {
                    inputted = true;
                    jFrame.setVisible(false);
                } else {
                    showMessage("禁止出现双引号！");
                }
            }
        });
        jButtons[1].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    input = Integer.parseInt(jTextField.getText());
                    inputted = true;
                    jFrame.setVisible(false);
                } catch (NumberFormatException e) {
                    showMessage("请输入整数！");
                }
            }
        });
        jFrame.setLayout(new GridLayout(2, 1));
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        jPanel.add(jLabel);
        jPanel.add(jTextField);
        jFrame.add(jPanel);
        jPanel = new JPanel();
        jPanel.add(jButtons[0]);
        jPanel.add(jButtons[1]);
        jFrame.add(jPanel);
        Ippotim.setWindowCenter(jFrame);
//        jFrame.setAlwaysOnTop(true);
        jFrame.setVisible(true);
        // 这样就不用通过添加空格统一按钮长度了，setSize()不行
        jButtons[1].setPreferredSize(new Dimension(jButtons[0].getWidth(), jButtons[0].getHeight()));
        jFrame.pack();
        waitForInput();
        return input;
    }

    DrawTable() {
        addMouseListener(new MouseListener() {
                             public void mousePressed(MouseEvent e) {
                                 adapter.click(e.getX(), e.getY());
                                 if (!adapter.hasSelectedTreeNode()) {
                                     if (e.isMetaDown()) { // 右键新建函数定义
                                         create("Function");
                                         return;
                                     } else if (e.getClickCount() == 2) { // 双击新建结构定义
                                         create("Structure");
                                         return;
                                     }
                                 }
                                 doRepaint();
                             }//当用户按下鼠标按钮时发生

                             public void mouseReleased(MouseEvent e) {
                             }//当用户松开鼠标按钮时发生

                             public void mouseClicked(MouseEvent e) {
                             }

                             public void mouseEntered(MouseEvent e) {
                             }

                             public void mouseExited(MouseEvent e) {
                             }
                         }
        );
        addMouseMotionListener(new MouseMotionListener() {
                                   public void mouseMoved(MouseEvent e) {
                                   }//当用户按下鼠标按钮并在松开之前进行移动时发生

                                   public void mouseDragged(MouseEvent e) {
                                       adapter.drag(e.getX(), e.getY());
                                       doRepaint();
                                   }//当鼠标在组件上移动而 不时拖动时发生
                               }
        );
        addMouseWheelListener(e ->
        {
            int x, y;
            x = e.getX();
            y = e.getY();
            double scale = Math.pow(1.1, e.getWheelRotation()); // 缩放倍数为1.1
            adapter.doWheelRotation(x, y, scale);
            doRepaint();
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                isScreenChanged = true;
                // 这里要重绘一次，不然会无法及时为adapter更新屏幕宽高。
                doRepaint();
            }
        });
    }

    private boolean isScreenChanged;

    protected void paintComponent(Graphics g) {
        // super.paintComponent(g);也行，但还是这个好
        g.clearRect(getX(), getY(), getWidth(), getHeight());
        this.g = g; // 总想着getGraphics()云云如何获取g，没想到可以直接在这里获取
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (null == adapter) {
            adapter = new Adapter(this, getWidth(), getHeight(), 1, new File(Ippotim.homeDirectory + "ippotim.properties"));
        }
        if (isScreenChanged) {
            isScreenChanged = false;
            adapter.setScreen(getWidth(), getHeight());
        }
        adapter.paintEverything();
    }
}
