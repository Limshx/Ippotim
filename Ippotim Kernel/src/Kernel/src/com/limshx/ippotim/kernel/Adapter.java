package com.limshx.ippotim.kernel;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

public class Adapter {
    private int x, y;
    static List selectedList;
    static int selectedTreeNodeIndex;
    static GraphicsOperations graphicsOperations; // 本来是static GraphicsOperations drawTable = new DrawTable();之实际用不到所有的DrawTable的方法与属性，通过接口或抽象类可以完美解耦合，这就是函数指针、接口、抽象类之类的真正奥义
    static int width, height;
    // 哈希表是建立映射的一种数据结构，可以用来给元素添加属性，而不需要新建一个类
    static HashMap<String, List> structures = new HashMap<>();
    static HashMap<String, List> functions = new HashMap<>();
    static HashMap<String, HashMap<String, Instance>> structureNameToInstances = new HashMap<>();
    static HashMap<String, HashMap<String, Instance>> functionNameToInstances = new HashMap<>();

    // 0是结构定义用色，1是main函数用色，2是函数定义用色
    private Color[] colors = {new Color(Color.RED, Color.YELLOW), new Color(Color.WHITE, Color.BLACK), new Color(Color.YELLOW, Color.RED)};

    private void setScale(double s) {
        Rectangle.scale = s;
        Rectangle.updateSize();
        Rectangle.tail.setContent("+");
    }

    private void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setScreen(int width, int height) {
        Adapter.width = width;
        Adapter.height = height;
    }

    public Adapter(GraphicsOperations g, int x, int y, double s, File file) {
        graphicsOperations = g;
        setScreen(x, y);
        Rectangle.defaultScale = 1 / s;
        Rectangle.tail = new Rectangle("+");
        setScale(Rectangle.defaultScale);
        addDefaultMainFunction();
        properties = file;
        output = new File(properties.getParent() + "/ippotim.output");
        loadProperties();
    }

    private void addDefaultMainFunction() {
        // main函数也并入函数集合
        List.currentGroupColor = colors[1];
        List main = new List(width / 2 - Rectangle.width / 2, height / 2 - Rectangle.height, null);
        Rectangle rectangle = new Rectangle("");
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = rectangle;
        main.treeNodes.add(treeNode);
        registerFunction(main, true);
        selectedList = main;
        selectedTreeNodeIndex = 0;
    }

    private File properties;

    private void showDifferentKeywords() {
        for (int i = 0; i < Syntax.defaultKeywords.length; i++) {
            if (!Syntax.currentKeywords[i].equals(Syntax.defaultKeywords[i])) {
                graphicsOperations.showMessage("The keyword \"" + Syntax.defaultKeywords[i] + "\" is now \"" + Syntax.currentKeywords[i] + "\"");
            }
        }
    }

