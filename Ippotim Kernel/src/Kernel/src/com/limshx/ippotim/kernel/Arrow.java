package com.limshx.ippotim.kernel;

class Arrow {
    private int xFrom, yFrom;
    List list;

    Arrow(int x, int y, List list) {
        xFrom = x + Size.width / 2;
        yFrom = y + Size.height / 2;
        this.list = list;
    }

    // 本来想要判断箭头是否与屏幕之矩形有交点，但这样计算似乎会比较麻烦，得不偿失，最终决定简单判断箭头的首尾是否都不在屏幕之矩形内。
    private boolean inScreen(int x, int y) {
        return 0 <= x && x <= Adapter.width && 0 <= y && y <= Adapter.height;
    }

    private int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    void draw() {
        int xTo, yTo;
        xTo = list.x + Size.width / 2;
        yTo = list.y + Size.height / 2;
        if (inScreen(xFrom, yFrom) || inScreen(xTo, yTo)) {
            Adapter.graphicsOperations.drawLine(xFrom, yFrom, xTo, yTo);
            int xa, ya, xb, yb;
            int distance = Math.abs(distance(xFrom, yFrom, xTo, yTo));
            xa = xTo + Size.arrowSize * ((xFrom - xTo) + (yFrom - yTo) / 2) / distance;
            ya = yTo + Size.arrowSize * ((yFrom - yTo) - (xFrom - xTo) / 2) / distance;
            xb = xTo + Size.arrowSize * ((xFrom - xTo) - (yFrom - yTo) / 2) / distance;
            yb = yTo + Size.arrowSize * ((yFrom - yTo) + (xFrom - xTo) / 2) / distance;
            Adapter.graphicsOperations.drawLine(xTo, yTo, xa, ya);
            Adapter.graphicsOperations.drawLine(xTo, yTo, xb, yb);
        }
    }
}
