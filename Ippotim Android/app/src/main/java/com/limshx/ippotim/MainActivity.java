package com.limshx.ippotim;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Context context; // View里的Context就是Activity里的this，所以不用从View里静态过来用
    private DrawTable drawTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        InfoBox.adb = new AlertDialog.Builder(context);
        InfoBox.inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        // API level 23以上需要申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            if (initDirectoryFailed()) {
                drawTable.showMessage("Create path failed!");
            }
        }

        drawTable = findViewById(R.id.paint_board);
        getWindowManager().getDefaultDisplay().getRealMetrics(drawTable.displayMetrics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

//        这两句也能实现透明状态栏，但是是半透明之还有一定的alpha值
//        Window window = getWindow();
//        window.setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        drawer.setFitsSystemWindows(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) { // 屏幕旋转时调用，没有需要添加的操作则不需要覆盖。
        super.onConfigurationChanged(newConfig);
        drawTable.isScreenChanged = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (0 == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (initDirectoryFailed()) {
                    drawTable.showMessage("Create path failed!");
                }
            } else {
                System.exit(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // 按下返回键后不销毁当前活动而是切入后台，以加速热启动实现秒开
            moveTaskToBack(false);
        }
    }

    // 这是按需锁方向的标准代码
//    void setOrientation(boolean lock) {
//        if (lock) {
//            setRequestedOrientation(getResources().getConfiguration().orientation % 2);
//        } else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//        }
//    }

    private String manual = "https://github.com/Limshx/Ippotim/blob/master/Docs/manual.md";

    private String[] demos = {"招牌菜", "基础篇 输入输出", "基础篇 条件语句", "基础篇 循环语句", "基础篇 复合布尔表达式", "基础篇 字符串遍历", "基础篇 结构体", "基础篇 嵌套结构体", "基础篇 函数", "基础篇 中文和表情标识符", "基础篇 取余运算", "基础篇 数组", "基础篇 广义数组", "基础篇 综合", "进阶篇 数组", "进阶篇 广义数组", "进阶篇 函数", "进阶篇 递归函数", "进阶篇 局部变量", "进阶篇 链表", "进阶篇 栈", "高级篇 使用头结点的栈", "高级篇 出队后回收利用结点的队列", "高级篇 队列的栈", "高级篇 空类型链表", "高级篇 螺旋三角问题递归", "高级篇 螺旋三角问题非递归"};

    private boolean download(String fileName) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String[] strings = fileName.split(" ");
            for (int i = 0; i < strings.length; i++) {
                stringBuilder.append(URLEncoder.encode(strings[i], "UTF-8"));
                if (i != strings.length - 1) {
                    stringBuilder.append("%20");
                }
            }
            URL url = new URL("https://raw.githubusercontent.com/Limshx/Ippotim/master/Demos/" + stringBuilder.toString());
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(homeDirectory + fileName);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                System.out.println(bytesRead);
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // new Handler().post()也会“java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()”
            drawTable.post(new Runnable() {
                @Override
                public void run() {
                    drawTable.showMessage("Can not connect to GitHub!");
                }
            });
            return false;
        }
        return true;
    }

    private void downloadDemos() {
        new InfoBox("There are no projects. Download demos?", "Cancel", "OK", null) {
            @Override
            void onNegative() {

            }

            @Override
            void onPositive() {
                drawTable.showMessage("Downloading demos...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (String demo : demos) {
                            if (!download(demo)) {
                                return;
                            }
                        }
                        drawTable.showMessage("Downloaded demos.");
                    }
                }).start();
            }
        }.showDialog();
    }

    private int selectedItem;
    private String[] items;

    private boolean isSystemFile(String fileName) {
        return fileName.equals("ippotim.properties") || fileName.equals("ippotim.output");
    }

    abstract class FileOperation {
        abstract void operateFile();

        void selectFile() {
            File[] files = new File(homeDirectory).listFiles();
            LinkedList<String> list = new LinkedList<>();
            for (File file : files) {
                String name = file.getName();
                if (!isSystemFile(name)) {
                    list.add(name);
                }
            }
            if (0 == list.size()) {
                downloadDemos();
                return;
            }
            items = new String[list.size()];
            list.toArray(items);
            selectedItem = null == openedFile ? 0 : list.indexOf(openedFile.getName());
            InfoBox infoBox = new InfoBox(null, "Cancel", "OK", null) {
                @Override
                void onNegative() {

                }

                @Override
                void onPositive() {
                    openedFile = new File(homeDirectory + items[selectedItem]);
                    new InfoBox("Import \"" + openedFile.getName() + "\" ?", "Cancel", "OK", null) {
                        @Override
                        void onNegative() {

                        }

                        @Override
                        void onPositive() {
                            operateFile();
                        }
                    }.showDialog();
                }
            };
            infoBox.getAdb().setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    selectedItem = i;
                }
            });
            infoBox.showDialog();
        }
    }

    private File openedFile;
    static final String homeDirectory = "/storage/emulated/0/Ippotim/";

    private void viewManual() {
        Uri uri = Uri.parse(manual);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean initDirectoryFailed() {
        File file = new File(homeDirectory);
        if (!file.exists()) {
            TextView textView = new TextView(context);
            textView.setTextColor(Color.BLACK);
            String text = "萌新你好，欢迎进入Ippotim语言的世界！\n\n请你务必先大致看下我们在GitHub上的用户手册，点确定后将拉起浏览器打开该页面。\n\n如果你此时未联网，可以在联网后点主界面右上角的3个小白点，在弹出的菜单中选择最下面的Help菜单项，那里也有打开用户手册的入口。";
            textView.setText(text);
            new InfoBox(null, "别点我", "点这里", textView) {
                @Override
                void onNegative() {
                    drawTable.showMessage("点我旁边那货！");
                }

                @Override
                void onPositive() {
                    viewManual();
                    new InfoBox("要记得先看手册哦亲！", "好哒", "看过啦", null) {
                        @Override
                        void onNegative() {

                        }

                        @Override
                        void onPositive() {

                        }
                    }.showDialog();
                }
            }.showDialog(false, true);
            return !file.mkdir();
        }
        return false;
    }

    private void importFromFile() {
        if (drawTable.adapter.getCodeFromXml(openedFile)) {
            drawTable.showMessage("Imported \"" + openedFile.getName() + "\"");
        }
    }

    private void exportToFile() {
        if (drawTable.adapter.setCodeToXml(openedFile)) {
            drawTable.showMessage("Exported \"" + openedFile.getName() + "\"");
        }
    }

    private void deleteFile() {
        if (openedFile.delete()) {
            drawTable.showMessage("Deleted \"" + openedFile.getName() + "\"");
        }
    }

    private void clear() {
        openedFile = null;
        drawTable.adapter.clear();
        drawTable.doRepaint();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.Import:
                new FileOperation() {
                    @Override
                    void operateFile() {
                        importFromFile();
                        drawTable.doRepaint();
                    }
                }.selectFile();
                break;
            case R.id.Export:
                final EditText editText = new EditText(context);
                InfoBox infoBox = new InfoBox("Input a file name :", "Cancel", "OK", editText) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        String fileName = editText.getText().toString();
                        if (!fileName.equals("")) {
                            if (isSystemFile(fileName)) {
                                drawTable.showMessage("Can not export to a system file!");
                                return;
                            }
                            openedFile = new File(homeDirectory + fileName);
                            if (openedFile.exists()) {
                                new InfoBox("File \"" + openedFile.getName() + "\" exists, overwrite it?", "Cancel", "OK", null) {
                                    @Override
                                    void onNegative() {

                                    }

                                    @Override
                                    void onPositive() {
                                        exportToFile();
                                    }
                                }.showDialog();
                            } else {
                                exportToFile();
                            }
                        } else {
                            drawTable.showMessage("The name of file can not be empty!");
                        }
                    }
                };
                infoBox.showDialog();
                if (null != openedFile) {
                    editText.setText(openedFile.getName());
                }
                break;
            case R.id.Clear:
                new InfoBox("Close current project without saving?", "Cancel", "OK", null) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        clear();
                    }
                }.showDialog();
                break;
            case R.id.Delete:
                if (null != openedFile) {
                    new InfoBox("Delete \"" + openedFile.getName() + "\" ?", "Cancel", "OK", null) {
                        @Override
                        void onNegative() {

                        }

                        @Override
                        void onPositive() {
                            // 跟Clear功能有重叠似乎不够优雅，不过这也是没有办法的事。
                            deleteFile();
                            clear();
                        }
                    }.showDialog();
                } else {
                    drawTable.showMessage("Please import a project first!");
                }
                break;
            case R.id.Settings:
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                String[] defaultKeywords = drawTable.adapter.getDefaultKeywords();
                final String[] currentKeywords = drawTable.adapter.getCurrentKeywords();
                final EditText[] editTexts = new EditText[currentKeywords.length];
                for (int i = 0; i < currentKeywords.length; i++) {
                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setGravity(Gravity.CENTER);
                    TextView textView = new TextView(context);
                    String text = defaultKeywords[i] + " -> ";
                    textView.setText(text);
                    linearLayout.addView(textView);
                    editTexts[i] = new EditText(context);
                    editTexts[i].setSingleLine();
                    editTexts[i].setSelectAllOnFocus(true);
                    editTexts[i].setText(currentKeywords[i]);
                    linearLayout.addView(editTexts[i]);
                    layout.addView(linearLayout);
                }
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(layout);
//                LayoutInflater layoutInflater = LayoutInflater.from(this);
//                LinearLayout linearLayout = findViewById(R.id.keywords);
//                View view = layoutInflater.inflate(R.layout.keywords, linearLayout);
                // 原来Layout可以直接当View用
                new InfoBox(null, "Cancel", "OK", scrollView) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        LinkedList<String> linkedList = new LinkedList<>();
                        for (int i = 0; i < currentKeywords.length; i++) {
                            String keyword = editTexts[i].getText().toString().replace(" ", "");
                            if (!linkedList.contains(keyword)) {
                                linkedList.add(keyword);
                            }
                            currentKeywords[i] = keyword;
                        }
                        if (currentKeywords.length == linkedList.size()) {
                            final AlertDialog alertDialog = getAlertDialog();
                            new InfoBox("Make the keywords default?", "Cancel", "OK", null) {
                                @Override
                                void onNegative() {
                                    if (drawTable.adapter.setCurrentKeywords(currentKeywords, false)) {
                                        alertDialog.cancel();
                                    }
                                }

                                @Override
                                void onPositive() {
                                    if (drawTable.adapter.setCurrentKeywords(currentKeywords, true)) {
                                        alertDialog.cancel();
                                    }
                                }
                            }.showDialog();
                        } else {
                            drawTable.showMessage("A keyword must be different from the others!");
                        }
                    }
                }.showDialog(true, false);
                break;
            default:
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private Terminal terminal;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem[] menuItem = new MenuItem[8];

        menuItem[0] = menu.add(0, 0, 0, "Run");
        menuItem[0].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 之前是认为或者说猜测“加个或者说套个while判断是否清完也不行因为会直接运行下去故直接new一个这是最好也是最干净最优雅的突破或者说超越极限之类似误差出必然的方法，可能还没清完SurfaceView就又新建Kernel又启动了，这就导致Kernel运行结果的第一句被删除了，如果机器再卡一些估计还会删更多反正数据丢失怀疑到数据删除操作上是没错的，线程冲突那些也可能“，现在确认确实是跟线程有关，是因为Kernel线程启动的时候SurfaceView的paint可能还没有初始化，genCutStrings()返回的cutStrings[0]为“”，这就看起来像是被删除了
                terminal = new Terminal(context);
                terminal.adapter = drawTable.adapter;
                drawTable.terminal = terminal;
                InfoBox infoBox = new InfoBox(null, "Jump", "OK", terminal) {
                    @Override
                    void onPositive() {
                    }

                    @Override
                    void onNegative() {
                        if (terminal.running) {
                            drawTable.showMessage("Please wait for the program to finish.");
                            return;
                        }

                        final EditText editText = new EditText(context);
                        terminal.infoBox[0] = new InfoBox("0~" + terminal.getPagesCount() + " :", "Cancel", "OK", editText) {
                            @Override
                            void onNegative() {

                            }

                            @Override
                            void onPositive() {
                                try {
                                    terminal.jumpToPage(Integer.parseInt(editText.getText().toString()));
                                    getAlertDialog().cancel();
                                } catch (NumberFormatException e) {
                                    drawTable.showMessage("Not an integer!");
                                }
                            }
                        };
                        terminal.infoBox[0].showDialog(true, false);
                        editText.setText("0");
                    }
                };
                infoBox.showDialog(false, true);
                return true;
            }
        });

        menuItem[1] = menu.add(0, 0, 0, "Insert");
        menuItem[1].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.create("Member");
                } else {
                    drawTable.showMessage("Please select a statement first!");
                }
                return true;
            }
        });

        menuItem[2] = menu.add(0, 0, 0, "Modify");
        menuItem[2].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.create("Modify");
                } else {
                    drawTable.showMessage("Please select a statement first!");
                }
                return true;
            }
        });

        menuItem[3] = menu.add(0, 0, 0, "Copy");
        menuItem[3].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.adapter.copy();
                } else {
                    drawTable.showMessage("Please select a statement first!");
                }
                return true;
            }
        });

        menuItem[4] = menu.add(0, 0, 0, "Paste");
        menuItem[4].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.adapter.paste();
                    drawTable.doRepaint();
                } else {
                    drawTable.showMessage("Please select a statement first!");
                }
                return true;
            }
        });

        menuItem[5] = menu.add(0, 0, 0, "Remove");
        menuItem[5].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.adapter.remove();
                    drawTable.doRepaint();
                } else {
                    drawTable.showMessage("Please select a statement first!");
                }
                return true;
            }
        });

        menuItem[6] = menu.add(0, 0, 0, "Sort");
        menuItem[6].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final EditText editText = new EditText(context);
                InfoBox infoBox = new InfoBox("Input capacity :", "Cancel", "OK", editText) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        try {
                            int capacity = Integer.parseInt(editText.getText().toString());
                            getAlertDialog().cancel();
                            drawTable.adapter.sort(capacity);
                            drawTable.doRepaint();
                        } catch (NumberFormatException e) {
                            drawTable.showMessage("Not an integer!");
                        }
                    }
                };
                infoBox.showDialog(true, false);
                editText.setText("0");
                return true;
            }
        });

        menuItem[7] = menu.add(0, 0, 0, "Help");
        menuItem[7].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                TextView[] textViews = new TextView[2];
                String text;
                textViews[0] = new TextView(context);
                textViews[0].setTextColor(Color.BLACK);
                text = "Ippotim语言是一门类C语言，语法简明易懂，适合于启蒙教学与算法演示。\n\nIppotim系列软件是Ippotim语言的官方IDE，借鉴了UML的图形设计思想，旨在打造一个清新优雅的图形编程平台。\n\nIppotim项目已在GitHub上以GPL-3.0开源，同时提供有安卓版和桌面版。笔者暂时只实现了简单的解释运行功能，后续会添加智能编程辅助功能。\n";
                textViews[0].setText(text);
                textViews[1] = new TextView(context);
                textViews[1].setTextColor(Color.BLACK);
                text = "用户手册：" + manual;
                textViews[1].setAutoLinkMask(Linkify.ALL);
                textViews[1].setText(text);
                layout.addView(textViews[0]);
                layout.addView(textViews[1]);
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(layout);
                InfoBox infoBox = new InfoBox(null, "Cancel", "OK", scrollView) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        viewManual();
                    }
                };
                infoBox.showDialog();
                return true;
            }
        });

        return true;
    }

}
