package Kernel;

class Rectangle {
    static Color currentGroupColor; // 用来储存当前导入的TreeNode组的颜色，本来是打算用int表示之用以还原组号与颜色的对应关系，0为主函数、1为结构定义、2为函数定义，不过还是直接存RectStringColor方便
    Color color;
    int x, y;
    static int width;
    static int height;
    private static int fontSize;
    private static final int defaultWidth = 200;
    private static final int defaultHeight = 40;
    private static final int defaultFontSize = 32;
    private String content;
    int pixelWidth;

    Rectangle(String content, int x, int y) {
        this.x = x;
        this.y = y;
        this.color = currentGroupColor;
        setContent(content);
    }

    static void updateSize() {
        width = (int) (defaultWidth * Adapter.scale);
        height = (int) (defaultHeight * Adapter.scale);
        fontSize = (int) (defaultFontSize * Adapter.scale);
    }

    void moveTo(int xv, int yv) {
        this.x += xv;
        this.y += yv;
    }

    void setContent(String s) {
        content = s;
        pixelWidth = Adapter.graphicsOperations.getTextLength(content, fontSize);
        pixelWidth = pixelWidth < width ? width : pixelWidth;
    }

    String getContent() {
        return content;
    }

    void draw() {
        Adapter.graphicsOperations.fillRect(x, y, pixelWidth, height, color.rectangleColor);
        Adapter.graphicsOperations.drawRect(x, y, pixelWidth, height);
        Adapter.graphicsOperations.drawString(content, x, y + height * 4 / 5, color.stringColor);
    }
}
