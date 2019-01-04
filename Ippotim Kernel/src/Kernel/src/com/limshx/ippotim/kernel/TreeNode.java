package com.limshx.ippotim.kernel;

import java.util.ArrayList;

class TreeNode {
    static TreeNode tail = new TreeNode("+", false);
    private String content;
    int pixelWidth;
    StatementType statementType;
    ArrayList<String> elements;
    // 函数调用是设计成只提供参数列表之根据所有参数的类型确定要调用的函数的，本身是没有子句的，但预编译设计成函数中的语句成为函数调用语句的子句。这样问题就来了，这与if和else和while的子句不同，是一种多对一的子句，且子句可以独立存在，这样当函数调用语句改变或函数参数定义改变子句联系关系就要同步改变，这样实现起来会很麻烦。不过好在函数的调用执行不是简单的语句替换，而是比较完美的沙盒机制，这样还是比如n的函数调用对应的elements的元素有x和n的好之x是函数调用的预编译编号，这里直接取函数所属组号。至于点击函数调用所在的矩形反色显示其调用函数定义所在矩形的功能，选中矩形时查看预编译结果之查看elements中的预编译编号，然后找到要调用的函数即可。然而由于结构定义和函数定义直接影响语句类型的判断，又不能每次改动都全部刷新，所以只能在点击运行后再批量刷新elements。
//    TreeNode preTreeNode;
    List matchedFunction;
    List list;

    TreeNode(String content) {
        // 如何判断结点是不是首结点，或者说是不是结构头或函数头。之前是!content.isEmpty()之判断内容为空，这是main函数和子句而已，于是又想着各种奇技淫巧之为TreeNode增加boolean元素记录是否第一个结点云云。
        this(content, Adapter.selectedList.treeNodes.isEmpty());
    }

    private TreeNode(String content, boolean isHead) {
        update(content, isHead);
    }

    void setContent(String s) {
        content = s;
        pixelWidth = Adapter.graphicsOperations.getPixelWidth(content, Size.fontSize);
        pixelWidth = pixelWidth < Size.width ? Size.width : pixelWidth;
    }

    String getContent() {
        return content;
    }

    private String getRegularContent() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String element : elements) {
            stringBuilder.append(element).append(" ");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }

    void update(String content, boolean isHead) {
        elements = Syntax.getRegularElements(content);
        setContent(getRegularContent());
        if (isHead) {
            statementType = StatementType.HEAD;
        } else {
            Syntax.updateStatementType(this);
        }
    }

    void draw(int x, int y, boolean drawRect, int rectangleColor, int stringColor) {
        Adapter.graphicsOperations.fillRect(x, y, pixelWidth, Size.height, rectangleColor);
        if (drawRect) {
            Adapter.graphicsOperations.drawRect(x, y, pixelWidth, Size.height, stringColor);
        }
        Adapter.graphicsOperations.drawString(content, x, y + Size.height * 4 / 5, stringColor);
    }
}
