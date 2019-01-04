package com.limshx.ippotim.kernel;

import java.util.ArrayList;
import java.util.LinkedList;

class List {
    static Color currentGroupColor; // 用来储存当前导入的TreeNode组的颜色，本来是打算用int表示之用以还原组号与颜色的对应关系，0为主函数、1为结构定义、2为函数定义，不过还是直接存RectStringColor方便
    Color color;
    static LinkedList<List> lists = new LinkedList<>();
    Integer x, y;
    ArrayList<TreeNode> treeNodes = new ArrayList<>();
    List preList;

    List(List list) {
        color = currentGroupColor;
        preList = list;
        registerList(this);
    }

    List(int x, int y, List list) {
        color = currentGroupColor;
        this.x = x;
        this.y = y;
        preList = list;
        registerList(this);
    }

    static String getListHead(List list) {
        return list.treeNodes.get(0).getContent();
    }

    static void unregisterList(List list) {
        lists.remove(list);
        list.treeNodes.clear();
    }

    private void registerList(List list) {
        Adapter.selectedList = list;
        Adapter.selectedTreeNodeIndex = 0;
        // 子句不加入lists豪华套餐
        if (null == preList) {
            lists.add(list);
        }
    }
}
