package com.limshx.ippotim.kernel;

public interface GraphicsOperations {
    void drawRect(int x, int y, int width, int height, int color);

    void fillRect(int x, int y, int width, int height, int color);

    void drawLine(int x1, int y1, int x2, int y2);

    void drawString(String str, int x, int y, int color);

    void appendText(String s);

    int getPixelWidth(String s, int fontSize);

    Object getInput();

    void create(String s);

    void showMessage(String s);

    void doRepaint();
}
