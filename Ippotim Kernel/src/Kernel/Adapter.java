package Kernel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
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

public class Adapter {
    private int x, y;
    private int width, height;
    private List selectedList;
    private TreeNode selectedTreeNode;
    static GraphicsOperations graphicsOperations; // 本来是static GraphicsOperations drawTable = new DrawTable();之实际用不到所有的DrawTable的方法与属性，通过接口或抽象类可以完美解耦合，这就是函数指针、接口、抽象类之类的真正奥义
    private Executor executor;

    static HashMap<String, List> structures = new HashMap<>();
    static HashMap<String, String> functionNameToParameters = new HashMap<>();
    static HashMap<String, List> functions = new HashMap<>();
    private LinkedList<List> lists = new LinkedList<>();
    // 哈希表是建立映射的一种数据结构，可以用来给元素添加属性，而不需要新建一个类

    // 当前页名
    private String currentPageName = "";
    // 分页哈希表，用以像代码分文件那样给矩形组分页
    private HashMap<String, LinkedList<List>> pages = new HashMap<>();

    // 0是结构定义用色，1是main函数用色，2是函数定义用色
    private Color[] colors = {new Color(Color.RED, Color.YELLOW), new Color(Color.WHITE, Color.BLACK), new Color(Color.YELLOW, Color.RED)};

    private void setScale(double s) {
        Rectangle.scale = s;
        Rectangle.updateSize();
    }

    public void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setScreen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Adapter(GraphicsOperations g, int x, int y, double s) {
        graphicsOperations = g;
        setScreen(x, y);
        Rectangle.defaultScale = 1 / s;
        setScale(Rectangle.defaultScale);
        structures.put("void", null);
        addDefaultMainFunction();
    }

    private void addDefaultMainFunction() {
        // main函数也并入函数集合
        Rectangle.currentGroupColor = colors[1];
        List main = new List(width / 2 - Rectangle.width / 2, height / 2 - Rectangle.height);

        Rectangle rectangle = new Rectangle("");
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        main.treeNodes.add(treeNode);

        rectangle = new Rectangle("+");
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        main.treeNodes.add(treeNode);

        registerList(main);
        registerFunction(main);
        pages.put(currentPageName, lists);
    }

    private void clear(boolean addDefaultMainFunction) {
        setScale(Rectangle.defaultScale);
        pages.clear();
        lists.clear();
        structures.clear();
        structures.put("void", null);
        functionNameToParameters.clear();
        functions.clear();
        selectedTreeNode = null;
        if (addDefaultMainFunction) {
            addDefaultMainFunction();
        }
    }

    public void clear() {
        clear(true);
    }

    private void updateElements() {
        for (List list : lists) {
            for (TreeNode treeNode : Executor.getDataList(list)) {
                treeNode.updateElements();
            }
        }
    }

    public void run() {
        updateElements();
        try {
            executor = new Executor();
            executor.run(Executor.getDataList(functions.get("")));
            // debug就是研究非预期行为的成因，看代码推敲是不够的，还需要有显示中间数据的手段
        } catch (Exception e) {
            graphicsOperations.showMessage(e.toString());
            e.printStackTrace();
        }
    }

    public void stop() {
        executor.doBreak = true;
        executor.stop = true;
    }

    private TreeNode importTreeNode(Node node) {
        NodeList childNodes = node.getChildNodes();

        Node contentNode = childNodes.item(0);
        String content = contentNode.hasChildNodes() ? contentNode.getFirstChild().getNodeValue() : "";
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = new Rectangle(content);

        Node subTreeNodes = childNodes.item(1);
        // 这里原来有个else，即没有子结点就将subTreeNodes置为null，然而这个初始化就是null，所以直接去掉，这是一种典型的优化。
        if (subTreeNodes.hasChildNodes()) {
            treeNode.list = importList(subTreeNodes); // 之前subGroupNumber是maxGroupNumber + 1，这样在循环的时候maxGroupNumber就会不断增加从而导致逻辑错误，这个要引以为鉴。
        }
        return treeNode;
    }

