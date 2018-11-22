package Kernel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

public class Adapter {
    private LinkedList<TreeNode> selectedList;
    private int x, y;
    static double scale;
    private TreeNode selectedTreeNode;
    private Vector<Line> vectorLine = new Vector<>();
    private GraphicsOperation graphicsOperation; // 本来是static GraphicsOperation drawTable = new DrawTable();之实际用不到所有的DrawTable的方法与属性，通过接口或抽象类可以完美解耦合，这就是函数指针、接口、抽象类之类的真正奥义
    private Executor executor;

    private HashMap<String, LinkedList<TreeNode>> structures = new HashMap<>();
    private HashMap<String, String> functionNameToParameters = new HashMap<>();
    private HashMap<String, LinkedList<TreeNode>> functions = new HashMap<>();
    private LinkedList<LinkedList<TreeNode>> lists = new LinkedList<>();
    // 哈希表是建立映射的一种数据结构，可以用来给元素添加属性，而不需要新建一个类

    // 0是结构定义用色，1是main函数用色，2是函数定义用色
    private Color[] colors = {new Color(Color.RED, Color.YELLOW), new Color(Color.WHITE, Color.BLACK), new Color(Color.YELLOW, Color.RED)};

    public Adapter(GraphicsOperation graphicsOperation) {
        this.graphicsOperation = graphicsOperation;
    }

    public void setXY(int v0, int v1) {
        x = v0;
        y = v1;
    }

    // 因为是直接调用静态方法，所以使用init()实现构造函数的作用
    public void init(int x, int y, double s) {
        clearAll();

        scale = 1 / s;

        structures.put("string", null);
        structures.put("number", null);

        // main函数也并入函数集合
        Rectangle.currentGroupColor = colors[1];
        LinkedList<TreeNode> commands = new LinkedList<>();

        Rectangle rectangle = new Rectangle("", x - Rectangle.getWidth() / 2, y - Rectangle.getHeight());
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        commands.add(treeNode);

        rectangle = new Rectangle("+", x - Rectangle.getWidth() / 2, y);
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        commands.add(treeNode);

        registerTreeNodeList(commands);
        registerFunction(commands);
    }

    public void run() {
        executor = new Executor(graphicsOperation, structures, functionNameToParameters, functions);
        for (LinkedList<TreeNode> list : lists) {
            for (TreeNode treeNode : Executor.getDataList(list)) {
                treeNode.updateElements(executor);
            }
        }

        executor.run(Executor.getDataList(functions.get("")));
        // debug就是研究非预期行为的成因，看代码推敲是不够的，还需要有显示中间数据的手段
    }

    public void stop() {
        executor.doBreak = true;
        executor.stop = true;
    }

    private TreeNode importTreeNode(Node node, int x, int y) {
        NodeList childNodes = node.getChildNodes();

        Node contentNode = childNodes.item(0);
        String content = contentNode.hasChildNodes() ? contentNode.getFirstChild().getNodeValue() : "";
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = new Rectangle(content, x, y);

        Node subTreeNodes = childNodes.item(1);
        // 这里原来有个else，即没有子结点就将subTreeNodes置为null，然而这个初始化就是null，所以直接去掉，这是一种典型的优化。
        if (subTreeNodes.hasChildNodes()) {
            treeNode.subTreeNodes = new LinkedList<>();
            registerTreeNodeList(treeNode.subTreeNodes);
            importTreeNodes(subTreeNodes, treeNode.subTreeNodes); // 之前subGroupNumber是maxGroupNumber + 1，这样在循环的时候maxGroupNumber就会不断增加从而导致逻辑错误，这个要引以为鉴。
            vectorLine.add(new Line(treeNode.rectangle, treeNode.subTreeNodes.getFirst().rectangle));
        }
        return treeNode;
    }

    private void importTreeNodes(Node node, LinkedList<TreeNode> list) {
        int x, y;
        x = Integer.parseInt(node.getChildNodes().item(0).getFirstChild().getNodeValue());
        y = Integer.parseInt(node.getChildNodes().item(1).getFirstChild().getNodeValue());
        for (int i = 2; i < node.getChildNodes().getLength(); i++) { // 从2开始是因为0和1已经用来给x和y赋值了
            list.add(importTreeNode(node.getChildNodes().item(i), x, y));
            y += Rectangle.getHeight();
        }
    }

