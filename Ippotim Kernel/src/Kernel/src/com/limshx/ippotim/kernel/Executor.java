package com.limshx.ippotim.kernel;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

class Executor {
    static boolean stop;
    // 0是正常，1是break，2是continue，3是return。
    private int statementsCtrl;

    private Instance getArrayInstance(HashMap<String, Instance> instances, Instance instance, String arrayPart) {
        if (!arrayPart.equals("")) {
            LinkedList<String> arrayChains = getChains(arrayPart, false);
            StringBuilder nameOfArrayInstanceBuilder = new StringBuilder();
            for (String arrayChain : arrayChains) {
                nameOfArrayInstanceBuilder.append(getNumber(instances, arrayChain)).append(" ");
            }
            String nameOfArrayInstance = nameOfArrayInstanceBuilder.deleteCharAt(nameOfArrayInstanceBuilder.length() - 1).toString();
            // arrayElements为null的时候其实elements也是null，类似S s; s[0].next[0] = s[0];的时候会为null，因为添加实例的elements的时候因为不能死循环所以initElements是false。
//            if (null == instance.arrayElements) {
//                instance.arrayElements = new HashMap<>();
//            }
            Instance instanceTemp = instance.getArrayElements().get(nameOfArrayInstance);
            if (null == instanceTemp) {
                Instance newInstance = new Instance(instance.type);
                instance.getArrayElements().put(nameOfArrayInstance, newInstance);
                instanceTemp = newInstance;
            }
            return instanceTemp;
        } else {
            return instance;
        }
    }

    // arrayOrElement：true是array，false是element。原来是elementOrArray，这样函数中一处是elementOrArray一处是!elementOrArray之正用反用一起出现，会把人绕晕之不清晰。
    private LinkedList<String> getChains(String instanceName, boolean elementOrArray) {
        LinkedList<String> chains = new LinkedList<>();
        int start = 0;
        int match = 0;
        char separator = elementOrArray ? '.' : ']';
        for (int i = 0; i < instanceName.length(); i++) {
            if ('[' == instanceName.charAt(i)) {
                match += 1;
            }
            if (']' == instanceName.charAt(i)) {
                match -= 1;
            }
            if (0 == match && separator == instanceName.charAt(i)) {
                chains.add(instanceName.substring(start + (elementOrArray ? 0 : 1), i));
                start = i + 1;
            }
        }
        if (elementOrArray) {
            chains.add(instanceName.substring(start));
        }
        return chains;
    }

    // 原始或者说一般形式是类似((a[a][a])[a])[a]
    // elementName[0]是零维数组名，也即上式左边第一个a；elementName[1]是第一层小括号里的字符串，也即上式的(a[a][a])[a]；elementName[2]是第一层小括号右边的字符串，也即上式的[a]。
    private String[] getElementName(String element) {
        String[] elementName = new String[3];
        elementName[0] = element;
        boolean done = false;
        while (elementName[0].startsWith("(")) {
            int match = 0;
            for (int i = 0; i < elementName[0].length(); i++) {
                if ('(' == elementName[0].charAt(i)) {
                    match += 1;
                } else if (')' == elementName[0].charAt(i)) {
                    match -= 1;
                }
                if (0 == match) {
                    if (')' == elementName[0].charAt(i)) {
                        elementName[0] = elementName[0].substring(1, i);
                        if (!done) {
                            done = true;
                            // substring可以传入总长度，不需要判断是否遍历到最后一个字符了
                            elementName[2] = element.substring(i + 1);
                            elementName[1] = elementName[0];
                        }
                    }
                }
            }
        }
        // 这里用if就可以了，不用while，这里体现了while是if的加强版
        if (elementName[0].endsWith("]")) {
            elementName[0] = elementName[0].split("\\[")[0];
        }
        if (null == elementName[1]) {
            elementName[2] = element.substring(elementName[0].length());
        }
        return elementName;
    }

    private Instance getInstance(HashMap<String, Instance> instances, Instance fatherInstance, String instanceName) {
        if (null != fatherInstance) {
            if (null != fatherInstance.getElements()) {
                return fatherInstance.getElements().get(instanceName);
            } else {
                Adapter.error("该实例不存在！");
            }
            return null;
        } else {
            return instances.get(instanceName);
        }
    }

