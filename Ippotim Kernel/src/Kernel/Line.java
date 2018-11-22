package Kernel;

class Line {
    Rectangle from;
    private Rectangle to;
    private static final int defaultArrowLen = 16;

    Line(Rectangle from, Rectangle to) {
        this.from = from;
        this.to = to;
    }

    private static int getArrowLen() {
        return (int) (defaultArrowLen * Adapter.scale);
    }

    private int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    void draw(GraphicsOperation graphicsOperation) {
        int xFrom = from.x + Rectangle.getWidth() / 2;
        int yFrom = from.y + Rectangle.getHeight() / 2;
        int xTo = to.x + Rectangle.getWidth() / 2;
        int yTo = to.y + Rectangle.getHeight() / 2;
        graphicsOperation.drawLine(xFrom, yFrom, xTo, yTo);
        int xa, ya, xb, yb;
        int distance = Math.abs(distance(xFrom, yFrom, xTo, yTo));
        xa = xTo + getArrowLen() * ((xFrom - xTo) + (yFrom - yTo) / 2) / distance;
        ya = yTo + getArrowLen() * ((yFrom - yTo) - (xFrom - xTo) / 2) / distance;
        xb = xTo + getArrowLen() * ((xFrom - xTo) - (yFrom - yTo) / 2) / distance;
        yb = yTo + getArrowLen() * ((yFrom - yTo) + (xFrom - xTo) / 2) / distance;

        graphicsOperation.drawLine(xTo, yTo, xa, ya);
        graphicsOperation.drawLine(xTo, yTo, xb, yb);
    }
}
