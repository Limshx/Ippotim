package Kernel;

class Arrow {
    private int xFrom, yFrom;
    List list;

    Arrow(int x, int y, List list) {
        xFrom = x + Rectangle.width / 2;
        yFrom = y + Rectangle.height / 2;
        this.list = list;
    }

    private int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    void draw() {
        int xTo, yTo;
        xTo = list.x + Rectangle.width / 2;
        yTo = list.y + Rectangle.height / 2;
        Adapter.graphicsOperations.drawLine(xFrom, yFrom, xTo, yTo);
        int xa, ya, xb, yb;
        int distance = Math.abs(distance(xFrom, yFrom, xTo, yTo));
        xa = xTo + Rectangle.arrowSize * ((xFrom - xTo) + (yFrom - yTo) / 2) / distance;
        ya = yTo + Rectangle.arrowSize * ((yFrom - yTo) - (xFrom - xTo) / 2) / distance;
        xb = xTo + Rectangle.arrowSize * ((xFrom - xTo) - (yFrom - yTo) / 2) / distance;
        yb = yTo + Rectangle.arrowSize * ((yFrom - yTo) + (xFrom - xTo) / 2) / distance;
        Adapter.graphicsOperations.drawLine(xTo, yTo, xa, ya);
        Adapter.graphicsOperations.drawLine(xTo, yTo, xb, yb);
    }
}