    private Instance getElementInstance(HashMap<String, Instance> instances, Instance instance, String instanceName) {
        Instance elementInstance;
        String[] elementName = getElementName(instanceName);
//        System.out.println((null != instance ? instance.type : "null") + Arrays.toString(elementName));
        if (null != elementName[1]) {
            // 本来这里是headInstance，也就是第一层小括号里面的字符串对应的变量，要先计算或者说得到，不过直接用instance就成，压缩代码也是一种技巧或者说艺术或者说算法或者说方法论，所以会觉得很奇妙很能体现智慧。
            elementInstance = getElementInstance(instances, instance, elementName[1]);
        } else {
            elementInstance = getInstance(instances, instance, elementName[0]);
        }
        // 在getArrayInstance()前判空就不用在getArrayInstance()里面判了，getArrayInstance()肯定不会返回空，这也是一种优化，如何优化是一门学问。
        if (null == elementInstance) {
            return null;
        }
        return getArrayInstance(instances, elementInstance, elementName[2]);
        // 结构变量为null就是elements为空集
        // 现在设计成基本数据类型也有元素之其值就是其唯一的元素了，所以这里要判空也是从其结构体定义判，不过可能会导致性能进一步损失，直接删掉进入下面的统一流程更好
        // 给数据结构起复数名可以方便遍历时候临时变量命名
    }

    // 之前是方法重载实现局部变量功能，使用新方案后就不用了
    private Instance getInstance(HashMap<String, Instance> instances, String instanceName) {
        Instance instance = null;
        // 一般的形式是类似a[a[a+a].a[a+a]].a[a[a+a].a[a+a]]，这样是不能简单split的，需要像括号匹配那样进行整体分离。
        LinkedList<String> elementChains = getChains(instanceName, true);
        for (String elementChain : elementChains) {
            instance = getElementInstance(instances, instance, elementChain);
            if (null == instance) {
                Adapter.error("该实例不存在！");
                return null;
            }
        }
        return instance;
    }

