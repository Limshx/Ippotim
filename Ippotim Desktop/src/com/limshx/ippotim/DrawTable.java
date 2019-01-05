package com.limshx.ippotim;

import com.limshx.ippotim.kernel.Adapter;
import com.limshx.ippotim.kernel.GraphicsOperations;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;

class DrawTable extends JPanel implements GraphicsOperations {
    Adapter adapter;
    private Graphics g;
    private Font font;
    JTextArea jTextArea = new JTextArea();

    private String s;
    public void create(String type) {
        switch (type) {
            case "Function":
                s = JOptionPane.showInputDialog("Input a function head :", s);
                break;
            case "Structure":
                s = JOptionPane.showInputDialog("Input a structure name :", s);
                break;
            case "Member":
                s = JOptionPane.showInputDialog("Input a statement :", s);
                break;
            case "Modify":
                s = JOptionPane.showInputDialog("Input a statement :", adapter.getRectangleContent());
                break;
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
                case "Member":
                    adapter.insert(s);
                    break;
                case "Modify":
                    adapter.modify(s);
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
        JFrame jFrame = new JFrame("Input");
        JLabel jLabel = new JLabel("Input a value :");
        JTextField jTextField = new JTextField();
        jTextField.setColumns(20);
        JButton[] jButtons = new JButton[2];
        jButtons[0] = new JButton(" String ");
        jButtons[1] = new JButton("Number");
        jButtons[0].addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                input = jTextField.getText();
                if (!((String) input).contains("\"")) {
                    inputted = true;
                    jFrame.setVisible(false);
                } else {
                    showMessage("Double quotation mark is forbidden!");
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
                    showMessage("Not an integer!");
                }
            }
        });
        jFrame.setLayout(new FlowLayout(FlowLayout.CENTER));
        jFrame.add(jLabel);
        jFrame.add(jTextField);
        JPanel jPanel = new JPanel();
        jPanel.add(jButtons[0]);
        jPanel.add(jButtons[1]);
        jFrame.add(jPanel);
        jFrame.setSize(250, 110);
        Main.setWindowCenter(jFrame);
        jFrame.setAlwaysOnTop(true);
        jFrame.setVisible(true);
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
            adapter = new Adapter(this, getWidth(), getHeight(), 1, new File(Main.homeDirectory + "ippotim.properties"));
        }
        if (isScreenChanged) {
            isScreenChanged = false;
            adapter.setScreen(getWidth(), getHeight());
        }
        adapter.paintEverything();
    }
}
