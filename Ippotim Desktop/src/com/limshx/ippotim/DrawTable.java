package com.limshx.ippotim;

import Kernel.Adapter;
import Kernel.GraphicsOperation;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class DrawTable extends JPanel implements GraphicsOperation {
    Adapter adapter;
    int windowSize = 600;
    private Graphics g;
    JTextArea jTextArea = new JTextArea();

    public void create(String type) {
        String s;
        switch (type) {
            case "Function":
                s = JOptionPane.showInputDialog("Input a function name :");
                break;
            case "Structure":
                s = JOptionPane.showInputDialog("Input a structure name :");
                break;
            case "Member":
                s = JOptionPane.showInputDialog("Input a sentence :");
                break;
            case "Modify":
                s = JOptionPane.showInputDialog("Input a sentence :", adapter.getRectangleContent());
                break;
            default:
                s = null;
                break;
        }
        if (s != null) {
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
        } else {
            if (type.equals("Member")) {
                adapter.insert("");
            }
        }
    }

    void doRepaint() {
        repaint();
    }

    public void drawRect(int x, int y, int width, int height) {
        g.setColor(Color.black);
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
        g.setColor(new Color(color));
        g.drawString(str, x, y);
    }

    StringBuilder stringBuilder;
    private boolean hasSoftWrap;

    @Override
    public void appendText(String s) {
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
                appendText(s.substring(freeSpace));
            }
        } else {
            if (hasSoftWrap && s.equals("\n")) {
                hasSoftWrap = false;
            } else {
                stringBuilder.append(s);
                jTextArea.append(s);
            }
        }
        jTextArea.setCaretPosition(jTextArea.getText().length());
    }

    @Override
    public int getTextLength(String s, int fontSize) {
//        return (float) new Font("圆体", Font.PLAIN, (int) fontSize).getStringBounds(s, ((Graphics2D) g).getFontRenderContext()).getWidth();
        g.setFont(new Font("圆体", Font.PLAIN, fontSize));
        return g.getFontMetrics().stringWidth(s);
    }

    @Override
    public String getInput(String s) {
        return JOptionPane.showInputDialog("Input a " + s + " :");
    }

    DrawTable() {
        addMouseListener(new MouseListener() {
                             public void mousePressed(MouseEvent e) {
                                 adapter.setXY(e.getX(), e.getY());
                                 if (e.isMetaDown()) { // 右键新建函数定义
                                     create("Function");
                                 } else if (e.getClickCount() == 2) // 双击新建结构定义
                                 {
                                     create("Structure");
                                 } else {
                                     adapter.click();
                                     doRepaint();
                                 }
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
    }

    protected void paintComponent(Graphics g) {
        this.g = g; // 总想着getGraphics()云云如何获取g，没想到可以直接在这里获取
        g.clearRect(0, 0, getWidth(), getHeight()); // 没这句就会有重影
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (null == adapter) {
            adapter = new Adapter(this);
            adapter.init(getWidth() / 2, getHeight() / 2, 1);
        }

        adapter.paintEverything();
    }
}
