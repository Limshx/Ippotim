package Kernel;

import java.util.LinkedList;

class List {
    static LinkedList<List> lists = new LinkedList<>();
    Integer x, y;
    LinkedList<TreeNode> treeNodes = new LinkedList<>();
    List preList;

    List(List list) {
        preList = list;
        registerList(this);
    }

    List(int x, int y, List list) {
        this.x = x;
        this.y = y;
        preList = list;
        registerList(this);
    }

    static void unregisterList(List list) {
        lists.remove(list);
        list.treeNodes.clear();
    }

    private void registerList(List list) {
        lists.add(list);
    }
}
