package com.limshx.ippotim.kernel;

import java.util.ArrayList;

class TreeNode {
    Rectangle rectangle; // 矩形与TreeNode绑定，直接使用矩形对象而不是编号更好
    StatementType statementType;
    ArrayList<String> elements;
    // 函数调用是设计成只提供参数列表之根据所有参数的类型确定要调用的函数的，本身是没有子句的，但预编译设计成函数中的语句成为函数调用语句的子句。这样问题就来了，这与if和else和while的子句不同，是一种多对一的子句，且子句可以独立存在，这样当函数调用语句改变或函数参数定义改变子句联系关系就要同步改变，这样实现起来会很麻烦。不过好在函数的调用执行不是简单的语句替换，而是比较完美的沙盒机制，这样还是比如n的函数调用对应的elements的元素有x和n的好之x是函数调用的预编译编号，这里直接取函数所属组号。至于点击函数调用所在的矩形反色显示其调用函数定义所在矩形的功能，选中矩形时查看预编译结果之查看elements中的预编译编号，然后找到要调用的函数即可。然而由于结构定义和函数定义直接影响语句类型的判断，又不能每次改动都全部刷新，所以只能在点击运行后再批量刷新elements。
//    TreeNode preTreeNode;
    List matchedFunction;
    List list;

    void updateElements() {
        // 先将matchedFunction置空是必要的，不然
        matchedFunction = null;
        elements = Syntax.getRegularElements(rectangle.getContent());
        statementType = Syntax.getStatementType(this);
    }
}