    private void loadProperties() {
        if (properties.exists()) {
            Properties properties = new Properties();
            try {
                InputStream inputStream = new FileInputStream(this.properties);
                properties.load(inputStream);
                // structures.remove(Syntax.currentKeywords[0]); // 执行本函数的时候structures已经是空的了
                for (int i = 0; i < Syntax.currentKeywords.length; i++) {
                    String keyword = properties.getProperty(Syntax.defaultKeywords[i]);
                    Syntax.currentKeywords[i] = keyword;
                }
                structures.put(Syntax.currentKeywords[0], null);
                showDifferentKeywords();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            setCurrentKeywords(Syntax.defaultKeywords, true);
        }
    }

    private void storeProperties(String[] keywords) {
        Properties properties = new Properties();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.properties);
            for (int i = 0; i < keywords.length; i++) {
                properties.setProperty(Syntax.defaultKeywords[i], keywords[i]);
            }
            properties.store(fileOutputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getDefaultKeywords() {
        return Syntax.defaultKeywords;
    }

    public String[] getCurrentKeywords() {
        String[] keywords = new String[Syntax.currentKeywords.length];
        System.arraycopy(Syntax.currentKeywords, 0, keywords, 0, keywords.length);
        return keywords;
    }

    public void setCurrentKeywords(String[] keywords, boolean storeProperties) {
        if (null != keywords) {
            if (storeProperties) {
                storeProperties(keywords);
            }
            structures.remove(Syntax.currentKeywords[0]);
            System.arraycopy(keywords, 0, Syntax.currentKeywords, 0, keywords.length);
            structures.put(Syntax.currentKeywords[0], null);
        }
    }

    private void resetSelectedTreeNode() {
        selectedTreeNodeIndex = -1;
        selectedList = null;
    }

    private void clear(boolean addDefaultMainFunction) {
        setScale(Rectangle.defaultScale);
        List.lists.clear();
        structures.clear();
        functions.clear();
        structureNameToInstances.clear();
        functionNameToInstances.clear();
        resetSelectedTreeNode();
        if (addDefaultMainFunction) {
            addDefaultMainFunction();
            loadProperties();
        }
    }

    public void clear() {
        clear(true);
    }

    private boolean updateElements(List list) {
        // 从1开始才是语句
        for (int i = 1; i < list.treeNodes.size(); i++) {
            TreeNode t = list.treeNodes.get(i);
            t.updateElements();
            if (null == t.statementType) {
                selectedTreeNodeIndex = i;
                return true;
            }
            if (null != t.list) {
                if (updateElements(t.list)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean updateElements(Color color) {
        for (List list : List.lists) {
            if (color == list.color) {
                selectedList = list;
                if (updateElements(list)) {
                    return true;
                }
                if (colors[0] == color) {
                    registerStructure(list, false);
                } else {
                    registerFunction(list, false);
                }
            }
        }
        return false;
    }

    // 结构定义注册的时候已经更新过了，这里只更新函数
    private boolean updateElements() {
        for (Color color : colors) {
            if (updateElements(color)) {
                return true;
            }
        }
        return false;
    }

    private void inspectCode() {
        Executor.stop = false;
        if (updateElements()) {
            error("Invalid statement!");
        } else {
            resetSelectedTreeNode();
        }
    }

    private static boolean error;
    private File output;
    static FileOutputStream fileOutputStream;

    public void run() {
        Executor executor = new Executor();
        inspectCode();
        List main = functions.get("");
        HashMap<String, Instance> instances = new HashMap<>();
        try {
            fileOutputStream = new FileOutputStream(output);
            executor.run(instances, main);
            fileOutputStream.close();
            // debug就是研究非预期行为的成因，看代码推敲是不够的，还需要有显示中间数据的手段
        } catch (Exception e) {
            error(e.toString());
        }
        if (error) {
            error = false;
        } else {
            resetSelectedTreeNode();
        }
    }

    static void error(String s) {
        // 可能会触发多次，故只收集一次
        if (!Executor.stop) {
            error = true;
            graphicsOperations.showMessage(s);
            graphicsOperations.doRepaint();
            Executor.stop = true;
        }
    }

    public void stop() {
        Executor.stop = true;
    }

    private TreeNode importTreeNode(Node node, List preList) {
        NodeList childNodes = node.getChildNodes();

        Node contentNode = childNodes.item(0);
        String content = contentNode.hasChildNodes() ? contentNode.getFirstChild().getNodeValue() : "";
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = new Rectangle(content);

        Node subTreeNodes = childNodes.item(1);
        // 这里原来有个else，即没有子结点就将subTreeNodes置为null，然而这个初始化就是null，所以直接去掉，这是一种典型的优化。
        if (subTreeNodes.hasChildNodes()) {
            treeNode.list = importList(subTreeNodes, preList); // 之前subGroupNumber是maxGroupNumber + 1，这样在循环的时候maxGroupNumber就会不断增加从而导致逻辑错误，这个要引以为鉴。
        }
        return treeNode;
    }

    private List importList(Node node, List preList) {
        int x = Integer.parseInt(node.getChildNodes().item(0).getFirstChild().getNodeValue());
        int y = Integer.parseInt(node.getChildNodes().item(1).getFirstChild().getNodeValue());
        List list = new List(x, y, preList);
        for (int i = 2; i < node.getChildNodes().getLength(); i++) { // 从2开始是因为0和1已经用来给x和y赋值了
            list.treeNodes.add(importTreeNode(node.getChildNodes().item(i), list));
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

            Node size = childNodes.item(0);
            setScale(Double.parseDouble(size.getFirstChild().getNodeValue()));

            Node keywords = childNodes.item(1);
            for (int i = 0; i < keywords.getChildNodes().getLength(); i++) {
                Syntax.currentKeywords[i] = keywords.getChildNodes().item(i).getFirstChild().getNodeValue();
            }
            structures.put(Syntax.currentKeywords[0], null);
            showDifferentKeywords();

            Node structures = childNodes.item(2);
            List.currentGroupColor = colors[0];
            for (int i = 0; i < structures.getChildNodes().getLength(); i++) {
                List structure = importList(structures.getChildNodes().item(i), null);
                registerStructure(structure, true);
            }

            Node functions = childNodes.item(3);
            List.currentGroupColor = colors[2];
            for (int i = 0; i < functions.getChildNodes().getLength(); i++) {
                List function = importList(functions.getChildNodes().item(i), null);
                registerFunction(function, true);
            }

            inspectCode();
            // 设置main函数矩形颜色
            List main = Adapter.functions.get("");
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
            size.appendChild(document.createTextNode(String.valueOf(Rectangle.scale)));
            root.appendChild(size);

            Element keywords = document.createElement("Keywords");
            for (int i = 0; i < Syntax.defaultKeywords.length; i++) {
                Element keyword = document.createElement(Syntax.defaultKeywords[i]);
                keyword.appendChild(document.createTextNode(Syntax.currentKeywords[i]));
                keywords.appendChild(keyword);
            }
            root.appendChild(keywords);

            Element structures = document.createElement("Structures");
            for (List list : Adapter.structures.values()) {
                if (list != null) {
                    Element structure = document.createElement("Structure");
                    exportList(document, structure, list);
                    structures.appendChild(structure);
                }
            }
            root.appendChild(structures);

            Element functions = document.createElement("Functions");
            for (List list : Adapter.functions.values()) {
                Element function = document.createElement("Function");
                exportList(document, function, list);
                functions.appendChild(function);
            }
            root.appendChild(functions);

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

    private void registerStructure(List list, boolean listOrInstance) {
        String structureName = List.getListHead(list);
        if (listOrInstance) {
            structures.put(structureName, list);
        } else {
            structureNameToInstances.put(structureName, Instance.getStructureInstances(list));
        }
    }

    private void unregisterStructure(List list) {
        String structureName = List.getListHead(list);
        structures.remove(structureName);
        structureNameToInstances.remove(structureName);
    }

    private void registerFunction(List list, boolean listOrInstance) {
        String[] strings = List.getListHead(list).split(" ", 3);
        if (listOrInstance) {
            functions.put(strings[0], list);
        } else {
            String parameters = 3 == strings.length ? strings[2] : "";
            functionNameToInstances.put(strings[0], Instance.getFunctionInstances(parameters));
        }
    }

    private void unregisterFunction(List list) {
        String[] strings = List.getListHead(list).split(" ", 3);
        functions.remove(strings[0]);
        functionNameToInstances.remove(strings[0]);
    }

    private boolean isValidFunctionHead(String s) {
        String[] strings = s.split(" ", 3);
        if (1 == strings.length || !strings[1].equals(":")) {
            graphicsOperations.showMessage("The function head is invalid!");
            return false;
        }
        if (functions.containsKey(strings[0])) {
            graphicsOperations.showMessage("The function name is in use!");
            return false;
        }
        return true;
    }

    public void createFunction(String s) {
        if (isValidFunctionHead(s)) {
            List.currentGroupColor = colors[2];
            List function = new List(x, y, null);
            Rectangle rectangle = new Rectangle(s);
            TreeNode treeNode = new TreeNode();
            treeNode.rectangle = rectangle;
            function.treeNodes.add(treeNode);
            registerFunction(function, true);
        }
    }

    private boolean isValidStructureName(String s) {
        String[] strings = s.split(" ");
        if (1 != strings.length) {
            graphicsOperations.showMessage("The structure name is invalid!");
            return false;
        }
        if (Syntax.isKeyword(s)) {
            graphicsOperations.showMessage("A structure name can not be a keyword!");
            return false;
        }
        if (structures.containsKey(s)) {
            graphicsOperations.showMessage("The structure name is in use!");
            return false;
        }
        return true;
    }

    public void createStructure(String s) {
        if (isValidStructureName(s)) {
            List.currentGroupColor = colors[0];
            List structure = new List(x, y, null);
            Rectangle rectangle = new Rectangle(s);
            TreeNode treeNode = new TreeNode();
            treeNode.rectangle = rectangle;
            structure.treeNodes.add(treeNode);
            registerStructure(structure, true);
        }
    }

    private TreeNode createMember(String s) {
        List.currentGroupColor = selectedList.color;
        TreeNode treeNode = new TreeNode();

        Rectangle rectangle = new Rectangle(s);
        treeNode.rectangle = rectangle;
        treeNode.updateElements();
        if (StatementType.IF == treeNode.statementType || StatementType.ELSE == treeNode.statementType || StatementType.WHILE == treeNode.statementType) {
            treeNode.list = new List(selectedList);
            rectangle = new Rectangle("");
            TreeNode subTreeNode = new TreeNode();
            subTreeNode.rectangle = rectangle;
            treeNode.list.treeNodes.add(subTreeNode);
        }

        return treeNode;
    }

    private boolean canCreateElse(List list) {
        if (null != list.preList) {
            // 从1开始才是语句
            for (int i = 1; i < list.preList.treeNodes.size(); i++) {
                TreeNode t = list.preList.treeNodes.get(i);
                t.updateElements();
                if (StatementType.IF == t.statementType && t.list.equals(selectedList)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setListColor(List list, Color color) {
        list.color = color;
        for (TreeNode treeNode : list.treeNodes) {
            if (null != treeNode.list) {
                setListColor(treeNode.list, color);
            }
        }
    }

    private void insert(int index, TreeNode t, boolean pasted) {
        if (pasted) {
            if (null != t.list) {
                setListColor(t.list, selectedList.color);
            }
        }
        selectedTreeNodeIndex = index + 1;
        selectedList.treeNodes.add(selectedTreeNodeIndex, t);
    }

    private boolean isValidStatement(String content, String s, boolean insertOrModify) {
        if (Color.RED == selectedList.color.rectangleColor) {
            String[] strings = s.split(" ");
            if (!structures.containsKey(strings[0])) {
                return false;
            }
        }
        if (insertOrModify) {
            if (s.equals(Syntax.currentKeywords[2])) {
                // 只有在if语句的子句里才能新建else语句
                // else语句只能点击“+”矩形新建，不能由插入、修改来
                return canCreateElse(selectedList) && -1 == selectedTreeNodeIndex;
            }
            return !content.equals(Syntax.currentKeywords[2]);
        } else {
            // 不允许修改main函数和子句的头结点，不允许修改为else语句
            return !content.equals("") && !s.equals(Syntax.currentKeywords[2]);
        }
    }

    // 先像点带加号的矩形添加新矩形那样新建一个矩形，然后做一次轮换把新建矩形换到指定位置，包括矩形链表和TreeNode链表。要在第一个矩形上面添加矩形，只能把第一个矩形设为头节点，比如指令链表的头节点为“main”，结构定义和集合运算定义的头节点自然就分别是结构名和参数列表，最后处理时略过即可
    private void insert(int index, String s) {
        if (isValidStatement(selectedList.treeNodes.get(index).rectangle.getContent(), s, true)) {
            insert(index, createMember(s), false);
        } else {
            graphicsOperations.showMessage("Forbidden statement!");
        }
    }

    public void insert(String s) {
        int index = -1 != selectedTreeNodeIndex ? selectedTreeNodeIndex : selectedList.treeNodes.size() - 1;
        insert(index, s);
    }

    // 选中矩形后菜单项选删除，先删除选中的矩形所属TreeNode及其子TreeNode之包括从矩形链表中删除TreeNode对应的矩形，然后把该组TreeNode后面的TreeNode的矩形的y值减去一个矩形高
    public void remove() {
        TreeNode t = selectedList.treeNodes.get(selectedTreeNodeIndex);
        // 特殊结点删除作特殊处理
        if (t.rectangle.getContent().equals("")) { // 子句的第一个矩形不允许删除
            graphicsOperations.showMessage("Cannot remove the statement!");
        } else if (0 == selectedTreeNodeIndex) { // 全部删除，包括结构定义、函数定义
            if (Color.RED == selectedList.color.rectangleColor) { // 说明是结构定义
                unregisterStructure(selectedList);
            } else if (Color.YELLOW == selectedList.color.rectangleColor) { // 说明是函数定义
                unregisterFunction(selectedList);
            }
            // 要么是结构定义要么是函数定义，删不掉的是main函数和子句，在上一个判断中已经返回了。
            List.unregisterList(selectedList);
            resetSelectedTreeNode();
        } else {
            selectedList.treeNodes.remove(t);
        }
        selectedTreeNodeIndex = -1;
    }

    public boolean hasSelectedTreeNode() {
        return -1 != selectedTreeNodeIndex;
    }

    public String getRectangleContent() {
        return hasSelectedTreeNode() ? selectedList.treeNodes.get(selectedTreeNodeIndex).rectangle.getContent() : "";
    }

    // 选中矩形后长按即弹出输入窗口让更新内容，如果是改了像if、else、while这样有子句者则删除子句，或者为了防止误触还是设置一个modify菜单项。本来是可以直接改的，不过改变等价于删除加添加，只是这样删除第一个矩形的时候就难了，可以在TreeNode链表再添加一个不关联矩形的头结点，只是第一个矩形其实没有删除的必要
    public void modify(String s) {
        TreeNode t = selectedList.treeNodes.get(selectedTreeNodeIndex);
        // 特殊结点删除作特殊处理
        if (0 == selectedTreeNodeIndex) {
            if (Color.RED == selectedList.color.rectangleColor) { // 说明是结构定义
                unregisterStructure(selectedList);
                if (isValidStructureName(s)) {
                    t.rectangle.setContent(s);
                }
                registerStructure(selectedList, true);
            } else if (Color.YELLOW == selectedList.color.rectangleColor) { // 说明是函数定义
                unregisterFunction(selectedList);
                if (isValidFunctionHead(s)) {
                    t.rectangle.setContent(s);
                }
                registerFunction(selectedList, true);
            }
            return; // 这个return很必要，一段时间不看都不记得当初是怎么想的怎么写上去的了
        } else {
            if (!isValidStatement(t.rectangle.getContent(), s, false)) {
                graphicsOperations.showMessage("Forbidden statement!");
                return;
            }
        }
        t.rectangle.setContent(s);
        t.updateElements();
        boolean hasSubTreeNodes = StatementType.IF == t.statementType || StatementType.WHILE == t.statementType;
        if (null != t.list == hasSubTreeNodes) {
            return;
        }
        insert(selectedTreeNodeIndex, s);
        selectedTreeNodeIndex -= 1;
        int index = selectedTreeNodeIndex;
        remove();
        selectedTreeNodeIndex = index;
    }

    private TreeNode copiedTreeNode;

    private TreeNode copy(TreeNode t) {
        TreeNode treeNode = new TreeNode();
        treeNode.rectangle = new Rectangle(t.rectangle.getContent());
        if (null != t.list) {
            treeNode.list = new List(t.list.x, t.list.y, selectedList);
            for (TreeNode subTreeNode : t.list.treeNodes) {
                treeNode.list.treeNodes.add(copy(subTreeNode));
            }
        }
        return treeNode;
    }

    public void copy() {
        TreeNode t = selectedList.treeNodes.get(selectedTreeNodeIndex);
        if (0 == selectedTreeNodeIndex || t.rectangle.getContent().equals(Syntax.currentKeywords[2])) {
            graphicsOperations.showMessage("Cannot copy the head of a list or else statement!");
            return;
        }
        copiedTreeNode = t;
    }

    public void paste() {
        if (null != copiedTreeNode) {
            copiedTreeNode = copy(copiedTreeNode);
            insert(selectedTreeNodeIndex, copiedTreeNode, true);
        } else {
            graphicsOperations.showMessage("Copy a statement first!");
        }
    }

    private boolean click(List list) {
        boolean searchCurrentList = true;
        int baseX = list.x;
        int baseY = list.y;
        // 判断点击点的y是否在该组的范围内，略微加速查找。由于矩形的长度是由其内文字决定的，遍历开销似乎也能接受，日后有更好的方案再优化。
        if (!(baseY <= y && y <= baseY + (list.treeNodes.size() + 1) * Rectangle.height)) {
            searchCurrentList = false;
        }
        if (list.equals(selectedList)) {
            if (baseX <= x && x <= baseX + Rectangle.width && baseY + list.treeNodes.size() * Rectangle.height <= y && y <= baseY + (list.treeNodes.size() + 1) * Rectangle.height) {
                // 通过将selectedTreeNodeIndex置为-1而不是新设置一个标志位可以免去该标志位的还原问题的处理，简化代码。毕竟selectedTreeNodeIndex为-1是看得到的，不还原也无妨。
                selectedTreeNodeIndex = -1;
                graphicsOperations.create("Member");
                return true;
            }
        }
        for (int i = list.treeNodes.size() - 1; i >= 0; i--) {
            TreeNode t = list.treeNodes.get(i);
            // 要先从子句找，不然当子句移动到父句上，选中的都会是父句
            if (null != t.list) {
                if (click(t.list)) {
                    return true;
                }
            }
        }
        if (searchCurrentList) {
            for (int i = 0; i < list.treeNodes.size(); i++) {
                TreeNode t = list.treeNodes.get(i);
                int width = t.rectangle.pixelWidth;
                if (baseX <= x && x <= baseX + width && baseY <= y && y <= baseY + Rectangle.height) {
                    // 不能selectedList.equals(list)，因为selectedList可以为null。
                    if (null != selectedList && !getSourceList(list).equals(getSourceList(selectedList))) {
                        break;
                    }
                    selectedList = list;
                    selectedTreeNodeIndex = i;
                    return true;
                }
                baseY += Rectangle.height;
            }
        }
        return false;
    }

    public void click(int x, int y) {
        setXY(x, y);
        // 从后往前遍历
        ListIterator<List> iterator = List.lists.listIterator(List.lists.size());
        while (iterator.hasPrevious()) {
            List list = iterator.previous();
            if (click(list)) {
                return;
            }
        }
        resetSelectedTreeNode();
    }

    private void moveList(List list, int x, int y) {
        list.x += x;
        list.y += y;
        for (TreeNode treeNode : list.treeNodes) {
            if (null != treeNode.list) {
                moveList(treeNode.list, x, y);
            }
        }
    }

    public void drag(int x, int y) {
        if (null == selectedList) {
            for (List list : List.lists) {
                moveList(list, x - this.x, y - this.y);
            }
        } else {
            List list = selectedList;
            moveList(list, x - this.x, y - this.y);
        }
        setXY(x, y);
    }

    private void sort(List list, int targetX, int targetY) {
        int x = targetX - list.x;
        int y = targetY - list.y;
        moveList(list, x, y);
        moveToTail(list);
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
            if (!list.treeNodes.get(0).rectangle.getContent().equals("")) {
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

    private void doWheelRotation(List list, int x, int y, double s) {
        list.x = (int) (x + (list.x - x) / s);
        list.y = (int) (y + (list.y - y) / s);
        for (TreeNode t : list.treeNodes) {
            t.rectangle.setContent(t.rectangle.getContent());
            if (null != t.list) {
                doWheelRotation(t.list, x, y, s);
            }
        }
    }

    public void doWheelRotation(int x, int y, double s) {
        if (Rectangle.scale / s < 0.4 || Rectangle.scale / s > 2.5) { // 缩放倍率不超过基准倍率2.5倍
            return;
        }
        setScale(Rectangle.scale / s);
        for (List list : List.lists) {
            doWheelRotation(list, x, y, s);
        }
    }

    private void moveToTail(List list) {
        if (!list.equals(List.lists.getLast())) {
            List.lists.remove(list);
            List.lists.add(list);
        }
    }

    private void drawFocusedTreeNode(List list) {
        if (null != list) {
            Rectangle rectangle = list.treeNodes.get(0).rectangle;
            if (!getSourceList(selectedList).equals(getSourceList(list))) {
                drawList(list);
            }
            rectangle.draw(list.x, list.y, false, list.color.stringColor, list.color.rectangleColor);
            if (selectedList != list.preList) {
                moveToTail(list);
            }
        }
    }

    private void drawFocusedTreeNode(int x, int y) {
        TreeNode focusedTreeNode = hasSelectedTreeNode() ? selectedList.treeNodes.get(selectedTreeNodeIndex) : null;
        if (focusedTreeNode != null) {
            Rectangle rectangle = focusedTreeNode.rectangle;
            rectangle.draw(x, y, false, selectedList.color.stringColor, selectedList.color.rectangleColor);
            // 选中第0个矩形就不用进行下面的判断了
            if (0 == selectedTreeNodeIndex) {
                // 反色显示函数头中所有相关结构头。
                if (Color.YELLOW == selectedList.color.rectangleColor) {
                    String functionName = List.getListHead(selectedList).split(" ", 3)[0];
                    HashMap<String, Instance> instances = functionNameToInstances.get(functionName);
                    if (null != instances) {
                        LinkedList<String> matchedStructures = new LinkedList<>();
                        for (Instance instance : instances.values()) {
                            String type = (String) instance.type;
                            if (!matchedStructures.contains(type)) {
                                matchedStructures.add(type);
                            }
                        }
                        for (String matchedStructure : matchedStructures) {
                            drawFocusedTreeNode(structures.get(matchedStructure));
                        }
                    }
                }
                return;
            }
            // 先更新元素
            focusedTreeNode.updateElements();
            List targetList = null;
            if (null == focusedTreeNode.statementType) {
                return;
            }
            switch (focusedTreeNode.statementType) {
                case IF:
                case ELSE:
                case WHILE:
                    // 选中有子句的矩形后同时反色显示其子句的第一个矩形，便于查看
                    targetList = focusedTreeNode.list;
                    break;
                case FUNCTION_CALL:
                    // 选中函数调用语句所在的矩形后反色显示调用到的函数所在主句的第一个矩形，便于查看。
                    targetList = focusedTreeNode.matchedFunction;
                    break;
                case DEFINE:
                    // 选中定义语句所在的矩形后反色显示结构定义所在主句的第一个矩形，便于查看
                    targetList = structures.get(focusedTreeNode.elements.get(0));
                    break;
                default:
                    break;
            }
            drawFocusedTreeNode(targetList);
        }
    }

    private void drawList(List list) {
        drawList(list, null);
    }

    private Integer focusedX, focusedY;

    private void drawList(List list, Arrow preArrow) {
        LinkedList<Arrow> arrows = new LinkedList<>();
        int baseX = list.x;
        // 先减去Rectangle.height是为了baseY += Rectangle.height;这句能放到最前面，放最前面是说必须执行到。
        int baseY = list.y - Rectangle.height;
        for (TreeNode treeNode : list.treeNodes) {
            baseY += Rectangle.height;
            if (null != treeNode.list) {
                if (null == treeNode.list.x) {
                    treeNode.list.x = baseX + Rectangle.width;
                    treeNode.list.y = baseY;
                }
                arrows.add(new Arrow(baseX, baseY, treeNode.list));
            }
            if (baseX + treeNode.rectangle.pixelWidth < 0 || baseX > width || baseY + Rectangle.height < 0 || baseY > height) {
                continue;
            }
            treeNode.rectangle.draw(baseX, baseY, true, list.color.rectangleColor, list.color.stringColor);
            if (hasSelectedTreeNode() && treeNode.equals(selectedList.treeNodes.get(selectedTreeNodeIndex))) {
                focusedX = baseX;
                focusedY = baseY;
            }
        }
        if (null != preArrow) {
            preArrow.draw();
        }
        // selectedList可以为空，两个都可以为空则用Objects.equals()
        if (list.equals(selectedList)) {
            Rectangle.tail.draw(baseX, baseY + Rectangle.height, true, list.color.rectangleColor, list.color.stringColor);
        }
        for (Arrow arrow : arrows) {
            drawList(arrow.list, arrow);
        }
    }

    private List getSourceList(List list) {
        List sourceList = list;
        while (null != sourceList.preList) {
            sourceList = sourceList.preList;
        }
        return sourceList;
    }

    public void paintEverything() {
        if (null == selectedList) {
            for (List list : List.lists) {
                drawList(list);
            }
        } else {
            List list = getSourceList(selectedList);
            moveToTail(list);
            drawList(list);
        }
        // 这里判focusedX是因为虽然选中了矩形，但该矩形不在屏幕内，这种情况只有在桌面端才存在或者说会出现之鼠标可以拖动到窗口外。
        // 不能轻而易举理解的判空等操作要注释，不然后续会以为是冗余代码又难以证明。
        if (hasSelectedTreeNode() && null != focusedX) {
            drawFocusedTreeNode(focusedX, focusedY);
            focusedX = null;
        }
    }
}
