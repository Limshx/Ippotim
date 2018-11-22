package Kernel;

class Rectangle {
    static Color currentGroupColor; // 用来储存当前导入的TreeNode组的颜色，本来是打算用int表示之用以还原组号与颜色的对应关系，0为主函数、1为结构定义、2为函数定义，不过还是直接存RectStringColor方便
    Color color;
    int x, y;
    private static final int defaultWidth = 200;
    private static final int defaultHeight = 40;
    private static final int defaultFontSize = 32;
    String content;

    Rectangle(String content, int x, int y) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.color = currentGroupColor;
    }

    static int getWidth() {
        return (int) (defaultWidth * Adapter.scale);
    }

    static int getHeight() {
        return (int) (defaultHeight * Adapter.scale);
    }

    static int getFontSize() {
        return (int) (defaultFontSize * Adapter.scale);
    }

    void moveTo(int xv, int yv) {
        this.x += xv;
        this.y += yv;
    }

    void draw(GraphicsOperation graphicsOperation) {
        int textWidth = graphicsOperation.getTextLength(content, getFontSize());
        graphicsOperation.fillRect(x, y, textWidth < getWidth() ? getWidth() : textWidth, getHeight(), color.rectangleColor);
        graphicsOperation.drawRect(x, y, textWidth < getWidth() ? getWidth() : textWidth, getHeight());
        graphicsOperation.drawString(content, x, y + getHeight() * 4 / 5, color.stringColor);
    }
}
