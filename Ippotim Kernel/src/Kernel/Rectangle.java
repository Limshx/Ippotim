package Kernel;

class Rectangle {
    static int width;
    static int height;
    static int arrowSize;
    private static int fontSize;
    static double scale;
    static double defaultScale;
    private static final int defaultWidth = 200;
    private static final int defaultHeight = 40;
    private static final int defaultFontSize = 32;
    private static final int defaultArrowSize = 16;
    private String content;
    int pixelWidth;
    static Rectangle tail;

    Rectangle(String content) {
        setContent(content);
    }

    static void updateSize() {
        width = (int) (defaultWidth * scale);
        height = (int) (defaultHeight * scale);
        fontSize = (int) (defaultFontSize * scale);
        arrowSize = (int) (defaultArrowSize * scale);
    }

    void setContent(String s) {
        content = s;
        pixelWidth = Adapter.graphicsOperations.getPixelWidth(content, fontSize);
        pixelWidth = pixelWidth < width ? width : pixelWidth;
    }

    String getContent() {
        return content;
    }

    void draw(int x, int y, boolean drawRect, int rectangleColor, int stringColor) {
        Adapter.graphicsOperations.fillRect(x, y, pixelWidth, height, rectangleColor);
        if (drawRect) {
            Adapter.graphicsOperations.drawRect(x, y, pixelWidth, height, stringColor);
        }
        Adapter.graphicsOperations.drawString(content, x, y + height * 4 / 5, stringColor);
    }
}
