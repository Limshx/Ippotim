package Kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

class List {
    static Color currentGroupColor; // 用来储存当前导入的TreeNode组的颜色，本来是打算用int表示之用以还原组号与颜色的对应关系，0为主函数、1为结构定义、2为函数定义，不过还是直接存RectStringColor方便
    Color color;
    static LinkedList<List> lists = new LinkedList<>();
    Integer x, y;
    ArrayList<TreeNode> treeNodes = new ArrayList<>();
    List preList;
    // 局部变量表跟if或while语句之也即跟TreeNode绑定会很难处理好全局变量表，能想到跟List绑定是真厉害。
    // 套Stack是用以处理递归函数。
    // 用顺序栈简直就是绝配。
    // 调用层级初始化为-1而不是0是为了方便编号
    // 似乎还是得用哈希表，否则子句的层级很难控制
    int level = -1;
    private HashMap<Integer, HashMap<String, Instance>> hashMap = new HashMap<>();

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

    HashMap<String, Instance> getInstances() {
        int currentFunctionLevel = Executor.functionStack.lastElement().level;
        HashMap<String, Instance> instances = hashMap.get(currentFunctionLevel);
        if (null != instances) {
            return instances;
        } else {
            instances = new HashMap<>();
            hashMap.put(currentFunctionLevel, instances);
            return instances;
        }
    }

    void putInstances(HashMap<String, Instance> instances) {
        hashMap.put(level, instances);
    }

    static void unregisterList(List list) {
        lists.remove(list);
        list.treeNodes.clear();
    }

    private void registerList(List list) {
        if (null != preList) {
            // 子句才铺上一层局部变量表，这样主要是考虑到函数的变量表不能简单空建，然后发现顺便屏蔽掉结构定义的list了，nice！
            // 这里多铺几层对递归函数运行效率会有好处，当然由于并不清空，第二次运行即可得到足够多的层数，本来想可以让用户自己设置要铺几层的，想想还是算了吧。
            hashMap.put(0, new HashMap<>());
        } else {
            // 子句不加入lists豪华套餐
            lists.add(list);
        }
    }
}
