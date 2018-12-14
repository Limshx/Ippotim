package Kernel;

import java.util.ArrayList;
import java.util.HashMap;

// 变量结构
class Instance {
    String type;
    Object name;

    // 延迟或者说lazy加载或者说初始化方案中必需的标志位，挺不错的抽象
    private boolean automaticallyInitElements;
    private ArrayList<Instance> elements;

    // 对数组的实现是在此设立一个数组用以存放数组的各下标上限，使用具体的数组元素时先看数组名是否在变量表中，在的话再看各下标是否大于0且小于其上限，符合要求了再看类似a[1][2]之具体数组元素是否在arrayElements变量表里，不在就加进去再用，在就直接用
    private HashMap<String, Instance> arrayElements;

    // 这样就是说常量也是变量之是变量的特例，type为null，name为其值。
    private Instance() {}

    // 内部类中私有构造函数似乎对宿主类可见
    Instance(String type, String name) {
        this(type, name, true);
    }

    // 这里原来是Instance(String var0, String var1, boolean initElements)，想要在里面拿到matchedStructureOrFunction，传入一个TreeNode的话似乎不好处理，传入matchedStructureOrFunction的话似乎也不优雅，直接把initElements替换为matchedStructureOrFunction是一种很好的解决方案之改为Instance(String var0, String var1, List<TreeNode> elements)，不过这其实就是或者说都是TreeNode里的数据，所以直接Instance(TreeNode command)更好，然而果然还是要加上initElements防止死循环。不过getArrayInstance()里有非根据TreeNode新建Instance的需求，这样还是得细化参数。最终因为函数调用时新建Instance不好传入指向结构定义的链表，还是决定恢复成原来的设计。
    private Instance(String type, String name, boolean initElements) {
        // type和name本来就是null，就不用开个else赋为null了，这是一种需要细心才能发现的优化。
        this.type = type;
        this.name = name;
        // 虽然绝大多数变量都用不到数组元素，但如果不初始化的话似s.next[0]赋了值而s.next还是null，s.next[0]为不为null直接决定了s.next作为参数后会不会同步修改，这是不合理的。至于说每一个函数都有一个总的arrayElements，函数调用的时候映射关系的继承是个问题。这个暂时没有更好的解决方案，暂搁置。
//        this.arrayElements = new HashMap<>();
        if ("void".equals(type)) {
            // 基本数据类型的值设计为其唯一的元素且也是变量，暂时假装不是元素而已，这样是方便判断是不是有传统意义上的结构体元素
            // 本来是结构体系定义有元素的则初始化elements，不过基本数据类型的值设计为其元素了，所以一起初始化
            this.elements = new ArrayList<>();
            this.elements.add(new Instance());
        } else {
            if (initElements) {
                automaticallyInitElements = true;
            }
        }
    }

    // 这样就是按需获取或者说分配了，就是说像广义数组哈希表arrayElements不用定义数组也能直接使用任意维度的数组变量那样，比如结构体S中定义有S类型的成员next，则S的实例比如说s可以类似s.next.next...之无限取成员而不会报空指针。
    ArrayList<Instance> getElements() {
        if (null == elements && automaticallyInitElements) {
            // 本来是结构体系定义有元素的则初始化elements，不过基本数据类型的值设计为其元素了，所以一起初始化
            this.elements = new ArrayList<>();
            List elements = Adapter.structures.get(type);
            if (null != elements) {
                // 从1开始才是语句
                for (int i = 1; i < elements.treeNodes.size(); i++) {
                    // 如果结构体元素中有基本数据类型元素，则初始化
                    TreeNode t = elements.treeNodes.get(i);
                    this.elements.add(new Instance(t.elements.get(0), t.elements.get(1), false));
                }
            }
        }
        return elements;
    }

    void setElements(ArrayList<Instance> elements) {
        automaticallyInitElements = null != elements;
        this.elements = elements;
    }

    HashMap<String, Instance> getArrayElements() {
        if (null == arrayElements) {
            arrayElements = new HashMap<>();
        }
        return arrayElements;
    }

    void setArrayElements(HashMap<String, Instance> arrayElements) {
        this.arrayElements = arrayElements;
    }
}
