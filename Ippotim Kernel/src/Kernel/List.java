package Kernel;

import java.util.LinkedList;

class List {
    Integer x, y;
    LinkedList<TreeNode> treeNodes = new LinkedList<>();

    List() {}

    List(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
