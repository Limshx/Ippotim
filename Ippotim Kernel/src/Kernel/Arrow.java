package Kernel;

class Arrow {
    private int xFrom, yFrom;
    private int xTo, yTo;

    Arrow(int x1, int y1, int x2, int y2) {
        xFrom = x1 + Rectangle.width / 2;
        yFrom = y1 + Rectangle.height / 2;
        xTo = x2 + Rectangle.width / 2;
        yTo = y2 + Rectangle.height / 2;
    }

    private int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    void draw() {
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
