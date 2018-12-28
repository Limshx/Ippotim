package com.limshx.ippotim.kernel;

import java.util.ArrayList;

class Syntax {
    static String[] defaultKeywords = {"void", "if", "else", "while", "break", "continue", "return", "input", "output"};
    static String[] currentKeywords = new String[defaultKeywords.length];

    static boolean isKeyword(String s) {
        for (String currentKeyword : currentKeywords) {
            if (currentKeyword.equals(s)) {
                return true;
            }
        }
        return false;
    }

    static ArrayList<String> getRegularElements(String s) {
        String separator = " ";
        ArrayList<String> regularElements = new ArrayList<>();
        int preIndex = 0;
        boolean hasQuote = false;
        for (int i = 0; i < s.length() - 1; i++) {
            if ('"' == s.charAt(i)) {
                hasQuote = !hasQuote;
            }
            if (s.substring(i).startsWith(separator) && !hasQuote) {
                String element = s.substring(preIndex, i);
                regularElements.add(element);
                preIndex = i + separator.length();
            }
        }
        regularElements.add(s.substring(preIndex));
        return regularElements;
    }

    private static String getParameters(List function) {
        String[] strings = List.getListHead(function).split(" ", 3);
        return 3 == strings.length ? strings[2] : "";
    }

    private static ArrayList<String> getFormalInstanceNames(String parameters) {
        ArrayList<String> formalInstanceNames = new ArrayList<>();
        if (!parameters.equals("")) {
            String[] formalParameters = parameters.split(", ");
            for (String formalParameter : formalParameters) {
                String[] typeAndName = formalParameter.split(" ");
                formalInstanceNames.add(typeAndName[1]);
            }
        }
        return formalInstanceNames;
    }

    // 函数调用如果还是由变量类型序列确定的话，似乎得运行时才能够做到了。其实不需要，至少对于现在的设计而言，通过处理之前的定义语句即可。这里要注意的是：getCommandType()应当是面向一个TreeNode组的，毕竟main函数也是函数，而函数被设计成比较完美的沙盒机制。至于说使用传统的函数名设计，其实也要校验参数类型，所以直接砍掉函数名也是合理的。这个首先需要找到执行到函数调用语句前执行到的所有语句，可以倒推回去，先找到所在组的首结点，然后找到所在子句的主句，然后再找到主句所在的组的首结点，得到所有的定义语句；难在根据子句找主句。可以遍历vectorLine找到主句的矩形编号，然后遍历vectorRectangle找到组号。这样是可行的，但效率太低了，不如修改TreeNode定义，添加一个指向主句的指针，空间复杂度换时间复杂度。即便如此，对于每一个函数调用语句都回溯似乎也是不可忍受的，只能深度优先遍历之对于函数集合中的每一个函数进行深度优先遍历，这样指向前驱结点的指针preTreeNode也可以不要了。
    // 之所以要添加预编译就是想要去掉频繁查表的开销，然而还有1张表没有去掉，这就是变量表。变量表似乎不能通过预编译去掉，像C语言对于变量的处理或者说实现应该是借助各种寻址方式，其实变量都是由定义语句生成的，如此说来变量确实可以在预编译时
    static StatementType getStatementType(TreeNode statement) {
        int size = statement.elements.size();
        String fistElement = statement.elements.get(0);
        // 函数调用语句在定义语句前认定，是因为函数调用语句一般比定义语句多，减少不必要的判断。
        if ('{' == fistElement.charAt(0) && '}' == fistElement.charAt(fistElement.length() - 1)) {
            String functionName = fistElement.substring(1, fistElement.length() - 1);
            if (Adapter.functions.containsKey(functionName)) {
                statement.matchedFunction = Adapter.functions.get(functionName);
                statement.elements.set(0, functionName);
                String parameters = getParameters(statement.matchedFunction);
                statement.elements.addAll(getFormalInstanceNames(parameters));
                return StatementType.FUNCTION_CALL;
            }
        }
        // 赋值语句认定放在定义语句认定之前就可以让变量名与结构名重名了。
        if (3 == size && statement.elements.get(1).equals("=")) {
            return StatementType.ASSIGN;
        }
        // 惟有定义语句是前两个元素都不是关键字而且第一个元素不是被花括号包起来的。
        if (Adapter.structures.containsKey(fistElement)) {
            // 结构定义还是通过结构名查表得，不然函数调用的时候新建Instance不好传入指向结构定义的链表
            // command.matchedStructureOrFunction = setStructures.get(command.elements.get(0));
            return StatementType.DEFINE; // 定义语句需要查结构表，实际可以像函数调用那样处理之直接获取结构所在组号这样就不用查了，令结构所在组号为g，则预编译号可以设计成(-8-g)。不过只是得到组号的话还是要根据组号查表才能得到数据，这与根据结构名查表似乎没什么两样。除非将结构所在的组置为定义语句的子句，这样就还需要设置一个标志位表示该子句是临时的，第二次运行时执行本函数会检查该标志位，子句是临时的则先将子句置为null。这样也不用保存组号了，函数调用也应这样处理。其实这样也不好，最好是新增一个链表元素。由于不用保存组号了，这样就可以用enum保存语句类型了。
        }
        if (2 == size && fistElement.equals(currentKeywords[1])) {
            return StatementType.IF;
        }
        if (1 == size && fistElement.equals(currentKeywords[2])) {
            return StatementType.ELSE;
        }
        if (2 == size && fistElement.equals(currentKeywords[3])) {
            return StatementType.WHILE;
        }
        if (1 == size && fistElement.equals(currentKeywords[4])) {
            return StatementType.BREAK;
        }
        if (1 == size && fistElement.equals(currentKeywords[5])) {
            return StatementType.CONTINUE;
        }
        if (1 == size && fistElement.equals(currentKeywords[6])) {
            return StatementType.RETURN;
        }
        if (fistElement.equals(currentKeywords[7])) {
            return StatementType.INPUT;
        }
        if (fistElement.equals(currentKeywords[8])) {
            return StatementType.OUTPUT;
        }
        if (fistElement.equals("//")) {
            return  StatementType.COMMENT;
        }
        return null;
    }
}
