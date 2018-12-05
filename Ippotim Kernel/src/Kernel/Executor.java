package Kernel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

class Executor {
    boolean stop = false;
    boolean doBreak = false;
    private boolean doContinue = false;
    // 集合变量
    private HashMap<String, Instance> instances = new HashMap<>();

    // 变量结构
    private class Instance {
        String type;
        Object name;
        LinkedList<Instance> elements;
        // 对数组的实现是在此设立一个数组用以存放数组的各下标上限，使用具体的数组元素时先看数组名是否在变量表中，在的话再看各下标是否大于0且小于其上限，符合要求了再看类似a[1][2]之具体数组元素是否在arrayElements变量表里，不在就加进去再用，在就直接用
        HashMap<String, Instance> arrayElements;

        // 这样就是说常量也是变量之是变量的特例，type为null，name为其值。
        Instance() {}

        // 内部类中私有构造函数似乎对宿主类可见
        Instance(String type, String name) {
            this(type, name, true);
        }

        // 这里原来是Instance(String var0, String var1, boolean initElements)，想要在里面拿到matchedStructureOrFunction，传入一个TreeNode的话似乎不好处理，传入matchedStructureOrFunction的话似乎也不优雅，直接把initElements替换为matchedStructureOrFunction是一种很好的解决方案之改为Instance(String var0, String var1, List<TreeNode> elements)，不过这其实就是或者说都是TreeNode里的数据，所以直接Instance(TreeNode command)更好，然而果然还是要加上initElements防止死循环。不过getArrayInstance()里有非根据TreeNode新建Instance的需求，这样还是得细化参数。最终因为函数调用时新建Instance不好传入指向结构定义的链表，还是决定恢复成原来的设计。
        Instance(String type, String name, boolean initElements) {
            // type和name本来就是null，就不用开个else赋为null了，这是一种需要细心才能发现的优化。
            this.type = type;
            this.name = name;
            this.arrayElements = new HashMap<>();
            if ("void".equals(type)) {
                // 基本数据类型的值设计为其唯一的元素且也是变量，暂时假装不是元素而已，这样是方便判断是不是有传统意义上的结构体元素
                // 本来是结构体系定义有元素的则初始化elements，不过基本数据类型的值设计为其元素了，所以一起初始化
                this.elements = new LinkedList<>();
                this.elements.add(new Instance());
            } else {
                if (initElements) {
                    // 本来是结构体系定义有元素的则初始化elements，不过基本数据类型的值设计为其元素了，所以一起初始化
                    this.elements = new LinkedList<>();
                    LinkedList<TreeNode> elements = getDataList(Adapter.structures.get(type));
                    if (null != elements) {
                        for (TreeNode element : elements) {
                            // 如果结构体元素中有基本数据类型元素，则初始化
                            this.elements.add(new Instance(element.elements.get(0), element.elements.get(1), false));
                        }
                    }
                }
            }
        }
    }

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
            Instance instanceTemp = instance.arrayElements.get(nameOfArrayInstance);
            if (null == instanceTemp) {
                Instance newInstance = new Instance(instance.type, nameOfArrayInstance);
                instance.arrayElements.put(nameOfArrayInstance, newInstance);
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
            for (Instance element : fatherInstance.elements) {
                if (null != element.name) {
                    if (element.name.equals(instanceName)) {
                        return element;
                    }
                }
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
        // 对elementInstance.type判空是说常量不能作为变量使用，之现在的设计是常量保存在type为空的变量中。
        if (null == elementInstance || null == elementInstance.type) {
            return null;
        }
        return getArrayInstance(instances, elementInstance, elementName[2]);
        // 结构变量为null就是elements为空集
        // 现在设计成基本数据类型也有元素之其值就是其唯一的元素了，所以这里要判空也是从其结构体定义判，不过可能会导致性能进一步损失，直接删掉进入下面的统一流程更好
        // 给数据结构起复数名可以方便遍历时候临时变量命名
    }

    // 之前是方法重载实现局部变量功能，使用新方案后就不用了
    private Instance getInstance(HashMap<String, Instance> instances, String instanceName) {
        if (!getType(instanceName).equals("instance")) {
            return null;
        }
        Instance instance = null;
        // 一般的形式是类似a[a[a+a].a[a+a]].a[a[a+a].a[a+a]]，这样是不能简单split的，需要像括号匹配那样进行整体分离。
        LinkedList<String> elementChains = getChains(instanceName, true);
        for (String elementChain : elementChains) {
            instance = getElementInstance(instances, instance, elementChain);
            if (null == instance) {
                return null;
            }
        }
        return instance;
    }

    void run(LinkedList<TreeNode> commands) {
        run(instances, commands);
    }

    private void run(HashMap<String, Instance> instances, LinkedList<TreeNode> commands) {
        if (null == commands) {
            return;
        }

        for (TreeNode command : commands) {
            if (stop || doBreak || doContinue) {
                return;
            }
//            System.out.println(command.commandType.toString() + command.elements);
            switch (command.commandType) {
                case DEFINE: {
                    // 这样就要求变量名不能与结构名有相同的，否则会导致数组越界异常，比如S S之定义结构S的一个变量S，然后S之调用结构S的一个函数，就会误认为这个S是结构名而非变量名，从而导致下面的parameters[1]数组越界
                    // 处理类似Set set之类的变量定义，暂时设计为定义集合变量的时候即分配存储空间之Set set等价于Set set = new Set()
                    // 从1开始是因为0是结构名
                    for (int i = 1; i < command.elements.size(); i++) {
                        Instance instance = new Instance(command.elements.get(0), command.elements.get(i));
                        instances.put(command.elements.get(i), instance);
                    }
                    break;
                }
                case ASSIGN: {
                    assign(instances, command.elements.get(2), instances, command.elements.get(0));
                    break;
                }
                case INPUT: {
                    // 从1开始是因为0是“input”。
                    // input设计为赋值语句的手动模式，也即input后可接任意变量，用户输入的可以是任意表达式和或者说包括任意变量名。
                    for (int i = 1; i < command.elements.size(); i++) {
                        Instance instance = getInstance(instances, command.elements.get(i));
                        if (instance != null) {
                            setValue(instance, getValue(instances, Adapter.graphicsOperations.getInput()));
                        }
                    }
                    break;
                }
                case OUTPUT: {
                    // 前期先求把功能实现，越简单越好，不必强求强大精妙，过早优化是万恶之源，似己想要写操作系统也是想要一个可以扩展成足够强大的操作系统的教科书级的简易实现。
                    // output设计为只是输出常量和void变量的语句，这样似乎就与input不是一路人或者说互为相反数或者说阴阳了。
                    StringBuilder stringBuilder = new StringBuilder();
                    if (command.elements.size() > 1) {
                        // 从1开始是因为0是“output”了，像这种以及case不加break等不合常规的行为都应添加注释或者说说明
                        for (int i = 1; i < command.elements.size(); i++) {
                            String element = command.elements.get(i);
                            Object value = getValue(instances, element);
                            if (null != value) {
                                stringBuilder.append(value);
                            }
                        }
                    } else {
                        // 当output单独成句之不接任何参数，就是输出换行符，这个设计简直巧夺天工。
                        stringBuilder.append("\n");
                    }
                    Adapter.graphicsOperations.appendText(stringBuilder.toString());
                    break;
                }
                case IF: {
                    // 不管有没有else之子句最后一句是"else :"还是"+"，都是去头去尾
                    LinkedList<TreeNode> listIf = getDataList(command.list);
                    // 如果没有else，得到的就是null
                    LinkedList<TreeNode> listElse = getDataList(command.list.treeNodes.get(command.list.treeNodes.size() - 1).list);
                    HashMap<String, Instance> subInstances = new HashMap<>(instances);

                    if (isTrue(instances, command.elements.get(1))) {
                        run(subInstances, listIf);
                    } else {
                        run(subInstances, listElse);
                    }
                    break;
                }
                case WHILE: {
                    LinkedList<TreeNode> listWhile = getDataList(command.list);
                    HashMap<String, Instance> subInstances = new HashMap<>(instances);
                    while (isTrue(instances, command.elements.get(1))) {
                        run(subInstances, listWhile);
                    }
                    break;
                }
                case BREAK: { // while语句之外也可以使用break，比如主函数中，相当于后面的直接不执行了，暂不设限制
                    doBreak = true;
                    break;
                }
                case CONTINUE: { // 有循环似乎就得有break和continue
                    doContinue = true;
                    break;
                }
                case FUNCTION_CALL: {
                    run(genFunctionInstances(instances, command), getDataList(command.matchedFunction));
                }
            }
        }
        // 清空变量表，这个本来主要是为的清空while语句新建的变量表，认为这样就简单也巧妙地实现匿名变量了，不过后来发现根据原来新建同名变量会覆盖原有变量的机制也不影响匿名变量实现，本来又以为反正没用了清空也无妨，但while语句的变量表事先用主句的变量表中的元素初始化了，所以不应清空。
        // instances.clear();
    }

    private void assign(Instance from, Instance to, boolean treatAsGeneral) {
        if (treatAsGeneral) {
            to.elements = from.elements;
        } else {
            setValue(to, getValue(from));
        }
    }

    private void assign(HashMap<String, Instance> instancesFrom, String stringFrom, Instance to) {
        setValue(to, getValue(instancesFrom, stringFrom));
    }

    private void assign(HashMap<String, Instance> instancesFrom, String stringFrom, HashMap<String, Instance> instancesTo, String stringTo) {
        Instance from;
        Instance to = getInstance(instancesTo, stringTo);
        if (null != to) {
            from = getInstance(instancesFrom, stringFrom);
            if (null != from) {
                boolean treatAsGeneral = !to.type.equals("void");
                // 当instancesFrom与instancesTo不同，说明是函数调用时的赋值操作。
                if (!instancesFrom.equals(instancesTo)) {
                    treatAsGeneral = true;
                    to.arrayElements = from.arrayElements;
                }
                assign(from, to, treatAsGeneral);
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

    private HashMap<String, Instance> genFunctionInstances(HashMap<String, Instance> instances, TreeNode command) {
        HashMap<String, Instance> functionInstances = new HashMap<>();
        String s = command.elements.getLast();
        if (!s.equals("")) {
            String[] formalParameters = s.split(", ");
            for (int i = 0; i < formalParameters.length; i++) {
                String[] typeAndName = formalParameters[i].split(" ");
                Instance instanceFormal = new Instance(typeAndName[0], typeAndName[1]);
                functionInstances.put(typeAndName[1], instanceFormal);
                // 第0个是函数名，第1个开始是实参名
                assign(instances, command.elements.get(i + 1), functionInstances, typeAndName[1]);
            }
        }
        return functionInstances;
    }

    // 去掉首尾结点，即""与"+"与"else :"结点
    static LinkedList<TreeNode> getDataList(List list) {
        return null != list ? new LinkedList<>(list.treeNodes.subList(1, list.treeNodes.size() - 1)) : null;
    }

    private boolean isTheSame(HashMap<String, Instance> instances, String[] parameters) {
        Instance[] instance = new Instance[2];
        instance[0] = getInstance(instances, parameters[0]);
        instance[1] = getInstance(instances, parameters[1]);
        if (null != instance[0] && null != instance[1]) {
            if (!instance[0].type.equals("void") && !instance[1].type.equals("void")) {
                return Objects.equals(instance[0].elements, instance[1].elements);
            } else {
                return Objects.equals(getValue(instance[0]), getValue(instance[1]));
            }
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
        if (doBreak) {
            doBreak = false;
            return false;
        }
        if (doContinue) {
            doContinue = false; // 似乎不用判断直接置为false即可或者说也是一样的
        }
        if (booleanExpression.startsWith("!")) {
            return !isTrue(instances, booleanExpression.substring(1));
        }

        boolean result = true;
        int preOperation = 0; // 0是“&&”，1是“||”
        boolean metOperation = false;

        int start = 0;
        int nextStart = 0;
        while (nextStart < booleanExpression.length()) {
            nextStart = getNextStart(booleanExpression, nextStart);
            // 判断所在位置是否某字符是charAt(i)，是否某多字符除了可以判断charAt(i)、charAt(i + 1)、charAt(i + 2)...还可以类似substring(i).startsWith("&&")。
            if (nextStart < booleanExpression.length()) {
                String s = booleanExpression.substring(nextStart);
                if (s.startsWith("&&") || s.startsWith("||")) {
                    metOperation = true;
                    result = isTrue(instances, booleanExpression, start, nextStart, preOperation, result);
                    preOperation = s.startsWith("&&") ? 0 : 1;
                    nextStart += 2; // 暂时写死为2
                    start = nextStart;
                }
            } else if (0 == start) {
                if ('(' == booleanExpression.charAt(0)) {
                    String subExpression = booleanExpression.substring(1, booleanExpression.length() - 1);
                    return isTrue(instances, subExpression);
                }
            } else {
                result = isTrue(instances, booleanExpression, start, nextStart, preOperation, result);
            }
        }
        if (!metOperation) {
            return isTrueBasic(instances, booleanExpression);
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
        // 根据现有处理机制elements为空即变量为空
        return instance != null && instance.elements != null;
    }

    private void setValue(Instance instance, Object value) {
        if (value instanceof Integer) {
            // 用函数调用而不是直接强制类型转换就不会报“Casting 'value' to 'int' is redundant”了。
            instance.elements.getFirst().name = getNumber(value);
        } else if (value instanceof String){
            instance.elements.getFirst().name = getString(value);
        } else {
            instance.elements.getFirst().name = null;
        }
    }

    private Object getValue(Instance instance) {
        return null != instance ? instance.elements.getFirst().name : null;
    }

    private Object getValue(HashMap<String, Instance> instances, String instanceName) {
        String type = getType(instanceName);
        if (type.equals("instance")) {
            return getValue(getInstance(instances, instanceName));
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
            string = String.valueOf(getValue(instances, parts.get(0)));
        }
        if (1 == parts.size()) {
            return string;
        } else {
            if (null != string) {
                int index = getNumber(instances, parts.get(1));
                if (0 <= index && index < string.length()) {
                    return String.valueOf(string.charAt(index));
                }
            }
            // 字符串索引越界则返回空，这跟普通或者说一般的数组越界处理是统一的。这里跟string为null的情况合在一起写了，string为null就是没初始化。
            return "";
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
            } else if (0 == start) {
                if (1 == operationType && '(' == instanceName.charAt(0)) {
                    return getNumber(instances, instanceName.substring(1, instanceName.length() - 1));
                }
                // start为0说明没有遇到运算符
                return null;
            } else {
                // 调用本函数之前先整体判断了下，如果整个字符串是一个变量名，就不会进来这里了，所以走到这一步说明之前是遇到了操作符的。
                value = getNumber(value, getNumber(instances, instanceName.substring(start, nextStart)), addOrMul, operationType);
            }
        }
//        if (!metOperation) {
//            if (1 == operationType) {
//                return (Integer) getValue(instances, instanceName, "number");
//            }
//            return null;
//        }
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
            Instance instance = getInstance(instances, instanceName);
            if (null != instance) {
                value = (Integer) getValue(instance);
                return null != value ? value : 0;
            }
            for (int i = 0; i < 2; i++) {
                if (null != value) {
                    return value;
                }
                value = getNumber(instances, instanceName, i);
            }
            return value;
        }
    }

    static LinkedList<String> getRegularElements(String s) {
        String separator = " ";
        LinkedList<String> regularElements = new LinkedList<>();
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

    // 函数调用如果还是由变量类型序列确定的话，似乎得运行时才能够做到了。其实不需要，至少对于现在的设计而言，通过处理之前的定义语句即可。这里要注意的是：getCommandType()应当是面向一个TreeNode组的，毕竟main函数也是函数，而函数被设计成比较完美的沙盒机制。至于说使用传统的函数名设计，其实也要校验参数类型，所以直接砍掉函数名也是合理的。这个首先需要找到执行到函数调用语句前执行到的所有语句，可以倒推回去，先找到所在组的首结点，然后找到所在子句的主句，然后再找到主句所在的组的首结点，得到所有的定义语句；难在根据子句找主句。可以遍历vectorLine找到主句的矩形编号，然后遍历vectorRectangle找到组号。这样是可行的，但效率太低了，不如修改TreeNode定义，添加一个指向主句的指针，空间复杂度换时间复杂度。即便如此，对于每一个函数调用语句都回溯似乎也是不可忍受的，只能深度优先遍历之对于函数集合中的每一个函数进行深度优先遍历，这样指向前驱结点的指针preTreeNode也可以不要了。
    // 之所以要添加预编译就是想要去掉频繁查表的开销，然而还有1张表没有去掉，这就是变量表。变量表似乎不能通过预编译去掉，像C语言对于变量的处理或者说实现应该是借助各种寻址方式，其实变量都是由定义语句生成的，如此说来变量确实可以在预编译时
    static CommandType getCommandType(TreeNode command) {
        String fistElement = command.elements.get(0);
        if (Adapter.structures.containsKey(fistElement)) {
            // 结构定义还是通过结构名查表得，不然函数调用的时候新建Instance不好传入指向结构定义的链表
            // command.matchedStructureOrFunction = setStructures.get(command.elements.get(0));
            return CommandType.DEFINE; // 定义语句需要查结构表，实际可以像函数调用那样处理之直接获取结构所在组号这样就不用查了，令结构所在组号为g，则预编译号可以设计成(-8-g)。不过只是得到组号的话还是要根据组号查表才能得到数据，这与根据结构名查表似乎没什么两样。除非将结构所在的组置为定义语句的子句，这样就还需要设置一个标志位表示该子句是临时的，第二次运行时执行本函数会检查该标志位，子句是临时的则先将子句置为null。这样也不用保存组号了，函数调用也应这样处理。其实这样也不好，最好是新增一个链表元素。由于不用保存组号了，这样就可以用enum保存语句类型了。
        }
        if (Adapter.functions.containsKey(fistElement)) {
            command.elements.add(Adapter.functionNameToParameters.get(fistElement));
            command.matchedFunction = Adapter.functions.get(fistElement);
            return CommandType.FUNCTION_CALL;
        }
        if (3 == command.elements.size() && command.elements.get(1).equals("=")) {
            return CommandType.ASSIGN;
        }
        switch (fistElement) {
            case "input":
                return CommandType.INPUT;
            case "output":
                return CommandType.OUTPUT;
            case "if":
                return CommandType.IF;
            case "while":
                return CommandType.WHILE;
            case "break":
                return CommandType.BREAK;
            case "continue":
                return CommandType.CONTINUE;
            default:
                return CommandType.UNKNOWN;
        }
    }
}