    private List importList(Node node) {
        int x = Integer.parseInt(node.getChildNodes().item(0).getFirstChild().getNodeValue());
        int y = Integer.parseInt(node.getChildNodes().item(1).getFirstChild().getNodeValue());
        List list = new List(x, y);
        registerList(list);
        for (int i = 2; i < node.getChildNodes().getLength(); i++) { // 从2开始是因为0和1已经用来给x和y赋值了
            list.treeNodes.add(importTreeNode(node.getChildNodes().item(i)));
        }
        return list;
    }

    private void exportTreeNode(Document document, Element element, TreeNode treeNode) {
        Element treeNodeElement = document.createElement("TreeNode");

        Element contentElement = document.createElement("Content");
        contentElement.appendChild(document.createTextNode(treeNode.rectangle.getContent()));
        treeNodeElement.appendChild(contentElement);

        Element subTreeNodesElement = document.createElement("SubTreeNodes");
        treeNodeElement.appendChild(subTreeNodesElement);
        if (treeNode.list != null) {
            exportList(document, subTreeNodesElement, treeNode.list);
        }
        element.appendChild(treeNodeElement);
    }

    private void exportList(Document document, Element element, List list) {
        Element xElement = document.createElement("X");
        xElement.appendChild(document.createTextNode(String.valueOf(list.x)));
        element.appendChild(xElement);
        Element yElement = document.createElement("Y");
        yElement.appendChild(document.createTextNode(String.valueOf(list.y)));
        element.appendChild(yElement);
        for (TreeNode treeNode : list.treeNodes) {
            exportTreeNode(document, element, treeNode);
        }
    }

    private void unregisterList(List list) {
        lists.remove(list);
        list.treeNodes.clear();
    }

    private void registerList(List list) {
        lists.add(list);
    }

    private String getListHead(List list) {
        return list.treeNodes.getFirst().rectangle.getContent();
    }

    private void registerStructure(List list) {
        structures.put(getListHead(list), list);
    }

    private void unregisterStructure(List list) {
        structures.remove(getListHead(list));
    }

    private void registerFunction(List list) {
        String[] strings = getListHead(list).split(" ", 3);
        functions.put(strings[0], list);
        functionNameToParameters.put(strings[0], 3 == strings.length ? strings[2] : "");
    }

    private void unregisterFunction(List list) {
        String[] strings = getListHead(list).split(" ", 3);
        functions.remove(strings[0]);
        functionNameToParameters.remove(strings[0]);
    }

    // listType: 0是结构定义，1是函数定义
    private void getList(Node node, boolean structureOrFunction) {
        List list = importList(node);
        if (structureOrFunction) {
            registerStructure(list);
        } else {
            registerFunction(list);
        }
    }

    // 主要是用于导入代码后重新设置main函数矩形颜色
    private void setListColor(List list, Color color) {
        for (TreeNode treeNode : list.treeNodes) {
            treeNode.rectangle.color = color;
            if (null != treeNode.list) {
                setListColor(treeNode.list, color);
            }
        }
    }

    public boolean getCodeFromXml(File file) {
        // 先清空现有表项
        clear(false);

        // 然后从XML读取
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);
            Node root = document.getDocumentElement();
            NodeList childNodes = root.getChildNodes();

            Node sizeNode = childNodes.item(0);
            setScale(Double.parseDouble(sizeNode.getFirstChild().getNodeValue()));

            Node structuresNode = childNodes.item(1);
            Rectangle.currentGroupColor = colors[0];
            for (int i = 0; i < structuresNode.getChildNodes().getLength(); i++) {
                getList(structuresNode.getChildNodes().item(i), true);
            }

