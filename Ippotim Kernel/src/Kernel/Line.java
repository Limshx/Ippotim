package Kernel;

class Line {
    Rectangle from;
    private Rectangle to;
    private static int arrowSize;
    private static final int defaultArrowSize = 16;

    Line(Rectangle from, Rectangle to) {
        this.from = from;
        this.to = to;
    }

    static void updateSize() {
        arrowSize = (int) (defaultArrowSize * Adapter.scale);
    }

    private int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    void draw() {
        int xFrom = from.x + Rectangle.width / 2;
        int yFrom = from.y + Rectangle.height / 2;
        int xTo = to.x + Rectangle.width / 2;
        int yTo = to.y + Rectangle.height / 2;
        Adapter.graphicsOperation.drawLine(xFrom, yFrom, xTo, yTo);
        int xa, ya, xb, yb;
        int distance = Math.abs(distance(xFrom, yFrom, xTo, yTo));
        xa = xTo + arrowSize * ((xFrom - xTo) + (yFrom - yTo) / 2) / distance;
        ya = yTo + arrowSize * ((yFrom - yTo) - (xFrom - xTo) / 2) / distance;
        xb = xTo + arrowSize * ((xFrom - xTo) - (yFrom - yTo) / 2) / distance;
        yb = yTo + arrowSize * ((yFrom - yTo) + (xFrom - xTo) / 2) / distance;

        Adapter.graphicsOperation.drawLine(xTo, yTo, xa, ya);
        Adapter.graphicsOperation.drawLine(xTo, yTo, xb, yb);
    }
}
