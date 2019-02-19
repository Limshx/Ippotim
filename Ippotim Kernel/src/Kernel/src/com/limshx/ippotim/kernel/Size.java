package com.limshx.ippotim.kernel;

class Size {
    static double scale;
    static int width;
    static int height;
    static int fontSize;
    static int arrowSize;
    static double defaultScale;
    private static final int defaultWidth = 200;
    private static final int defaultHeight = 40;
    private static final int defaultFontSize = 32;
    private static final int defaultArrowSize = 16;

    static void updateSize() {
        width = (int) (defaultWidth * scale);
        height = (int) (defaultHeight * scale);
        fontSize = (int) (defaultFontSize * scale);
        arrowSize = (int) (defaultArrowSize * scale);
    }
}