            Node functionsNode = childNodes.item(2);
            Rectangle.currentGroupColor = colors[2];
            for (int i = 0; i < functionsNode.getChildNodes().getLength(); i++) {
                getList(functionsNode.getChildNodes().item(i), false);
            }
            // 设置main函数矩形颜色
            List main = functions.get("");
            setListColor(main, colors[1]);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            return false;
        }

        updateElements();

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
            size.appendChild(document.createTextNode(String.valueOf(Rectangle.scale)));
            root.appendChild(size);

            Element structuresNodes = document.createElement("Structures");
            for (List list : structures.values()) {
                if (list != null) { // string和number的list是null
                    Element structure = document.createElement("Structure");
                    exportList(document, structure, list);
                    structuresNodes.appendChild(structure);
                }
            }
            root.appendChild(structuresNodes);

            Element functionsNodes = document.createElement("Functions");
            for (List list : functions.values()) {
                Element function = document.createElement("Function");
                exportList(document, function, list);
                functionsNodes.appendChild(function);
            }
            root.appendChild(functionsNodes);

            document.appendChild(root);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            // 设置成UTF-16可以让桌面端也能够正确处理
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
//             transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 不能添加换行否则会解析失败
            // 原来是new StreamResult(file)，这样当文件名中有中文，安卓下会变成乱码，Linux下倒是正常，可能FileOutputStream专治各种乱码
            transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(file)));
            return true;
        } catch (ParserConfigurationException | TransformerException | FileNotFoundException e) {
            return false;
        }
    }

    public void createFunction(String s) {
        if (s.equals("")) {
            graphicsOperations.showMessage("Function name cannot be empty!");
            return;
        }
        Rectangle.currentGroupColor = colors[2];
        List list = new List(x, y);
        Rectangle rectangle = new Rectangle(s);
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        list.treeNodes.add(treeNode);

        rectangle = new Rectangle("+");
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        list.treeNodes.add(treeNode);

        registerList(list);
        registerFunction(list);
    }

    public void createStructure(String s) {
        if (s.equals("")) {
            graphicsOperations.showMessage("Structure name cannot be empty!");
            return;
        }
        Rectangle.currentGroupColor = colors[0];
        List list = new List(x, y);
        Rectangle rectangle = new Rectangle(s);
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        list.treeNodes.add(treeNode);

        rectangle = new Rectangle("+");
        treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        list.treeNodes.add(treeNode);

        registerList(list);
        registerStructure(list);
    }

    private TreeNode createMember(String s) {
        Rectangle.currentGroupColor = selectedTreeNode.rectangle.color;
        TreeNode treeNode = new TreeNode();

        Rectangle rectangle = new Rectangle(s);
        treeNode.rectangle = rectangle;

        if (s.startsWith("if ") || s.equals("else") || s.startsWith("while ")) {
            List list = new List();
            treeNode.list = list;
            rectangle = new Rectangle("");
            TreeNode subTreeNode = new TreeNode();
            subTreeNode.rectangle = rectangle;
            treeNode.list.treeNodes.add(subTreeNode);

            rectangle = new Rectangle("+");
            subTreeNode = new TreeNode();
            subTreeNode.rectangle = rectangle;
            treeNode.list.treeNodes.add(subTreeNode);
            registerList(list);
        }

        return treeNode;
    }

    private boolean canCreateElse(List list) {
        for (TreeNode treeNode : Executor.getDataList(list)) {
            if (treeNode.rectangle.getContent().startsWith("if ") && treeNode.list.equals(selectedList)) {
                return true;
            }
            if (null != treeNode.list) {
                if (canCreateElse(treeNode.list)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insert(TreeNode t, boolean pasted) {
        if (pasted) {
            t.rectangle.color = selectedTreeNode.rectangle.color;
            if (null != t.list) {
                setListColor(t.list, selectedTreeNode.rectangle.color);
            }
        }
        for (int i = 0; i < selectedList.treeNodes.size(); i++) { // 这里原来是for (int i = 0; i < selectedList.size() - 1; i++)，这是一处耐人寻味的误解
            if (selectedList.treeNodes.get(i).equals(selectedTreeNode)) {
                selectedList.treeNodes.add(i + 1, t);
                // 似乎遍历的时候selectedList.size()不会改变
            }
        }
        selectedTreeNode = t;
    }

    // 先像点带加号的矩形添加新矩形那样新建一个矩形，然后做一次轮换把新建矩形换到指定位置，包括矩形链表和TreeNode链表。要在第一个矩形上面添加矩形，只能把第一个矩形设为头节点，比如指令链表的头节点为“main”，结构定义和集合运算定义的头节点自然就分别是结构名和参数列表，最后处理时略过即可
    public void insert(String s) {
        boolean savedDoCreateMember = doCreateMember;
        if (doCreateMember) {
            doCreateMember = false;
        }
        if (Color.RED == selectedTreeNode.rectangle.color.rectangleColor) {
            if (s.startsWith("if ") || s.equals("else") || s.startsWith("while")) {
                graphicsOperations.showMessage("Cannot create such a statement here!");
                selectedTreeNode = null;
                return;
            }
        }
        if (selectedTreeNode.rectangle.getContent().equals("else")) {
            graphicsOperations.showMessage("Cannot create a statement here!");
            selectedTreeNode = null;
            return;
        }
        if (s.equals("else")) {
            boolean canCreateElse = false;
            // 只有在if语句的子句里才能新建else语句
            for (List list : functions.values())  {
                if (canCreateElse(list)) {
                    canCreateElse = true;
                    break;
                }
            }
            // else语句只能点击“+”矩形新建，不能由插入、修改来
            canCreateElse = canCreateElse && savedDoCreateMember;
            if (!canCreateElse) {
                graphicsOperations.showMessage("Cannot create an else statement here!");
                selectedTreeNode = null;
                return;
            }
        }
            // 将这个判断从下一个判断中提取出来是较好的选择，否则下一个判断是if (selectedRectangle == null || selectedRectangle.content.startsWith("else ") || s.equals(""))，大括号里面还要再进行一次if (s.equals(""))判断，这就有了重复代码，不优雅
        if (s.equals("")) {
            selectedTreeNode = null;
            return;
        }

        insert(createMember(s), false);

        // 新增else语句后删除"+"结点
        if (s.equals("else")) {
            selectedList.treeNodes.removeLast();
        }
        if (null != selectedTreeNode.list) {
            moveToTail(selectedTreeNode.list);
        }
    }

    private void unregisterTreeNode(TreeNode treeNode) {
        if (treeNode.list != null) {
            for (TreeNode t : treeNode.list.treeNodes) {
                unregisterTreeNode(t);
            }
            unregisterList(treeNode.list);
        }
    }

    // 选中矩形后菜单项选删除，先删除选中的矩形所属TreeNode及其子TreeNode之包括从矩形链表中删除TreeNode对应的矩形，然后把该组TreeNode后面的TreeNode的矩形的y值减去一个矩形高
    public void remove() {
        for (int i = 0; i < selectedList.treeNodes.size(); i++) { // for (TreeNode t : selectedList) 会java.util.ConcurrentModificationException
            TreeNode t = selectedList.treeNodes.get(i);
            if (t.equals(selectedTreeNode)) {
                // 特殊结点删除作特殊处理
                if (t.rectangle.getContent().equals("")) { // 子句的第一个矩形不允许删除
                    graphicsOperations.showMessage("Cannot remove the statement!");
                    break;
                } else if (t.equals(selectedList.treeNodes.getFirst())) { // 全部删除，包括结构定义、函数定义
                    if (Color.RED == t.rectangle.color.rectangleColor) { // 说明是结构定义
                        unregisterStructure(selectedList);
                    } else if (Color.YELLOW == t.rectangle.color.rectangleColor) { // 说明是函数定义
                        unregisterFunction(selectedList);
                    }
                    // 要么是结构定义要么是函数定义，删不掉的是main函数和子句，在上一个判断中已经返回了
                    unregisterList(selectedList);
                } else {
                    unregisterTreeNode(t);
                    selectedList.treeNodes.remove(t);
                    // 因为新建else结点的时候把"+"结点删了，所以删else结点的时候加回去
                    if (t.rectangle.getContent().equals("else")) {
                        Rectangle.currentGroupColor = t.rectangle.color; // 这句很必要
                        Rectangle rectangle = new Rectangle("+");
                        TreeNode treeNode = new TreeNode();
                        treeNode.rectangle = rectangle;
                        selectedList.treeNodes.add(treeNode);
                    }
                }
            }
        }
        selectedTreeNode = null;
    }

    public boolean hasSelectedTreeNode() {
        return null != selectedTreeNode;
    }

    public String getRectangleContent() {
        return null != selectedTreeNode ? selectedTreeNode.rectangle.getContent() : "";
    }

    // 选中矩形后长按即弹出输入窗口让更新内容，如果是改了像if、else、while这样有子句者则删除子句，或者为了防止误触还是设置一个modify菜单项。本来是可以直接改的，不过改变等价于删除加添加，只是这样删除第一个矩形的时候就难了，可以在TreeNode链表再添加一个不关联矩形的头结点，只是第一个矩形其实没有删除的必要
    public void modify(String s) {
        // 不允许修改main函数和子句的头结点，不允许修改或修改为else语句或“”
        if (selectedTreeNode.rectangle.getContent().equals("else") || selectedTreeNode.rectangle.getContent().equals("") || s.equals("else") || s.equals("")) {
            graphicsOperations.showMessage("Cannot modify or change to an else or empty statement!");
            selectedTreeNode = null;
            return;
        }
        if (Color.RED == selectedTreeNode.rectangle.color.rectangleColor) {
            if (s.startsWith("if ") || s.startsWith("while")) {
                graphicsOperations.showMessage("Cannot create such a statement here!");
                return;
            }
        }
        List list = selectedList;
        TreeNode preTreeNode = list.treeNodes.getFirst();
        for (TreeNode t : list.treeNodes) {
            if (t.equals(selectedTreeNode)) {
                // 特殊结点删除作特殊处理
                if (t.equals(list.treeNodes.getFirst())) {
                    t.rectangle.setContent(s);
                    if (Color.RED == t.rectangle.color.rectangleColor) { // 说明是结构定义
                        unregisterStructure(selectedList);
                        registerStructure(selectedList);
                    } else if (Color.YELLOW == t.rectangle.color.rectangleColor) { // 说明是函数定义
                        unregisterFunction(selectedList);
                        registerFunction(selectedList);
                    }
                    return; // 这个return很必要，一段时间不看都不记得当初是怎么想的怎么写上去的了
                }
                boolean[] hasSubTreeNodes = new boolean[2];
                hasSubTreeNodes[0] = s.startsWith("if ") || s.startsWith("while ");
                // 这时候t就是selectedTreeNode
                String content = t.rectangle.getContent();
                hasSubTreeNodes[1] = content.startsWith("if ") || content.startsWith("while ");
                // 都没有子句或都有子句则直接返回，这是最简单的处理，不要想得太复杂
                // 之前是放在上面的if前面，这样tempRectangle.content就被覆盖了，导致普通指令修改为分支语句后会在doRepaint()时空指针
//                t.rectangle.content = s;
                t.rectangle.setContent(s);
                if (hasSubTreeNodes[0] == hasSubTreeNodes[1]) {
                    return;
                }
                break;
            }
            preTreeNode = t;
        }
        remove();
        selectedTreeNode = preTreeNode;
        insert(s);
    }

    private TreeNode copiedTreeNode;

    private TreeNode copy(TreeNode t) {
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = new Rectangle(t.rectangle.getContent());
        if (null != t.list) {
            treeNode.list = new List(t.list.x, t.list.y);
            for (TreeNode subTreeNode : t.list.treeNodes) {
                treeNode.list.treeNodes.add(copy(subTreeNode));
            }
            registerList(treeNode.list);
        }
        return treeNode;
    }

    public void copy() {
        if (selectedTreeNode.equals(selectedList.treeNodes.getFirst()) || selectedTreeNode.equals(selectedList.treeNodes.getLast())) {
            copiedTreeNode = null;
            graphicsOperations.showMessage("Cannot copy the head or the tail of a list!");
            return;
        }
        Rectangle.currentGroupColor = selectedTreeNode.rectangle.color;
        copiedTreeNode = selectedTreeNode;
    }

    public void paste() {
        if (null != copiedTreeNode) {
            copiedTreeNode = copy(copiedTreeNode);
            insert(copiedTreeNode, true);
        } else {
            graphicsOperations.showMessage("Copy a statement first!");
        }
    }

    private boolean doCreateMember;

    public void click() {
        // selectedGroup和selectedRect应该可以和tempRectangle合并
        selectedList = null;
        selectedTreeNode = null;
        TreeNode preTreeNode = null;
        TreeNode currentTreeNode;
        // 从后往前遍历
        ListIterator<List> iterator = lists.listIterator(lists.size());
        while (iterator.hasPrevious()) {
            List list = iterator.previous();
            int baseX = list.x;
            int baseY = list.y;
            // 判断点击点的y是否在该组的范围内，略微加速查找。由于矩形的长度是由其内文字决定的，遍历开销似乎也能接受，日后有更好的方案再优化。
            if (!(baseY <= y && y <= baseY + list.treeNodes.size() * Rectangle.height)) {
                continue;
            }
            for (TreeNode treeNode : list.treeNodes) {
                currentTreeNode = treeNode;
                int width = currentTreeNode.rectangle.pixelWidth;
                if (baseX <= x && x <= baseX + width && baseY <= y && y <= baseY + Rectangle.height) {
                    selectedTreeNode = currentTreeNode;
                    selectedList = list;
                    moveToTail(list);
                    if (currentTreeNode.rectangle.getContent().equals("+")) {
                        doCreateMember = true;
                        selectedTreeNode = preTreeNode;
                        graphicsOperations.create("Member");
                    }
                    return;
                }
                baseY += Rectangle.height;
                preTreeNode = currentTreeNode;
            }
        }
    }

    private void moveList(List list, int x, int y) {
        list.x += x;
        list.y += y;
    }

    private void moveList(List list, int x, int y, boolean moveSubTreeNodes) {
        moveList(list, x, y);
        if (moveSubTreeNodes) {
            for (TreeNode treeNode : list.treeNodes) {
                if (null != treeNode.list) {
                    moveList(treeNode.list, x, y, true);
                }
            }
        }
    }

    public void drag(int x, int y) {
        if (null == selectedList) {
            for (List list : lists) {
                moveList(list, x - this.x, y - this.y, false);
            }
        } else {
            List list = selectedList;
            moveList(list, x - this.x, y - this.y, true);
        }
        setXY(x, y);
    }

    private void sort(List list, int targetX, int targetY) {
        moveToTail(list);
        int x = targetX - list.x;
        int y = targetY - list.y;
        moveList(list, x, y, false);
        y = list.y;
        for (TreeNode t : list.treeNodes) {
            if (null != t.list) {
                sort(t.list, list.x + Rectangle.width, y);
            }
            y += Rectangle.height;
        }
    }

    private int getMaxXofList(List list) {
        int maxXofList = 0;
        for (TreeNode t : list.treeNodes) {
            int treeNodeX = list.x + t.rectangle.pixelWidth;
            int subTreeNodesX = null != t.list ? getMaxXofList(t.list) : 0;
            int maxXofTreeNode = treeNodeX < subTreeNodesX ? subTreeNodesX : treeNodeX;
            maxXofList = maxXofTreeNode < maxXofList ? maxXofList : maxXofTreeNode;
        }
        return maxXofList;
    }

    private int baseX, baseY;
    private List lastList;
    private void sort(HashMap<String, List> hashMap, int capacity) {
        int count = 0;
        for (List list : hashMap.values()) {
            // 这是专门处理void的。
            if (null == list) {
                continue;
            }
            // 这是专门为main函数准备的，这里结构和函数一起处理了。
            if (!list.treeNodes.getFirst().rectangle.getContent().equals("")) {
                sort(list, baseX, baseY);
                lastList = list;
                baseY += Rectangle.height;
                if (0 != capacity) {
                    count += 1;
                    if (count == capacity) {
                        count = 0;
                        baseX = getMaxXofList(lastList);
                        baseY = 0;
                    }
                }
            }
        }
    }

    public void sort(int capacity) {
        List main = functions.get("");
        sort(main, 0, 0);
        baseX = getMaxXofList(functions.get(""));
        baseY = 0;
        sort(structures, capacity);
        if (0 != baseY) {
            baseX = getMaxXofList(lastList);
            baseY = 0;
        }
        sort(functions, capacity);
    }

    public void doWheelRotation(int x, int y, double s) {
        if (Rectangle.scale / s < 0.4 || Rectangle.scale / s > 2.5) { // 缩放倍率不超过基准倍率2.5倍
            return;
        }
        setScale(Rectangle.scale / s);
        for (List list : lists) {
            list.x = (int) (x + (list.x - x) / s);
            list.y = (int) (y + (list.y - y) / s);
            for (TreeNode t : list.treeNodes) {
                t.rectangle.setContent(t.rectangle.getContent());
            }
        }
    }

    public void moveToPage(String s) {

    }

    private void moveToTail(List list) {
        if (!lists.getLast().equals(list)) {
            lists.remove(list);
            lists.add(list);
        }
    }

    private void drawFocusedTreeNode(int x, int y) {
        TreeNode focusedTreeNode = doCreateMember ? null : selectedTreeNode;
        if (focusedTreeNode != null) {
            Rectangle rectangle = focusedTreeNode.rectangle;
            rectangle.draw(x, y, rectangle.color.stringColor, rectangle.color.rectangleColor);
            List targetList = null;

            // 选中有子句的矩形后同时反色显示其子句的第一个矩形，便于查看
            if (focusedTreeNode.rectangle.getContent().startsWith("if ") || focusedTreeNode.rectangle.getContent().equals("else") || focusedTreeNode.rectangle.getContent().startsWith("while ")) {
                targetList = focusedTreeNode.list;
                rectangle = targetList.treeNodes.getFirst().rectangle;
                rectangle.draw(targetList.x, targetList.y, rectangle.color.stringColor, rectangle.color.rectangleColor);
            }

            // 选中函数调用语句所在的矩形后反色显示调用到的函数所在主句的第一个矩形，便于查看。
            if (null != focusedTreeNode.matchedFunction && !focusedTreeNode.matchedFunction.treeNodes.isEmpty()) {
                targetList = focusedTreeNode.matchedFunction;
                rectangle = focusedTreeNode.matchedFunction.treeNodes.getFirst().rectangle;
                rectangle.draw(targetList.x, targetList.y, rectangle.color.stringColor, rectangle.color.rectangleColor);
            }

            // 选中定义语句所在的矩形后反色显示结构定义所在主句的第一个矩形，便于查看
            List structure = null != focusedTreeNode.elements ? structures.get(focusedTreeNode.elements.get(0)) : null;
            if (null != structure) { // 本来是CommandType.DEFINE == selectedTreeNode.commandType，不过现在这样也好
                targetList = structure;
                rectangle = structure.treeNodes.getFirst().rectangle;
                rectangle.draw(targetList.x, targetList.y, rectangle.color.stringColor, rectangle.color.rectangleColor);
            }

            if (null != targetList) {
                moveToTail(targetList);
            }
        }
    }

    private Integer focusedX, focusedY;
    private LinkedList<Arrow> arrows = new LinkedList<>();
    private void drawList(List list) {
        baseX = list.x;
        // 先减去Rectangle.height是为了baseY += Rectangle.height;这句能放到最前面，放最前面是说必须执行到。
        baseY = list.y - Rectangle.height;
        for (TreeNode treeNode : list.treeNodes) {
            baseY += Rectangle.height;
            if (baseY >= height) {
                break;
            }
            if (baseY + Rectangle.height <= 0) {
                continue;
            }
            if (baseX + treeNode.rectangle.pixelWidth <= 0) {
                continue;
            }
            treeNode.rectangle.draw(baseX, baseY);
            if (null != treeNode.list) {
                if (null == treeNode.list.x) {
                    treeNode.list.x = baseX + Rectangle.width;
                    treeNode.list.y = baseY;
                }
                arrows.add(new Arrow(baseX, baseY, treeNode.list.x, treeNode.list.y));
            }
            if (treeNode.equals(selectedTreeNode)) {
                focusedX = baseX;
                focusedY = baseY;
            }
        }
        if (!arrows.isEmpty()) {
            for (Arrow arrow : arrows) {
                arrow.draw();
            }
            arrows.clear();
        }
    }

    public void paintEverything() {
        for (List list : lists) {
            if (list.x < width && list.y < height) {
                drawList(list);
            }
        }
        if (null != selectedTreeNode && null != focusedX) {
            drawFocusedTreeNode(focusedX, focusedY);
            focusedX = null;
        }
    }
}
