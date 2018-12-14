package com.limshx.ippotim;

import Kernel.Adapter;
import Kernel.GraphicsOperations;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class DrawTable extends JPanel implements GraphicsOperations {
    Adapter adapter;
    private Graphics g;
    private Font font;
    JTextArea jTextArea = new JTextArea();

    public void create(String type) {
        String s;
        switch (type) {
            case "Function":
                s = JOptionPane.showInputDialog("Input a function head :");
                break;
            case "Structure":
                s = JOptionPane.showInputDialog("Input a structure name :");
                break;
            case "Member":
                s = JOptionPane.showInputDialog("Input a statement :");
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

    void doRepaint() {
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
//        return (float) new Font("圆体", Font.PLAIN, (int) fontSize).getStringBounds(s, ((Graphics2D) g).getFontRenderContext()).getWidth();
        font = new Font("SERIF", Font.PLAIN, fontSize);
        g.setFont(font);
        return g.getFontMetrics().stringWidth(s);
    }

    @Override
    public String getInput(String name) {
        return JOptionPane.showInputDialog("Input for \"" + name + "\" :");
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
            adapter = new Adapter(this, getWidth(), getHeight(), 1);
        }
        if (isScreenChanged) {
            isScreenChanged = false;
            adapter.setScreen(getWidth(), getHeight());
        }
        adapter.paintEverything();
    }
}
