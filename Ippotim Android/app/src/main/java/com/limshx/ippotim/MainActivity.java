package com.limshx.ippotim;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Context context; // View里的Context就是Activity里的this，所以不用从View里静态过来用
    private DrawTable drawTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // API level 23以上需要申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            if (initDirectoryFailed()) {
                Toast.makeText(this, "Create path failed!", Toast.LENGTH_SHORT).show();
            }
        }

        context = this;
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

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) { // 屏幕旋转时调用，没有需要添加的操作则不需要覆盖。
//        super.onConfigurationChanged(newConfig);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (initDirectoryFailed()) {
                        Toast.makeText(this, "Create path failed!", Toast.LENGTH_SHORT).show();
                    }
                }
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

    private int selectedItem;

    abstract class FileOperation {
        abstract void operateFile();

        void selectFile() {
            File[] files = new File(homeDirectory).listFiles();

            if (files.length == 0) {
                Toast.makeText(context, "There are no projects.", Toast.LENGTH_SHORT).show();
                return;
            }

            final String[] items = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                items[i] = files[i].getName();
            }
            selectedItem = 0;
            InfoBox infoBox = new InfoBox(null, "OK", "Cancel", null, context) {
                @Override
                void onPositive() {
                    openedFile = new File(homeDirectory + items[selectedItem]);
                    new InfoBox("Import \"" + openedFile.getName() + "\" ?", "OK", "Cancel", null, context) {
                        @Override
                        void onPositive() {
                            operateFile();
                        }

                        @Override
                        void onNegative() {

                        }
                    }.showDialog();
                }

                @Override
                void onNegative() {

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
    private final String homeDirectory = "/storage/emulated/0/Ippotim/";

    private boolean initDirectoryFailed() {
        File file = new File(homeDirectory);
        return !file.exists() && !file.mkdir();
    }

    private void importFromFile() {
        if (drawTable.adapter.getCodeFromXml(openedFile)) {
            Toast.makeText(this, "Imported \"" + openedFile.getName() + "\"", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToFile() {
        if (drawTable.adapter.setCodeToXml(openedFile)) {
            Toast.makeText(this, "Exported \"" + openedFile.getName() + "\"", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile() {
        if (openedFile.delete()) {
            Toast.makeText(context, "Deleted \"" + openedFile.getName() + "\"", Toast.LENGTH_SHORT).show();
        }
    }

    private void clear() {
        openedFile = null;
        drawTable.adapter.init(drawTable.getMeasuredWidth() / 2, drawTable.getMeasuredHeight() / 2, drawTable.getScale());
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
                InfoBox infoBox = new InfoBox("Input a file name :", "OK", "Cancel", new EditText(context), context) {
                    @Override
                    void onPositive() {
                        String fileName = ((EditText) getView()).getText().toString();
                        if (!fileName.equals("")) {
                            openedFile = new File(homeDirectory + fileName);
                            if (openedFile.exists()) {
                                new InfoBox("File \"" + openedFile.getName() + "\" exists, overwrite it?", "OK", "Cancel", null, context) {
                                    @Override
                                    void onPositive() {
                                        exportToFile();
                                    }

                                    @Override
                                    void onNegative() {

                                    }
                                }.showDialog();
                            } else {
                                exportToFile();
                            }
                        } else {
                            Toast.makeText(context, "The name of file can not be empty!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    void onNegative() {

                    }
                };
                infoBox.showDialog();
                if (openedFile != null) {
                    ((EditText) infoBox.getView()).setText(openedFile.getName());
                }
                break;
            case R.id.Clear:
                new InfoBox("Close current project without saving?", "OK", "Cancel", null, context) {
                    @Override
                    void onPositive() {
                        clear();
                    }

                    @Override
                    void onNegative() {

                    }
                }.showDialog();
                break;
            case R.id.Delete:
                if (null != openedFile) {
                    new InfoBox("Delete \"" + openedFile.getName() + "\" ?", "OK", "Cancel", null, context) {
                        @Override
                        void onPositive() {
                            // 跟Clear功能有重叠似乎不够优雅，不过这也是没有办法的事。
                            deleteFile();
                            clear();
                        }

                        @Override
                        void onNegative() {

                        }
                    }.showDialog();
                } else {
                    Toast.makeText(context, "Please import a project first!", Toast.LENGTH_SHORT).show();
                }
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
        MenuItem menuItem[] = new MenuItem[4];

        menuItem[0] = menu.add(0, 0, 0, "Run");
        menuItem[0].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 之前是认为或者说猜测“加个或者说套个while判断是否清完也不行因为会直接运行下去故直接new一个这是最好也是最干净最优雅的突破或者说超越极限之类似误差出必然的方法，可能还没清完SurfaceView就又新建Kernel又启动了，这就导致Kernel运行结果的第一句被删除了，如果机器再卡一些估计还会删更多反正数据丢失怀疑到数据删除操作上是没错的，线程冲突那些也可能“，现在确认确实是跟线程有关，是因为Kernel线程启动的时候SurfaceView的paint可能还没有初始化，genCutStrings()返回的cutStrings[0]为“”，这就看起来像是被删除了
                terminal = new Terminal(context);
                terminal.adapter = drawTable.adapter;
                drawTable.terminal = terminal;
                InfoBox infoBox = new InfoBox(null, "OK", "Jump", terminal, context) {
                    @Override
                    void onPositive() {
                    }

                    @Override
                    void onNegative() {
                        if (terminal.running) {
                            Toast.makeText(context, "Please wait for the program to finish.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        terminal.infoBox = new InfoBox("0~" + terminal.getPagesCount() + " :", "OK", "Cancel", new EditText(context), context) {
                            @Override
                            void onPositive() {
                                try {
                                    terminal.jumpToPage(Integer.parseInt(((EditText) getView()).getText().toString()));
                                } catch (NumberFormatException e) {
                                    Toast.makeText(context, "Please input an integer.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            void onNegative() {

                            }
                        };
                        terminal.infoBox.showDialog();
                    }
                };
                infoBox.showDialog(false);
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
                    Toast.makeText(context, "Please select a rectangle first!", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(context, "Please select a rectangle first!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        menuItem[3] = menu.add(0, 0, 0, "Remove");
        menuItem[3].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (drawTable.adapter.hasSelectedTreeNode()) {
                    drawTable.adapter.delete();
                    drawTable.doRepaint();
                } else {
                    Toast.makeText(context, "Please select a rectangle first!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        return true;
    }

}