    private void exportLocation(Document document, Element element, Rectangle rectangle) {
        Element xElement = document.createElement("X");
        xElement.appendChild(document.createTextNode(String.valueOf(rectangle.x)));
        element.appendChild(xElement);
        Element yElement = document.createElement("Y");
        yElement.appendChild(document.createTextNode(String.valueOf(rectangle.y)));
        element.appendChild(yElement);
    }

    private void exportTreeNode(Document document, Element element, TreeNode treeNode) {
        Element treeNodeElement = document.createElement("TreeNode");

        Element contentElement = document.createElement("Content");
        contentElement.appendChild(document.createTextNode(treeNode.rectangle.content));
        treeNodeElement.appendChild(contentElement);

        Element subTreeNodesElement = document.createElement("SubTreeNodes");
        treeNodeElement.appendChild(subTreeNodesElement);
        if (treeNode.subTreeNodes != null) {
            exportTreeNodes(document, subTreeNodesElement, treeNode.subTreeNodes);
        }
        element.appendChild(treeNodeElement);
    }

    private void exportTreeNodes(Document document, Element element, LinkedList<TreeNode> list) {
        exportLocation(document, element, list.getFirst().rectangle);  // 有了批量导出的函数后exportLocation()也才能像这样统一被动调用而不用写得导出都是
        for (TreeNode treeNode : list) {
            exportTreeNode(document, element, treeNode);
        }
    }

    private void clearAll() {
        vectorLine.clear();
        structures.clear();
        functionNameToParameters.clear();
        functions.clear();
        lists.clear();
        selectedTreeNode = null;
    }

    private void unregisterTreeNodeList(LinkedList<TreeNode> list) {
        lists.remove(list);
        list.clear();
    }

    private void registerTreeNodeList(LinkedList<TreeNode> list) {
        lists.add(list);
    }

    private String getListHead(LinkedList<TreeNode> list) {
        return list.getFirst().rectangle.content;
    }

    private void registerStructure(LinkedList<TreeNode> list) {
        structures.put(getListHead(list), list);
    }

    private void unregisterStructure(LinkedList<TreeNode> list) {
        structures.remove(getListHead(list));
    }

    private void registerFunction(LinkedList<TreeNode> list) {
        String[] strings = getListHead(list).split(" ", 3);
        functions.put(strings[0], list);
        functionNameToParameters.put(strings[0], 3 == strings.length ? strings[2] : "");
    }

    private void unregisterFunction(LinkedList<TreeNode> list) {
        String[] strings = getListHead(list).split(" ", 3);
        functions.remove(strings[0]);
        functionNameToParameters.remove(strings[0]);
    }

    // listType: 0是结构定义，1是函数定义
    private void genTreeNodeList(Node node, boolean structureOrFunction) {
        LinkedList<TreeNode> list = new LinkedList<>();
        importTreeNodes(node, list);
        registerTreeNodeList(list);
        if (structureOrFunction) {
            registerStructure(list);
        } else {
            registerFunction(list);
        }
    }

    // 主要是用于导入代码后重新设置main函数矩形颜色
    private void setListColor(LinkedList<TreeNode> list, Color color) {
        for (TreeNode treeNode : list) {
            treeNode.rectangle.color = color;
            if (null != treeNode.subTreeNodes) {
                setListColor(treeNode.subTreeNodes, color);
            }
        }
    }

    public boolean getCodeFromXml(File file) {
        // 先清空现有表项
        clearAll();

        // 然后从XML读取
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            Node root = document.getDocumentElement();
            NodeList childNodes = root.getChildNodes();

            Node sizeNode = childNodes.item(0);
            scale = Double.parseDouble(sizeNode.getFirstChild().getNodeValue());

            Node structuresNode = childNodes.item(1);
            Rectangle.currentGroupColor = colors[0];
            for (int i = 0; i < structuresNode.getChildNodes().getLength(); i++) {
                genTreeNodeList(structuresNode.getChildNodes().item(i), true);
            }
            structures.put("string", null);
            structures.put("number", null);

            Node functionsNode = childNodes.item(2);
            Rectangle.currentGroupColor = colors[2];
            for (int i = 0; i < functionsNode.getChildNodes().getLength(); i++) {
                genTreeNodeList(functionsNode.getChildNodes().item(i), false);
            }
            // 设置main函数矩形颜色
            LinkedList<TreeNode> main = functions.get("");
            setListColor(main, colors[1]);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            return false;
        }

