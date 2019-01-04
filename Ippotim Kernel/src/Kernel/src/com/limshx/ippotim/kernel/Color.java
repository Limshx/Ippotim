package com.limshx.ippotim.kernel;

class Color {
    static final int BLACK = 0xff000000;
    static final int WHITE = 0xffffffff;
    static final int RED = 0xffff0000;
    static final int YELLOW = 0xffffff00;
    static final int BLUE = 0xff0000ff;

    int rectangleColor;
    int stringColor;

    Color(int a, int b) {
        rectangleColor = a;
        stringColor = b;
    }
}