    void run(HashMap<String, Instance> instances, List list) {
        // 从1开始才是语句
        for (int index = 1; index < list.treeNodes.size(); index++) {
            if (stop || 0 != statementsCtrl) {
                return;
            }
            TreeNode statement = list.treeNodes.get(index);
            Adapter.selectedList = list;
            Adapter.selectedTreeNodeIndex = index;
            if (null == statement.statementType) {
                Syntax.updateStatementType(statement);
                Adapter.error("无效的语句！");
            }
            switch (statement.statementType) {
                case DEFINE: {
                    // 这样就要求变量名不能与结构名有相同的，否则会导致数组越界异常，比如S S之定义结构S的一个变量S，然后S之调用结构S的一个函数，就会误认为这个S是结构名而非变量名，从而导致下面的parameters[1]数组越界
                    // 处理类似Set set之类的变量定义，暂时设计为定义集合变量的时候即分配存储空间之Set set等价于Set set = new Set()
                    Instance.putInstance(instances, statement);
                    break;
                }
                case ASSIGN: {
                    Instance to = getInstance(instances, statement.elements.get(0));
                    assign(instances, statement.elements.get(2), to, false);
                    break;
                }
                case INPUT: {
                    // 从1开始是因为0是“input”。
                    // input设计为赋值语句的手动模式，也即input后可接任意void变量，用户输入的可以是任意表达式和或者说包括任意变量名。
                    for (int i = 1; i < statement.elements.size(); i++) {
                        Instance instance = getInstance(instances, statement.elements.get(i));
                        if (null != instance && Syntax.currentKeywords[0].equals(instance.type)) {
                            setValue(instance, Adapter.graphicsOperations.getInput());
                        }
                    }
                    break;
                }
                case OUTPUT: {
                    // 前期先求把功能实现，越简单越好，不必强求强大精妙，过早优化是万恶之源，似己想要写操作系统也是想要一个可以扩展成足够强大的操作系统的教科书级的简易实现。
                    // output设计为只是输出常量和void变量的语句，这样似乎就与input不是一路人或者说互为相反数或者说阴阳了。
                    StringBuilder stringBuilder = new StringBuilder();
                    if (statement.elements.size() > 1) {
                        // 从1开始是因为0是“output”了，像这种以及case不加break等不合常规的行为都应添加注释或者说说明
                        for (int i = 1; i < statement.elements.size(); i++) {
                            String element = statement.elements.get(i);
                            Object value = getValue(instances, element);
                            if (null != value) {
                                stringBuilder.append(value);
                            } else {
                                Adapter.error("只有 \"" + Syntax.currentKeywords[0] + "\" 的实例才能被输出！");
                            }
                        }
                    } else {
                        // 当output单独成句之不接任何参数，就是输出换行符，这个设计简直巧夺天工。
                        stringBuilder.append("\n");
                    }
                    Adapter.graphicsOperations.appendText(stringBuilder.toString());
                    try {
                        Adapter.fileOutputStream.write(stringBuilder.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case IF: {
                    // 不管有没有else之子句最后一句是"else :"还是"+"，都是去头去尾
                    List listIf = statement.list;
                    // 如果没有else，得到的就是null
                    List listElse = statement.list.treeNodes.get(statement.list.treeNodes.size() - 1).list;
                    HashMap<String, Instance> subInstances = new HashMap<>(instances);
                    if (isTrue(instances, statement.elements.get(1))) {
                        run(subInstances, listIf);
                    } else {
                        if (null != listElse) {
                            run(subInstances, listElse);
                        }
                    }
                    break;
                }
                case WHILE: {
                    List listWhile = statement.list;
                    while (isTrue(instances, statement.elements.get(1))) {
                        // 必须每次新建，否则会报重定义错误。不报重定义错误或者说禁止重定义也不行，重定义全局变量会导致第一次运行与后续运行实际逻辑不同，这不合设计要求。之前特地设计了一个本地变量与全局变量分离的方案，也即将instances分成globalInstances和localInstances，发现全局变量应为所有父句的本地变量的并集。于是配合List的preList设计了一个看起来很优雅的链式本地变量方案，为每一个List实例添加一个本地变量表，从当前list开始到源list找变量直至找到为止，但这个方案需要处理好函数的递归调用，于是将List的本地变量表套上一个哈希表。后来发现本地变量表每次必须重建，就像现在这样，也是最初的那样，觉得与其弄着么复杂不如还是返璞归真了。
                        HashMap<String, Instance> subInstances = new HashMap<>(instances);
                        run(subInstances, listWhile);
                    }
                    break;
                }
                case BREAK: { // while语句之外也可以使用break，比如主函数中，相当于后面的直接不执行了，暂不设限制
                    statementsCtrl = 1;
                    break;
                }
                case CONTINUE: { // 有循环似乎就得有break和continue
                    statementsCtrl = 2;
                    break;
                }
                case CALL: {
                    run(instances, statement);
                    break;
                }
                case RETURN: {
                    statementsCtrl = 3;
                }
                default:
                    break;
            }
        }
    }

    private void run(HashMap<String, Instance> instances, TreeNode statement) {
        String functionName = statement.elements.get(0);
        // 要复制一份，不然递归调用的时候会影响到后面的语句。
        HashMap<String, Instance> templateFunctionInstances = Adapter.functionNameToInstances.get(functionName);
        HashMap<String, Instance> functionInstances = Instance.getFunctionInstances(templateFunctionInstances);
        if (!functionInstances.isEmpty()) {
            int size = statement.elements.size() / 2;
            // 第0个是函数名，第1个开始是实参名
            for (int i = 0; i < size; i++) {
                assign(instances, statement.elements.get(i + 1), functionInstances.get(statement.elements.get(size + i + 1)), true);
            }
        }
        run(functionInstances, statement.matchedFunction);
        if (3 == statementsCtrl) {
            statementsCtrl = 0;
        }
    }

    private void assign(Instance from, Instance to, boolean treatAsGeneral) {
        if (treatAsGeneral) {
            if (!to.type.equals(from.type)) {
                if (to.type.equals(Syntax.currentKeywords[0])) {
                    to.type = from.type;
                } else {
                    Adapter.error("无效的赋值！");
                    return;
                }
            }
            to.setElements(from.getElements());
        } else {
            setValue(to, getValue(from));
        }
    }

    private void assign(HashMap<String, Instance> instancesFrom, String stringFrom, Instance to) {
        setValue(to, getValue(instancesFrom, stringFrom));
    }

    private void assign(Instance instance) {
        if (instance.type.equals(Syntax.currentKeywords[0])) {
            setValue(instance, null);
        } else {
            instance.setElements(null);
        }
    }

    private void assign(HashMap<String, Instance> instancesFrom, String stringFrom, Instance to, boolean isFunctionCall) {
        Instance from;
        if (null != to) {
            if (stringFrom.equals(Syntax.currentKeywords[9])) {
                assign(to);
                return;
            }
            if (getType(stringFrom).equals("instance")) {
                from = getInstance(instancesFrom, stringFrom);
                if (null != from) {
                    boolean treatAsGeneral = !from.type.equals(Syntax.currentKeywords[0]) || !to.type.equals(Syntax.currentKeywords[0]) || isFunctionCall;
                    if (isFunctionCall) {
                        to.setArrayElements(from.getArrayElements());
                    }
                    assign(from, to, treatAsGeneral);
                }
            } else {
                assign(instancesFrom, stringFrom, to);
            }
        }
    }

    private int getNextStart(String s, int start) {
        int[] match = {0, 0};
        boolean hasQuote = false;
        for (int i = start; i < s.length(); i++) {
            if ('"' == s.charAt(i)) {
                hasQuote = !hasQuote;
            }
            // 处理双引号内出现小括号和中括号的情况，双引号内限制为不允许出现双引号了之不支持转义字符。
            // 字符串中允许出现任意字符，不设转义的话根本无法处理。像说英文双引号不区分左右无法像括号那样配对处理吧，好，我让你字符串用大括号括起来，之类似{XX OO}，那你字符串内部就是不能出现大括号了吧，那就没必要与大流相悖了。
            // 这样在低层级修补漏洞是很低效的，需要从文法上着手。
            if (hasQuote) {
                continue;
            }
            if ('(' == s.charAt(i)) {
                match[0] += 1;
            } else if (')' == s.charAt(i)) {
                match[0] -= 1;
            } else if ('[' == s.charAt(i)) {
                match[1] += 1;
            } else if (']' == s.charAt(i)) {
                match[1] -= 1;
            }
            if (0 == match[0] && 0 == match[1]) {
                return i + 1;
            }
        }
        return s.length();
    }

    private String getType(String s) {
        try {
            Integer.parseInt(s);
            return "number";
        } catch (NumberFormatException e) {
            int start = 0;
            int nextStart = 0;
            while (nextStart < s.length()) {
                nextStart = getNextStart(s, start);
                if (nextStart < s.length()) {
                    if ('+' == s.charAt(nextStart) || '-' == s.charAt(nextStart) || '*' == s.charAt(nextStart) || '/' == s.charAt(nextStart)) {
                        return "number";
                    }
                    if ('?' == s.charAt(nextStart)) {
                        return "string";
                    }
                } else if (0 == start) {
                    // 主体在一组小括号里面，这就要求非number类型的变量不能放在小括号内。首尾分别是左右括号主体却并非在小括号里的情况是类似(s).(s)或(s)&&(s)或(s)*(s)，其中布尔表达式不直接调用本函数，中间的数字运算符在上面已经判断过了。
                    if ('(' == s.charAt(0)) {
                        return "number";
                    }
                    // 双引号内不能有双引号
                    if ('"' == s.charAt(0)) {
                        return "string";
                    }
                }
                start = nextStart;
            }
            return "instance";
        }
    }

    private boolean isTheSame(HashMap<String, Instance> instances, String[] parameters) {
        if (getType(parameters[0]).equals("instance") && getType(parameters[1]).equals("instance")) {
            Instance[] instance = new Instance[2];
            instance[0] = getInstance(instances, parameters[0]);
            instance[1] = getInstance(instances, parameters[1]);
            if (null != instance[0] && null != instance[1]) {
                if (!instance[0].type.equals(Syntax.currentKeywords[0]) && !instance[1].type.equals(Syntax.currentKeywords[0])) {
                    return Objects.equals(instance[0].getElements(), instance[1].getElements());
                } else {
                    return Objects.equals(getValue(instance[0]), getValue(instance[1]));
                }
            }
            return false;
        } else {
            return Objects.equals(getValue(instances, parameters[0]), getValue(instances, parameters[1]));
        }
    }

    private boolean compare(HashMap<String, Instance> instances, String[] parameters, String operation) {
        switch (operation) {
            case "<": {
                return getNumber(instances, parameters[0]) < getNumber(instances, parameters[1]);
            }
            case "<=": {
                return getNumber(instances, parameters[0]) <= getNumber(instances, parameters[1]);
            }
            case "==": {
                return isTheSame(instances, parameters);
            }
            case "!=": {
                return !isTheSame(instances, parameters);
            }
            case ">=": {
                return getNumber(instances, parameters[0]) >= getNumber(instances, parameters[1]);
            }
            case ">": {
                return getNumber(instances, parameters[0]) > getNumber(instances, parameters[1]);
            }
            default: {
                return false;
            }
        }
    }

    private boolean isTrue(HashMap<String, Instance> instances, String booleanExpression, int start, int end, int preOperation, boolean result) {
        String subExpression = booleanExpression.substring(start, end);
        if (0 == preOperation) {
            return result && isTrue(instances, subExpression);
        } else {
            return result || isTrue(instances, subExpression);
        }
    }

    private boolean isTrue(HashMap<String, Instance> instances, String booleanExpression) {
        // break本就是针对或者说面向或者说基于while的，放到这里实现挺好
        if (1 == statementsCtrl) {
            statementsCtrl = 0;
            return false;
        }
        if (2 == statementsCtrl) {
            statementsCtrl = 0; // 似乎不用判断直接置为false即可或者说也是一样的
        }
        if (stop) {
            return false;
        }
        if (booleanExpression.startsWith("!")) {
            return !isTrue(instances, booleanExpression.substring(1));
        }

        boolean result = true;
        int preOperation = 0; // 0是“&&”，1是“||”
//        boolean metOperation = false;

        int start = 0;
        int nextStart = 0;
        while (nextStart < booleanExpression.length()) {
            nextStart = getNextStart(booleanExpression, nextStart);
            // 判断所在位置是否某字符是charAt(i)，是否某多字符除了可以判断charAt(i)、charAt(i + 1)、charAt(i + 2)...还可以类似substring(i).startsWith("&&")。
            if (nextStart < booleanExpression.length()) {
                String s = booleanExpression.substring(nextStart);
                if (s.startsWith("&&") || s.startsWith("||")) {
//                    metOperation = true;
                    result = isTrue(instances, booleanExpression, start, nextStart, preOperation, result);
                    preOperation = s.startsWith("&&") ? 0 : 1;
                    nextStart += 2; // 暂时写死为2
                    start = nextStart;
                }
            }
        }
        if (0 == start) {
            if ('(' == booleanExpression.charAt(0) && ')' == booleanExpression.charAt(booleanExpression.length() - 1)) {
                String subExpression = booleanExpression.substring(1, booleanExpression.length() - 1);
                return isTrue(instances, subExpression);
            } else {
                return isTrueBasic(instances, booleanExpression);
            }
        } else {
            result = isTrue(instances, booleanExpression, start, nextStart, preOperation, result);
        }
        return result;
    }

    // 布尔表达式设计成不允许字符串和数字直接或者说单独出现之类似if 1或if "1"，允许变量名和比较表达式。至于||与&&之类的复合布尔表达式，理应是要加上的，只是需要一种优雅的方法，最好与数字运算统一起来。一般的形式当然是有小括号有与或非，为简化设计，与或非皆用以连接子布尔表达式，除了单独出现的变量名之判断是否非空，都要加小括号，只有一条布尔表达式则可用可不用。合法的表达式是类似：a、!a、a<b、!(a<b)、!(a<b)||(!(a<b)&&!(a<b))，这里类似!!!a其实也是合法的，暂酱紫。
    private boolean isTrueBasic(HashMap<String, Instance> instances, String booleanExpression) {
        // 本来这里只保留< = >这3个就可以了，注意这里用了=而不是==，因为不允许布尔表达式中赋值的话就没必要两个等号表示等于了。不过考虑到所有主流的程序设计语言都是支持所有这些比较符的，尤其等于用=而不是==的话就是冒天下之大不韪了，故暂保留所有。
        // 这里像"<="要放在或者说排在"<"前面，不然就识别不到前者了。
        String[] comparisonOperators = {"<=", "<", "==", "!=", ">=", ">"};
        for (String comparisonOperator : comparisonOperators) {
            String[] elements = booleanExpression.split(comparisonOperator);
            if (elements.length == 2) {
                return compare(instances, elements, comparisonOperator);
            }
        }
        Instance instance = getInstance(instances, booleanExpression);
        if (null != instance) {
            if (Syntax.currentKeywords[0].equals(instance.type)) {
                return null != instance.getElements().get(null).type;
            } else {
                // 根据现有处理机制elements为空即变量为空
                return null != instance.getElements();
            }
        }
        return false;
    }

    private void setValue(Instance instance, Object value) {
        if (value instanceof Integer) {
            // 用函数调用而不是直接强制类型转换就不会报“Casting 'value' to 'int' is redundant”了。
            instance.getElements().get(null).type = getNumber(value);
        } else if (value instanceof String) {
            instance.getElements().get(null).type = getString(value);
        } else {
            instance.getElements().get(null).type = null;
        }
    }

    private Object getValue(Instance instance) {
        if (Syntax.currentKeywords[0].equals(instance.type)) {
            Object value = instance.getElements().get(null).type;
            return null != value ? value : 0;
        }
        return null;
    }

    private Object getValue(HashMap<String, Instance> instances, String instanceName) {
        String type = getType(instanceName);
        if (type.equals("instance")) {
            Instance instance = getInstance(instances, instanceName);
            return null != instance ? getValue(instance) : null;
        } else if (type.equals("string")) {
            return getString(instances, instanceName);
        } else {
            return getNumber(instances, instanceName);
        }
    }

    private LinkedList<String> getParts(String instanceName) {
        LinkedList<String> parts = new LinkedList<>();
        int nextStart = 0;
        while (nextStart < instanceName.length()) {
            nextStart = getNextStart(instanceName, nextStart);
            if (nextStart < instanceName.length()) {
                if ('?' == instanceName.charAt(nextStart)) {
                    parts.add(instanceName.substring(0, nextStart));
                    parts.add(instanceName.substring(nextStart + 1));
                    return parts;
                }
            }
        }
        parts.add(instanceName);
        return parts;
    }

    // 还有一个问题，似乎number、string类型变量与数字、字符串常量的映射表也要作局部变量处理
    private String getString(HashMap<String, Instance> instances, String instanceName) {
        // 双引号不设转义字符的话似乎无法正确处理一般字符串，故设计为字符串内不允许出现双引号之取字符串的真子集，其实也不影响功能，至少可以用单引号代替。
        // 这里是递归关系，不能instanceName.split("\\?")。
        LinkedList<String> parts = getParts(instanceName);
        String string;
        if ('"' == parts.get(0).charAt(0)) {
            string = parts.get(0).substring(1, parts.get(0).length() - 1);
        } else {
            Object value = getValue(instances, parts.get(0));
            string = null != value ? String.valueOf(value) : null;
        }
        if (1 == parts.size()) {
            return string;
        } else {
            if (null != string) {
                int index = getNumber(instances, parts.get(1));
                if (0 <= index && index < string.length()) {
                    return String.valueOf(string.charAt(index));
                } else {
                    // 字符串索引越界则返回""
                    return "";
                }
            }
            // 字符串为空则返回空。
            return null;
        }
    }

    private String getString(Object o) {
        return (String) o;
    }

    private int getNumber(Object o) {
        return (int) o;
    }

    // 统一之类似勾股变余弦是压缩优化代码的技巧甚至说基础
    private int getNumber(int a, int b, int c, int operationType) {
        int value;
        switch (operationType) {
            case 0:
                value = a + b * c;
                break;
            case 1:
                value = (int) (a * Math.pow(b, c));
                break;
            default:
                value = 0;
                break;
        }
        return value;
    }

    private char[][] operations = {{'+', '-'}, {'*', '/'}};

    // operationType决定是加减运算还是乘除运算，0表示加减，1表示乘除
    private Integer getNumber(HashMap<String, Instance> instances, String instanceName, int operationType) {
        int value = 0 == operationType ? 0 : 1;
//        boolean metOperation = false;
        // 上一个运算符是否+或*，1表示是，-1表示不是
        int addOrMul = 1;

        int start = 0;
        int nextStart = 0;
        while (nextStart < instanceName.length()) {
            nextStart = getNextStart(instanceName, nextStart);
            if (nextStart < instanceName.length()) {
                if (instanceName.charAt(nextStart) == operations[operationType][0] || instanceName.charAt(nextStart) == operations[operationType][1]) {
//                    metOperation = true;
                    value = getNumber(value, getNumber(instances, instanceName.substring(start, nextStart)), addOrMul, operationType);
                    addOrMul = instanceName.charAt(nextStart) == operations[operationType][0] ? 1 : -1;
                    start = nextStart + 1;
                }
            }
        }
        if (0 == start) {
            if (1 == operationType) {
                if ('(' == instanceName.charAt(0) && ')' == instanceName.charAt(instanceName.length() - 1)) {
                    return getNumber(instances, instanceName.substring(1, instanceName.length() - 1));
                } else {
                    Instance instance = getInstance(instances, instanceName);
                    if (null != instance) {
                        if (Syntax.currentKeywords[0].equals(instance.type)) {
                            return (Integer) getValue(instance);
                        }
                        Adapter.error("\"" + instanceName + "\" 不是一个 \"" + Syntax.currentKeywords[0] + "\" 的实例！");
                    }
                }
            }
            // start为0说明没有遇到运算符
            return null;
        } else {
            value = getNumber(value, getNumber(instances, instanceName.substring(start, nextStart)), addOrMul, operationType);
        }
        return value;
    }

    // 传入的字符串可以是类似：123、(a+1)*2/3。如果没有括号就是先对加减split再对乘除split，但有了的话就要先处理好来了。可以采用替换法，把类似(a+1)*2/3替换为b = a+1和b*2/3，这个b的命名似乎不大好处理，所以还是直接进入递归之遇到左括号就往后找到右括号把里面的算式送入递归。然后就是提取或者说识别出数字与变量，是数字就直接转成数字，是变量就查表得到值再转成数字。
    // 文法处理是一种典型的问题，因为要考虑很多情况，当然从高层级入手之少生多复合就可以轻松解决。要考虑可以获取那些元素，获取后该怎么用，比如a[2][2]这种就可以获取a、2、2,然后如何根据得到的数据达到目的。像算式解析算法这种，黑盒测试之无论数据集有多大都是不行的，必须从高层级之审视算法本身的正确性，这就是其数学本质或者说广义运算或者说广义计算本质。所谓KISS原则，并不是说就可以随便给出一些很蠢的方案，不然也就没必要研究算法了，只是说不要在一些无谓的细节上炫技。
    // 似信息检索很重要，我们面对一些问题，要尽可能从根源检索，一般是官方文档、API等，了解问题基本知识背景，然后才是问题编译说之考虑解决方案。
    // 只支持int是因为只关注离散量
    private Integer getNumber(HashMap<String, Instance> instances, String instanceName) {
        try {
            return Integer.parseInt(instanceName); // 数太大溢出会导致解析错误无限循环，这个提前报错即可，报错功能后续会添加
        } catch (NumberFormatException e) {
            Integer value = null;
            for (int i = 0; i < 2; i++) {
                // 如果判空返回放到循环后面，循环结束后就是直接返回null了，这样会到处报空指针警告。
                if (null != value) {
                    return value;
                }
                value = getNumber(instances, instanceName, i);
            }
            return value;
        }
    }
}