        return true;
    }

    // 程序相关的数据就是主函数链表、各个结构定义链表组成的哈希表、各个函数定义组成的哈希表，至于子句的子链表则可以用来还原连线信息所以不需要保存连线信息
    public boolean setCodeToXml(File file) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element root = document.createElement("Code");

            Element size = document.createElement("Size");
            size.appendChild(document.createTextNode(String.valueOf(scale)));
            root.appendChild(size);

            Element structuresNodes = document.createElement("Structures");
            for (LinkedList<TreeNode> list : structures.values()) {
                if (list != null) { // string和number的list是null
                    Element structure = document.createElement("Structure");
                    exportTreeNodes(document, structure, list);
                    structuresNodes.appendChild(structure);
                }
            }
            root.appendChild(structuresNodes);

            Element functionsNodes = document.createElement("Functions");
            for (LinkedList<TreeNode> list : functions.values()) {
                Element function = document.createElement("Function");
                exportTreeNodes(document, function, list);
                functionsNodes.appendChild(function);
            }
            root.appendChild(functionsNodes);

            document.appendChild(root);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            // transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 不能添加换行否则会解析失败
            // 原来是new StreamResult(file)，这样当文件名中有中文，安卓下会变成乱码，Linux下倒是正常，可能FileOutputStream专治各种乱码
            transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));
            return true;
        } catch (ParserConfigurationException | TransformerException | FileNotFoundException e) {
            return false;
        }
    }

    public void createFunction(String s) {
        if (s.equals("")) {
            return;
        }
        Rectangle.currentGroupColor = colors[2];
        LinkedList<TreeNode> linkedList = new LinkedList<>();
        Rectangle rectangle = new Rectangle(s, x, y);
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        linkedList.add(treeNode);

        rectangle = new Rectangle("+", x, y + Rectangle.getHeight());
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        linkedList.add(treeNode);

        registerTreeNodeList(linkedList);
        registerFunction(linkedList);
    }

    public void createStructure(String s) {
        if (s.equals("")) {
            return;
        }
        Rectangle.currentGroupColor = colors[0];
        LinkedList<TreeNode> linkedList = new LinkedList<>();
        Rectangle rectangle = new Rectangle(s, x, y);
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        linkedList.add(treeNode);

        rectangle = new Rectangle("+", x, y + Rectangle.getHeight());
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        linkedList.add(treeNode);

        registerTreeNodeList(linkedList);
        registerStructure(linkedList);
    }

    private TreeNode createMember(String s) {
        Rectangle.currentGroupColor = selectedTreeNode.rectangle.color;
        TreeNode treeNode = new TreeNode();

        Rectangle rectangle = new Rectangle(s, selectedTreeNode.rectangle.x, selectedTreeNode.rectangle.y);
        treeNode.rectangle = rectangle;

        if (s.startsWith("if ") || s.equals("else") || s.startsWith("while ")) {
            LinkedList<TreeNode> linkedList = new LinkedList<>();
            treeNode.subTreeNodes = linkedList;
            rectangle = new Rectangle("", rectangle.x + 2 * Rectangle.getWidth(), rectangle.y + Rectangle.getHeight());
            TreeNode subTreeNode = new TreeNode();
            subTreeNode.rectangle = rectangle;
            treeNode.subTreeNodes.add(subTreeNode);

            vectorLine.add(new Line(treeNode.rectangle, rectangle));

            rectangle = new Rectangle("+", rectangle.x, rectangle.y + Rectangle.getHeight());
            subTreeNode = new TreeNode();
            subTreeNode.rectangle = rectangle;
            treeNode.subTreeNodes.add(subTreeNode);
            registerTreeNodeList(linkedList);
        }

        return treeNode;
    }

    private boolean canCreateElse(LinkedList<TreeNode> list) {
        for (TreeNode treeNode : Executor.getDataList(list)) {
            if (treeNode.rectangle.content.startsWith("if ") && treeNode.subTreeNodes.equals(selectedList)) {
                return true;
            }
            if (null != treeNode.subTreeNodes) {
                if (canCreateElse(treeNode.subTreeNodes)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 先像点带加号的矩形添加新矩形那样新建一个矩形，然后做一次轮换把新建矩形换到指定位置，包括矩形链表和TreeNode链表。要在第一个矩形上面添加矩形，只能把第一个矩形设为头节点，比如指令链表的头节点为“main”，结构定义和集合运算定义的头节点自然就分别是结构名和参数列表，最后处理时略过即可
    public void insert(String s) {
        if (selectedTreeNode.rectangle.content.equals("else")) {
            selectedTreeNode = null;
        }
        if (s.equals("else")) {
            boolean canCreateElse = false;
            // 只有在if语句的子句里才能新建else语句
            for (LinkedList<TreeNode> list : functions.values())  {
                if (canCreateElse(list)) {
                    canCreateElse = true;
                    break;
                }
            }
            // else语句只能点击“+”矩形新建，不能由插入、修改来
            canCreateElse = canCreateElse && doCreateMember;
            if (!canCreateElse) {
                selectedTreeNode = null;
            }
        }

        if (doCreateMember) {
            doCreateMember = false;
        }

            // 将这个判断从下一个判断中提取出来是较好的选择，否则下一个判断是if (selectedRectangle == null || selectedRectangle.content.startsWith("else ") || s.equals(""))，大括号里面还要再进行一次if (s.equals(""))判断，这就有了重复代码，不优雅
        if (s.equals("")) {
            selectedTreeNode = null;
        }
        if (selectedTreeNode == null) { // 想要通知图形界面没选中矩形，除了可以在GraphicsOperation新增一个发送信息的方法，还可以把insert()的返回值设为int，返回约定好的比如-1表示未选中矩形，这里没有判断tempRectangle.content.startsWith("else ")也无妨
            return;
        }

        TreeNode member = createMember(s);
        boolean upForMove = false;
        for (int i = 0; i < selectedList.size(); i++) { // 这里原来是for (int i = 0; i < selectedList.size() - 1; i++)，这是一处耐人寻味的误解
            if (upForMove) {
                selectedList.get(i).rectangle.y += Rectangle.getHeight();
            }
            if (selectedList.get(i).equals(selectedTreeNode)) {
                selectedList.add(i + 1, member);
                // 似乎遍历的时候selectedList.size()不会改变
                upForMove = true;
            }
        }

        selectedTreeNode = member;
        // 新增else语句后删除"+"结点
        if (s.equals("else")) {
            selectedList.removeLast();
        }
    }

    private void unregisterLine(TreeNode treeNode) {
        for (Line line : vectorLine) {
            if (line.from.equals(treeNode.rectangle)) {
                vectorLine.remove(line);
                break;
            }
        }
    }

    private void unregisterTree(TreeNode treeNode) {
        if (treeNode.subTreeNodes != null) {
            unregisterLine(treeNode);
            unregisterTreeNodeList(treeNode.subTreeNodes);
            for (TreeNode t : treeNode.subTreeNodes) {
                unregisterTree(t);
            }
        }
    }

    // 选中矩形后菜单项选删除，先删除选中的矩形所属TreeNode及其子TreeNode之包括从矩形链表中删除TreeNode对应的矩形，然后把该组TreeNode后面的TreeNode的矩形的y值减去一个矩形高
    public void delete() {
        if (null == selectedList) {
            return;
        }
        boolean upForMoveDown = false;
        for (int i = 0; i < selectedList.size(); i++) { // for (TreeNode t : selectedList) 会java.util.ConcurrentModificationException
            TreeNode t = selectedList.get(i);
            if (upForMoveDown) {
                t.rectangle.y -= Rectangle.getHeight();
            }
            if (t.equals(selectedTreeNode)) {
                // 特殊结点删除作特殊处理
                if (t.rectangle.content.equals("")) { // 子句的第一个矩形不允许删除
                    break;
                } else if (t.equals(selectedList.getFirst())) { // 全部删除，包括结构定义、函数定义
                    if (t.rectangle.color.rectangleColor == Color.RED) { // 说明是结构定义
                        unregisterStructure(selectedList);
                    } else if (t.rectangle.color.rectangleColor == Color.YELLOW) { // 说明是函数定义
                        unregisterFunction(selectedList);
                    }
                    // 要么是结构定义要么是函数定义，删不掉的是main函数和子句，在上一个判断中已经返回了
                    unregisterTreeNodeList(selectedList);
                } else {
                    upForMoveDown = true;
                    unregisterTree(t);
                    selectedList.remove(t);
                    // 因为新建else结点的时候把"+"结点删了，所以删else结点的时候加回去
                    if (t.rectangle.content.equals("else")) {
                        Rectangle.currentGroupColor = t.rectangle.color; // 这句很必要
                        Rectangle rectangle = new Rectangle("+", t.rectangle.x, t.rectangle.y + Rectangle.getHeight());
                        TreeNode treeNode = new TreeNode();
                        treeNode.rectangle = rectangle;
                        selectedList.add(treeNode);
                    }
                    // 遍历时删除节点导致链表结构变化，需要作调整
                    selectedList.get(i).rectangle.y -= Rectangle.getHeight();
                }
            }
        }
        selectedTreeNode = null;
    }

    public boolean hasSelectedTreeNode() {
        return null != selectedTreeNode;
    }

    public String getRectangleContent() {
        return null != selectedTreeNode ? selectedTreeNode.rectangle.content : "";
    }

    // 选中矩形后长按即弹出输入窗口让更新内容，如果是改了像if、else、while这样有子句者则删除子句，或者为了防止误触还是设置一个modify菜单项。本来是可以直接改的，不过改变等价于删除加添加，只是这样删除第一个矩形的时候就难了，可以在TreeNode链表再添加一个不关联矩形的头结点，只是第一个矩形其实没有删除的必要
    public void modify(String s) {
        // 不允许修改main函数和子句的头结点，不允许修改为else语句或“”
        if (null == selectedList || selectedTreeNode.rectangle.content.equals("") || s.equals("else") || s.equals("")) {
            selectedTreeNode = null;
            return;
        }
        LinkedList<TreeNode> linkedList = selectedList;
        TreeNode preTreeNode = linkedList.getFirst();
        TreeNode savedTreeNode = null;
        for (TreeNode t : linkedList) {
            if (t.equals(selectedTreeNode)) {
                savedTreeNode = t;
                // 特殊结点删除作特殊处理
                if (t.rectangle.content.equals("")) { // 子句的第一个矩形不允许修改
                    selectedTreeNode = null;
                    return;
                } else if (t.equals(linkedList.getFirst())) {
                    if (t.rectangle.color.rectangleColor == Color.RED) { // 说明是结构定义
                        unregisterStructure(selectedList);
                        t.rectangle.content = s;
                        registerStructure(selectedList);
                    } else if (t.rectangle.color.rectangleColor == Color.YELLOW) { // 说明是函数定义
                        unregisterFunction(selectedList);
                        t.rectangle.content = s;
                        registerFunction(selectedList);
                    }
                    return; // 这个return很必要，一段时间不看都不记得当初是怎么想的怎么写上去的了
                }
                boolean justReturn = false;

                boolean[] hasSubTreeNodes = new boolean[2];
                hasSubTreeNodes[0] = s.startsWith("if ") || s.equals("else") || s.startsWith("while ");
                // 这时候t就是selectedTreeNode
                String content = t.rectangle.content;
                hasSubTreeNodes[1] = content.startsWith("if ") || content.equals("else") || content.startsWith("while ");
                // 都没有子句或都有子句则直接返回，这是最简单的处理，不要想得太复杂
                if (hasSubTreeNodes[0] == hasSubTreeNodes[1]) {
                    justReturn = true;
                }
                // 之前是放在上面的if前面，这样tempRectangle.content就被覆盖了，导致普通指令修改为分支语句后会在doRepaint()时空指针
                t.rectangle.content = s;
                if (justReturn) {
                    return;
                }
                break;
            }
            preTreeNode = t;
        }
        delete();
        selectedTreeNode = preTreeNode;
        insert(s);
        selectedTreeNode = savedTreeNode;
    }

    private boolean doCreateMember;

    public void click() {
        // selectedGroup和selectedRect应该可以和tempRectangle合并
        selectedList = null;
        selectedTreeNode = null;
        TreeNode preTreeNode = null;
        TreeNode currentTreeNode;
        // 从后往前遍历
        ListIterator<LinkedList<TreeNode>> iterator = lists.listIterator(lists.size());
        while (iterator.hasPrevious()) {
            LinkedList<TreeNode> list = iterator.previous();
            currentTreeNode = list.getFirst();
            // 判断点击点的y是否在该组的范围内，略微加速查找。由于矩形的长度是由其内文字决定的，遍历开销似乎也能接受，日后有更好的方案再优化。
            if (!(currentTreeNode.rectangle.y <= y && y <= currentTreeNode.rectangle.y + list.size() * Rectangle.getHeight())) {
                continue;
            }
            for (TreeNode treeNode : list) {
                currentTreeNode = treeNode;
                int textWidth = graphicsOperation.getTextLength(currentTreeNode.rectangle.content, Rectangle.getFontSize());
                int width = textWidth < Rectangle.getWidth() ? Rectangle.getWidth() : textWidth;
                if (currentTreeNode.rectangle.x <= x && x <= currentTreeNode.rectangle.x + width && currentTreeNode.rectangle.y <= y && y <= currentTreeNode.rectangle.y + Rectangle.getHeight()) {
                    selectedTreeNode = currentTreeNode;
                    selectedList = list;
                    if (currentTreeNode.rectangle.content.equals("+")) {
                        doCreateMember = true;
                        selectedTreeNode = preTreeNode;
                        graphicsOperation.create("Member");
                    }
                    lists.remove(list);
                    lists.add(list);
                    return;
                }
                preTreeNode = currentTreeNode;
            }
        }
    }

    public void drag(int ex, int ey) {
        if (null == selectedList) {
            for (LinkedList<TreeNode> list : lists) {
                for (TreeNode treeNode : list) {
                    treeNode.rectangle.moveTo(ex - x, ey - y);
                }
            }
        } else {
            LinkedList<TreeNode> list = selectedList;
            for (TreeNode treeNode : list) {
                treeNode.rectangle.moveTo(ex - x, ey - y);
            }
        }
        x = ex;
        y = ey;
    }

    public void doWheelRotation(int x, int y, double s) {
        if (scale / s < 0.1 || scale / s > 10) { // 缩放倍率不超过基准倍率10倍
            return;
        }
        scale /= s;

        for (LinkedList<TreeNode> list : lists) {
            int baseX, baseY;
            baseX = (int) (x + (list.getFirst().rectangle.x - x) / s);
            baseY = (int) (y + (list.getFirst().rectangle.y - y) / s);

            for (TreeNode treeNode : list) {
                treeNode.rectangle.x = baseX;
                treeNode.rectangle.y = baseY;
                baseY += Rectangle.getHeight();
            }
        }
    }

    public void paintEverything() {
        for (LinkedList<TreeNode> list : lists) {
            for (TreeNode treeNode : list) {
                treeNode.rectangle.draw(graphicsOperation);
            }
        }

        for (Line line : vectorLine) {
            line.draw(graphicsOperation);
        }

        TreeNode focusedTreeNode = doCreateMember ? null : selectedTreeNode;

        if (focusedTreeNode != null) {
            Color color = selectedList.getFirst().rectangle.color;
            Color inverseColor = new Color(color.stringColor, color.rectangleColor); // 能想到用反色也是很了不起了
            focusedTreeNode.rectangle.color = inverseColor;
            focusedTreeNode.rectangle.draw(graphicsOperation);
            focusedTreeNode.rectangle.color = color;

            // 选中有子句的矩形后同时反色显示其子句的第一个矩形，便于查看
            if (focusedTreeNode.rectangle.content.startsWith("if ") || focusedTreeNode.rectangle.content.equals("else") || focusedTreeNode.rectangle.content.startsWith("while ")) {
                focusedTreeNode.subTreeNodes.getFirst().rectangle.color = inverseColor;
                focusedTreeNode.subTreeNodes.getFirst().rectangle.draw(graphicsOperation);
                focusedTreeNode.subTreeNodes.getFirst().rectangle.color = color;
            }

            // 选中函数调用语句所在的矩形后反色显示调用到的函数所在主句的第一个矩形，便于查看
            if (null != focusedTreeNode.matchedFunction && !focusedTreeNode.matchedFunction.isEmpty()) {
                TreeNode functionHead = focusedTreeNode.matchedFunction.getFirst();
                color = functionHead.rectangle.color;
                inverseColor = new Color(color.stringColor, color.rectangleColor);

                functionHead.rectangle.color = inverseColor;
                functionHead.rectangle.draw(graphicsOperation);
                functionHead.rectangle.color = color;
            }

            // 选中定义语句所在的矩形后反色显示结构定义所在主句的第一个矩形，便于查看
            LinkedList<TreeNode> structure = null != focusedTreeNode.elements ? structures.get(focusedTreeNode.elements.get(0)) : null;
            if (null != structure) { // 本来是CommandType.DEFINE == selectedTreeNode.commandType，不过现在这样也好
                TreeNode functionHead = structure.getFirst();
                color = functionHead.rectangle.color;
                inverseColor = new Color(color.stringColor, color.rectangleColor);

                functionHead.rectangle.color = inverseColor;
                functionHead.rectangle.draw(graphicsOperation);
                functionHead.rectangle.color = color;
            }
        }
    }
}
