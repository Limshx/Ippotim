package com.limshx.ippotim.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

class Syntax {
    static String[] defaultKeywords = {"void", "if", "else", "while", "break", "continue", "return", "input", "output", "NULL"};
    static String[] currentKeywords = new String[defaultKeywords.length];

    // 只需要确保结构名和关键字不重名即可，之共用名池。
    static boolean isKeyword(String s) {
        for (String keyword : currentKeywords) {
            if (keyword.equals(s)) {
                return true;
            }
        }
        return false;
    }

    // 罗列出特殊字符是因为需要支持标识符中出现汉字和表情，而不能直接判断是不是字母或数字，当然也可以先判是不是ASCII码，但只是让标识符不会影响到解释器解析即可，这样直截了当也好。
    private static char[] forbiddenChars = {'.', '"', '?', '{', '}', '[', ']', '(', ')', '<', '>', '=', '!', '&', '|', '+', '-', '*', '/'};
    // 只有结构名和变量名需要判断，函数名不用，因为函数调用时函数名是放在花括号里的。
    static boolean isInvalidIdentifier(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            boolean containsForbiddenChar = false;
            for (char forbiddenChar : forbiddenChars) {
                if (-1 != s.indexOf(forbiddenChar)) {
                    containsForbiddenChar = true;
                    break;
                }
            }
            return containsForbiddenChar;
        }
    }

    // 不能replaceAll替换连续空格，得放在这里实现。既然新增和修改语句都会更新元素，似乎就不用运行前再统一更新了，只是当结构头和函数头改变，变量该如何同步改变元素。
    static ArrayList<String> getRegularElements(String s) {
        ArrayList<String> regularElements = new ArrayList<>();
        int preIndex = 0;
        boolean hasQuote = false;
        for (int i = 0; i < s.length() - 1; i++) {
            if ('"' == s.charAt(i)) {
                hasQuote = !hasQuote;
            }
            if (' ' == s.charAt(i) && !hasQuote) {
                if (' ' != s.charAt(i - 1)) {
                    String element = s.substring(preIndex, i);
                    regularElements.add(element);
                    preIndex = i + 1;
                } else {
                    preIndex += 1;
                }
            }
        }
        regularElements.add(s.substring(preIndex));
        return regularElements;
    }

    private static String getParameters(List function) {
        String[] strings = List.getListHead(function).split(" ", 3);
        return 3 == strings.length ? strings[2] : "";
    }

    private static LinkedList<String> getFormalInstanceNames(String parameters) {
        LinkedList<String> formalInstanceNames = new LinkedList<>();
        if (!parameters.equals("")) {
            String[] formalParameters = parameters.split(", ");
            for (String formalParameter : formalParameters) {
                String[] typeAndName = formalParameter.split(" ");
                formalInstanceNames.add(typeAndName[1]);
            }
        }
        return formalInstanceNames;
    }

    // 保存statementType为空或为StatementType.DEFINE或为StatementType.FUNCTION_CALL的TreeNode，结构头和函数头新建或删除或改变时更新这些TreeNode的statementType。这里本来是要分情况讨论提高效率的，不过其实效率要求并不高，先不管，算是代码清晰度换时间复杂度。
    static LinkedList<TreeNode> mutableStatements = new LinkedList<>();
    static void updateMutableStatements() {
        LinkedList<TreeNode> list = mutableStatements;
        mutableStatements = new LinkedList<>();
        for (TreeNode t : list) {
            updateStatementType(t);
        }
    }

    // 函数调用如果还是由变量类型序列确定的话，似乎得运行时才能够做到了。其实不需要，至少对于现在的设计而言，通过处理之前的定义语句即可。这里要注意的是：getCommandType()应当是面向一个TreeNode组的，毕竟main函数也是函数，而函数被设计成比较完美的沙盒机制。至于说使用传统的函数名设计，其实也要校验参数类型，所以直接砍掉函数名也是合理的。这个首先需要找到执行到函数调用语句前执行到的所有语句，可以倒推回去，先找到所在组的首结点，然后找到所在子句的主句，然后再找到主句所在的组的首结点，得到所有的定义语句；难在根据子句找主句。可以遍历vectorLine找到主句的矩形编号，然后遍历vectorRectangle找到组号。这样是可行的，但效率太低了，不如修改TreeNode定义，添加一个指向主句的指针，空间复杂度换时间复杂度。即便如此，对于每一个函数调用语句都回溯似乎也是不可忍受的，只能深度优先遍历之对于函数集合中的每一个函数进行深度优先遍历，这样指向前驱结点的指针preTreeNode也可以不要了。
    // 之所以要添加预编译就是想要去掉频繁查表的开销，然而还有1张表没有去掉，这就是变量表。变量表似乎不能通过预编译去掉，像C语言对于变量的处理或者说实现应该是借助各种寻址方式，其实变量都是由定义语句生成的，如此说来变量确实可以在预编译时
    static void updateStatementType(TreeNode statement) {
        if (StatementType.FUNCTION_CALL == statement.statementType) {
            statement.elements = getRegularElements(statement.getContent());
        }
        int size = statement.elements.size();
        String fistElement = statement.elements.get(0);
        // 赋值语句认定放在定义语句认定之前就可以让变量名与结构名重名了。
        if (3 == size && statement.elements.get(1).equals("=")) {
            statement.statementType = StatementType.ASSIGN;
            return;
        }
        if (2 == size && fistElement.equals(currentKeywords[1])) {
            statement.statementType = StatementType.IF;
            return;
        }
        if (1 == size && fistElement.equals(currentKeywords[2])) {
            statement.statementType = StatementType.ELSE;
            return;
        }
        if (2 == size && fistElement.equals(currentKeywords[3])) {
            statement.statementType = StatementType.WHILE;
            return;
        }
        if (1 == size && fistElement.equals(currentKeywords[4])) {
            statement.statementType = StatementType.BREAK;
            return;
        }
        if (1 == size && fistElement.equals(currentKeywords[5])) {
            statement.statementType = StatementType.CONTINUE;
            return;
        }
        if (1 == size && fistElement.equals(currentKeywords[6])) {
            statement.statementType = StatementType.RETURN;
            return;
        }
        if (2 <= size && fistElement.equals(currentKeywords[7])) {
            statement.statementType = StatementType.INPUT;
            return;
        }
        if (fistElement.equals(currentKeywords[8])) {
            statement.statementType = StatementType.OUTPUT;
            return;
        }
        if (fistElement.equals("//")) {
            statement.statementType =  StatementType.COMMENT;
            return;
        }
        // 惟有定义语句是前两个元素都不是关键字而且第一个元素不是被花括号包起来的。
        if (2 <= size && Adapter.structures.containsKey(fistElement)) {
            // 检查变量名合法性
            Executor.stop = false;
            Instance.putInstance(new HashMap<>(), statement);
            if (!Executor.stop) {
                statement.statementType = StatementType.DEFINE; // 定义语句需要查结构表，实际可以像函数调用那样处理之直接获取结构所在组号这样就不用查了，令结构所在组号为g，则预编译号可以设计成(-8-g)。不过只是得到组号的话还是要根据组号查表才能得到数据，这与根据结构名查表似乎没什么两样。除非将结构所在的组置为定义语句的子句，这样就还需要设置一个标志位表示该子句是临时的，第二次运行时执行本函数会检查该标志位，子句是临时的则先将子句置为null。这样也不用保存组号了，函数调用也应这样处理。其实这样也不好，最好是新增一个链表元素。由于不用保存组号了，这样就可以用enum保存语句类型了。
                mutableStatements.add(statement);
                return;
            } else {
                Executor.stop = false;
            }
        }
        if ('{' == fistElement.charAt(0) && '}' == fistElement.charAt(fistElement.length() - 1)) {
            String functionName = fistElement.substring(1, fistElement.length() - 1);
            if (Adapter.functions.containsKey(functionName)) {
                List function = Adapter.functions.get(functionName);
                // 检查实参个数与形参个数是否相等
                if (statement.elements.size() == function.treeNodes.get(0).elements.size() / 2) {
                    statement.matchedFunction = function;
                    // 这一句的意思是先把类似{f}改为f，这样调用的时候就不用临时剥离了。一时忘了，不记得有什么用了，删了后报空指针异常，最后才看到，可见注释很重要。
                    statement.elements.set(0, functionName);
                    String parameters = getParameters(statement.matchedFunction);
                    statement.elements.addAll(getFormalInstanceNames(parameters));
                    statement.statementType = StatementType.FUNCTION_CALL;
                    mutableStatements.add(statement);
                    return;
                }
            }
        }
        statement.statementType = null;
        mutableStatements.add(statement);
    }
}
