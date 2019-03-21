package com.limshx.ippotim;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.limshx.ippotim.kernel.Adapter;
import com.limshx.ippotim.kernel.GraphicsOperations;
import com.limshx.ippotim.kernel.StatementType;
import com.limshx.ippotim.kernel.TreeNode;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

public class DrawTable extends View implements GraphicsOperations {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    private Context context;
    private float baseValue = 0;
    private float preX, preY; // 上一次按下时的坐标值，用来判断是否想要长按
    private long preClickTime = 0;
    private boolean doLongClick = true;
    private boolean doneScale = true; // 避免双指开合缩放后由于手指离开先后不一致导致表格全体移动
    private Paint paint = new Paint();
    private Canvas canvas;
    Adapter adapter;

    public DrawTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!doLongClick) {
                    return false;
                }

                create("Function");
                return true;
            }
        });
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        //输入源为可显示的指针设备，如：mouse pointing device(鼠标指针),stylus pointing device(尖笔设备)
        //滚轮缩放很重要的，不然像Remix之类的桌面安卓就没法用了，然而桌面才是真正有生产力的
        if (0 != (motionEvent.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            // 处理滚轮事件
            if (motionEvent.getAction() == MotionEvent.ACTION_SCROLL) {//获得垂直坐标上的滚动方向,也就是滚轮向下滚
                int x, y;
                x = (int) motionEvent.getX();
                y = (int) motionEvent.getY();
                double scale = Math.pow(1.1, -motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL));
                adapter.doWheelRotation(x, y, scale);
                doRepaint();
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    public boolean performClick() {
        return super.performClick();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        performClick();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP:
                // 多点触控似乎所有手指都移开才判断为UP
                doneScale = true;
                doLongClick = true;
                baseValue = 0;
                break;
            case MotionEvent.ACTION_DOWN:
                preX = motionEvent.getX();
                preY = motionEvent.getY();
                adapter.click((int) preX, (int) preY);
                if (!adapter.hasSelectedTreeNode()) {
                    long currentClickTime = System.currentTimeMillis();
                    if (currentClickTime - preClickTime < 300) {
                        create("Structure");
                        return true;
                    }
                    preClickTime = currentClickTime;
                } else {
                    doLongClick = false;
                }
                doRepaint();
                break;
            case MotionEvent.ACTION_MOVE:
                float xv, yv;
                if (2 == motionEvent.getPointerCount()) {
                    doLongClick = false;
                    doneScale = false;
                    xv = motionEvent.getX(0) - motionEvent.getX(1);
                    yv = motionEvent.getY(0) - motionEvent.getY(1);
                    float value = (float) Math.sqrt(xv * xv + yv * yv);
                    if (baseValue == 0) {
                        baseValue = value;
                    } else {
                        if (Math.abs(value - baseValue) >= 0) {
                            int x = (int) ((motionEvent.getX(0) + motionEvent.getX(1)) / 2);
                            int y = (int) ((motionEvent.getY(0) + motionEvent.getY(1)) / 2);
                            double scale = baseValue / value;
                            adapter.doWheelRotation(x, y, scale);
                            doRepaint();
                            baseValue = value;
                        }
                    }
                } else {
                    xv = motionEvent.getX() - preX;
                    yv = motionEvent.getY() - preY;
                    if (Math.sqrt(xv * xv + yv * yv) > 10) { // 10作为阈值似乎是够了，之前不用判断应该是因为用的是1080p的手机，所以ACTION_MOVE不敏感，而到了720p就很敏感了之几乎无法触发长按事件
                        doLongClick = false;
                    }
                    if (doneScale) {
                        adapter.drag((int) motionEvent.getX(), (int) motionEvent.getY());
                        doRepaint();
                    }
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    private void initKernel(GraphicsOperations graphicsOperation) {
        double scale = getScale();
        adapter = new Adapter(graphicsOperation, getWidth(), getHeight(), scale, new File(MainActivity.homeDirectory + "ippotim.properties"));
        Terminal.fontSize = (int) (32 / scale);
    }

    boolean isScreenChanged = false;

    protected void onDraw(Canvas canvas) {
        this.canvas = canvas;
        if (null == adapter) {
            initKernel(this);
//            doRepaint();
        }
        if (isScreenChanged) {
            isScreenChanged = false;
            adapter.setScreen(getWidth(), getHeight());
        }
//        paint.setColor(Color.WHITE);
//        canvas.drawRect(0, 0, px, py, paint);
//        canvas.drawPaint(paint);
//        canvas.drawColor(Color.WHITE);
        adapter.paintEverything();
    }

    private double getScale() {
        return Math.sqrt((1280 * 720d) / (displayMetrics.widthPixels * displayMetrics.heightPixels));
    }

    private ArrayList<Button> buttonList;
    private LinkedList<EditText> editTextList;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private TreeNode treeNode;

    EditText getEditText() {
        EditText editText = new EditText(context);
        editText.setSingleLine();
        editText.setSelectAllOnFocus(true);
        return editText;
    }

    private TableRow getRow(String left, OnClickListener leftEvent, String right, OnClickListener rightEvent) {
        TableRow row = new TableRow(context);
        Button[] buttons = new Button[2];
        buttons[0] = new Button(context);
        buttons[1] = new Button(context);
        buttonList.add(buttons[0]);
        buttonList.add(buttons[1]);
        buttons[0].setText(left);
        buttons[0].setOnClickListener(leftEvent);
        buttons[1].setText(right);
        buttons[1].setOnClickListener(rightEvent);
        row.addView(buttons[0]);
        row.addView(buttons[1]);
        return row;
    }

    private void update(AlertDialog alertDialog, String statement, boolean insertOrModify) {
        alertDialog.cancel();
        if (insertOrModify) {
            adapter.insert(statement.trim());
        } else {
            adapter.modify(statement.trim());
        }
        doRepaint();
    }

    // infoBox.showDialog()后linearLayout.removeAllViews()再添加虽然可以不创建新的对话框直接刷新当前对话框，但点输入框无法弹出软键盘。可能进行某些设置可以恢复正常，但网上找了很久都无果或者说未果，故先这样处理，日后有更好的解决方案再说。
    private boolean isFirst = true; // 放到函数内的话因为内部类要用到，得是final，但final的话内部类就不能改动其值了只能调用，作为全局变量即可。
    private AlertDialog preDialog;
    private LinearLayout linearLayout;
    private void showFunctionCallDialog(final AlertDialog alertDialog, final Spinner spinner, final boolean insertOrModify) {
        if (null != preDialog) {
            preDialog.cancel();
            linearLayout.removeAllViews();
        }
        editTextList = new LinkedList<>();
        linearLayout = new LinearLayout(context);
        TextView textView = new TextView(context);
        textView.setText("{");
        linearLayout.addView(textView);
        linearLayout.addView(spinner);
        textView = new TextView(context);
        textView.setText("}");
        linearLayout.addView(textView);
        String data = (String) spinner.getSelectedItem();
        int functionParametersCount = adapter.getFunctionParametersCount(data);
        for (int i = 0; i < functionParametersCount; i++) {
            EditText editText = getEditText();
            if (!insertOrModify) {
                if (i < treeNode.elements.size() / 2) {
                    editText.setText(treeNode.elements.get(i + 1));
                }
            }
            linearLayout.addView(editText);
            editTextList.add(editText);
        }
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(context);
        horizontalScrollView.addView(linearLayout);
        InfoBox infoBox = new InfoBox(null, "取消", "确定", horizontalScrollView) {
            @Override
            void onNegative() {
                isFirst = true;
            }

            @Override
            void onPositive() {
                isFirst = true;
                StringBuilder stringBuilder = new StringBuilder("{" + spinner.getSelectedItem() + "}" + " ");
                for (EditText editText : editTextList) {
                    String parameter = editText.getText().toString().trim();
                    if (parameter.isEmpty()) {
                        showMessage("请补全参数！");
                        editText.requestFocus();
                        return;
                    }
                    stringBuilder.append(parameter).append(" ");
                }
                getAlertDialog().cancel();
                String statement = stringBuilder.toString();
                update(alertDialog, statement, insertOrModify);
            }
        };
        infoBox.showDialog(true, false);
        preDialog = infoBox.getAlertDialog();
    }

    private void create(final AlertDialog alertDialog, final String label, final boolean insertOrModify) {
        editTextList = new LinkedList<>();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView textView = new TextView(context);
        textView.setText(label);
        linearLayout.addView(textView);
        final EditText editText = getEditText();
        editTextList.add(editText);
        linearLayout.addView(editText);
        new InfoBox(null, "取消", "确定", linearLayout) {
            @Override
            void onNegative() {

            }

            @Override
            void onPositive() {
                String statement = label + " " + editText.getText();
                update(alertDialog, statement, insertOrModify);
            }
        }.showDialog();
    }

    private void create(final boolean insertOrModify) {
        if (!insertOrModify) {
            treeNode = adapter.getTreeNode();
            if (StatementType.HEAD == treeNode.statementType) {
                String content = treeNode.getContent();
                if (!content.isEmpty()) {
                    if (adapter.isStructure()) {
                        create("Structure");
                    } else {
                        create("Function");
                    }
                    editText.setText(content);
                } else {
                    showMessage("不能修改空白头语句！");
                }
                return;
            }
        }
        buttonList = new ArrayList<>();
        TableLayout rows = new TableLayout(context);
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(rows);
        adb.setView(scrollView);
        final AlertDialog alertDialog = adb.create();
        rows.setStretchAllColumns(true);
        rows.addView(getRow("定义", new OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextList = new LinkedList<>();
                Set<String> structures = adapter.getStructures();
                arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
                for (String structure : structures) {
                    arrayAdapter.add(structure);
                }
                spinner = new Spinner(context);
                spinner.setAdapter(arrayAdapter);
//                Dialog dialog = new Dialog(context);
//                dialog.setContentView(spinner);
//                dialog.show();
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                linearLayout.addView(spinner);
                final EditText editText = getEditText();
                editTextList.add(editText);
                linearLayout.addView(editText);
                HorizontalScrollView horizontalScrollView = new HorizontalScrollView(context);
                horizontalScrollView.addView(linearLayout);
                new InfoBox(null, "取消", "确定", horizontalScrollView) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        String statement = spinner.getSelectedItem() + " " + editText.getText();
                        update(alertDialog, statement, insertOrModify);
                    }
                }.showDialog();
            }
        }, "赋值", new OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextList = new LinkedList<>();
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                final EditText[] editTexts = new EditText[2];
                editTexts[0] = getEditText();
                editTexts[1] = getEditText();
                editTextList.add(editTexts[0]);
                editTextList.add(editTexts[1]);
                linearLayout.addView(editTexts[0]);
                TextView textView = new TextView(context);
                textView.setText("=");
                linearLayout.addView(textView);
                linearLayout.addView(editTexts[1]);
                new InfoBox(null, "取消", "确定", linearLayout) {
                    @Override
                    void onNegative() {

                    }

                    @Override
                    void onPositive() {
                        String statement = editTexts[0].getText() + " = " + editTexts[1].getText();
                        update(alertDialog, statement, insertOrModify);
                    }
                }.showDialog();
            }
        }));
        rows.addView(getRow("输入", new OnClickListener() {
            @Override
            public void onClick(View v) {
                create(alertDialog, adapter.getCurrentKeywords()[7], insertOrModify);
            }
        }, "输出", new OnClickListener() {
            @Override
            public void onClick(View v) {
                create(alertDialog, adapter.getCurrentKeywords()[8], insertOrModify);
            }
        }));
        rows.addView(getRow("如果", new OnClickListener() {
            @Override
            public void onClick(View v) {
                create(alertDialog, adapter.getCurrentKeywords()[1], insertOrModify);
            }
        }, "否则", new OnClickListener() {
            @Override
            public void onClick(View v) {
                String statement = adapter.getCurrentKeywords()[2];
                update(alertDialog, statement, insertOrModify);
            }
        }));
        rows.addView(getRow("循环", new OnClickListener() {
            @Override
            public void onClick(View v) {
                create(alertDialog, adapter.getCurrentKeywords()[3], insertOrModify);
            }
        }, "注释", new OnClickListener() {
            @Override
            public void onClick(View v) {
                create(alertDialog, "//", insertOrModify);
            }
        }));
        rows.addView(getRow("跳出", new OnClickListener() {
            @Override
            public void onClick(View v) {
                String statement = adapter.getCurrentKeywords()[4];
                update(alertDialog, statement, insertOrModify);
            }
        }, "继续", new OnClickListener() {
            @Override
            public void onClick(View v) {
                String statement = adapter.getCurrentKeywords()[5];
                update(alertDialog, statement, insertOrModify);
            }
        }));
        rows.addView(getRow("调用", new OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<String> functions = adapter.getFunctions();
                if (1 == functions.size()) {
                    showMessage("请先定义函数！");
                    return;
                }
                arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
                for (String function : functions) {
                    if (!function.isEmpty()) {
                        arrayAdapter.add(function);
                    }
                }
                spinner = new Spinner(context);
                spinner.setAdapter(arrayAdapter);
                showFunctionCallDialog(alertDialog, spinner, insertOrModify);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // Spinner显示时默认选中第1个或者说第0个，判断一下是否第一次选中解决重启对话框导致的闪烁问题。
                        if (!isFirst) {
                            showFunctionCallDialog(alertDialog, spinner, insertOrModify);
                        } else {
                            isFirst = false;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                });
            }
        }, "返回", new OnClickListener() {
            @Override
            public void onClick(View v) {
                String statement = adapter.getCurrentKeywords()[6];
                update(alertDialog, statement, insertOrModify);
            }
        }));
//        final String[] items = {"定义", "赋值", "输入", "输出", "如果", "否则", "循环", "跳出", "继续", "调用", "返回", "注释"};
//        adb.setItems(items, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                showMessage(items[which]);
//            }
//        });
//        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
//            @Override
//            public void onShow(DialogInterface dialogInterface) {
//                Window window = alertDialog.getWindow();
//                if (null != window) {
//                    WindowManager.LayoutParams params = window.getAttributes();
//                    params.width = tableRow.getWidth();
//                    alertDialog.getWindow().setAttributes(params);
//                }
//            }
//        });
        if (!adapter.canCreateElse() || !insertOrModify) {
            buttonList.get(5).setEnabled(false);
        }
        if (adapter.isStructure()) {
            for (int i = 0; i < buttonList.size(); i++) {
                if (i > 0) {
                    buttonList.get(i).setEnabled(false);
                }
            }
        }
        alertDialog.show();
        if (!insertOrModify) {
            modify(alertDialog);
        }
    }

    private void modify(AlertDialog alertDialog) {
        StatementType statementType = treeNode.statementType;
        if (null != statementType) {
            switch (statementType) {
                case ELSE: // 修改语句时else按钮虽然已经被禁用了，但似乎还是能performClick()
                case BREAK:
                case CONTINUE:
                case RETURN:
                    return;
            }
            String[] strings = treeNode.getContent().split(" ", 2);
            ArrayList<String> elements = treeNode.elements;
            buttonList.get(statementType.ordinal()).performClick();
            switch (statementType) {
                case DEFINE:
                    spinner.setSelection(arrayAdapter.getPosition(strings[0]));
                    editTextList.get(0).setText(strings[1]);
                    break;
                case ASSIGN:
                    editTextList.get(0).setText(elements.get(0));
                    editTextList.get(1).setText(elements.get(2));
                    break;
                case CALL:
                    spinner.setSelection(arrayAdapter.getPosition(elements.get(0)));
                    showFunctionCallDialog(alertDialog, spinner, false);
                    for (int i = 0; i < editTextList.size(); i++) {
                        editTextList.get(i).setText(elements.get(i + 1));
                    }
                    break;
                default:
                    if (2 == strings.length) {
                        editTextList.get(0).setText(strings[1]);
                    }
                    break;
            }
        }
    }

    private EditText editText;

    @Override
    public void create(final String type) { // 这里不能sleep()，不然会阻塞主线程卡死，getInput()没事是因为不是主线程
        String title;
        switch (type) {
            case "Function":
                title = "输入函数头：";
                break;
            case "Structure":
                title = "输入结构名：";
                break;
            case "Member":
                create(true);
                return;
            case "Modify":
                create(false);
                return;
            default:
                title = null;
                break;
        }
        editText = getEditText();
        InfoBox infoBox = new InfoBox(title, "取消", "确定", editText) {
            @Override
            void onNegative() {
            }

            @Override
            void onPositive() {
                String text = editText.getText().toString();
                // 去掉首尾空格符，这样就不可能调用到Ippotim语言里的main函数了。
                text = text.trim();
                if (!text.isEmpty()) {
                    switch (type) {
                        case "Function":
                            adapter.createFunction(text);
                            break;
                        case "Structure":
                            adapter.createStructure(text);
                            break;
                        default:
                            break;
                    }
                    doRepaint();
                }
            }
        };
        infoBox.showDialog();
//        if (type.equals("Modify")) {
//            editText.setText(adapter.getTreeNode());
//        } else {
//            editText.setText(text);
//        }
    }

    @Override
    public void showMessage(final String s) {
        post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void doRepaint() {
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(x, y, x + width, y + height, paint);
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(x, y, x + width, y + height, paint);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        paint.setColor(Color.BLACK);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void drawString(String str, int x, int y, int color) {
        paint.setColor(color);
        canvas.drawText(str, x, y, paint);
    }

    Terminal terminal;

    @Override
    public void appendText(String s) {
        if (terminal == null) { // 处理屏幕旋转后出现空指针异常的问题
            return;
        }
        terminal.getOutput(s);
    }

    @Override
    public int getPixelWidth(String s, int fontSize) {
//        Rect bounds = new Rect();
//        paint.getTextBounds(s, 0, s.length(), bounds);
//        return bounds.width();
        paint.setTextSize(fontSize);
        return (int) paint.measureText(s);
    }

    private boolean inputted;
    private Object input;

    private void waitForInput() {
        while (!inputted) {
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inputted = false;
    }

    @Override
    public Object getInput() {
        post(new Runnable() {
            @Override
            public void run() {
                final EditText editText = getEditText();
                terminal.infoBox[1] = new InfoBox("输入一个值：", "字符串", "整数", editText) {
                    @Override
                    void onNegative() {
                        input = editText.getText().toString();
                        if (!((String) input).contains("\"")) {
                            inputted = true;
                            getAlertDialog().cancel();
                        } else {
                            showMessage("禁止出现双引号！");
                        }
                    }

                    @Override
                    void onPositive() {
                        try {
                            input = Integer.parseInt((editText.getText().toString()));
                            inputted = true;
                            getAlertDialog().cancel();
                        } catch (NumberFormatException e) {
                            showMessage("请输入整数！");
                        }
                    }
                };
                terminal.infoBox[1].showDialog(false, false, true);
            }
        });
        waitForInput();
        terminal.infoBox[1] = null;
        return input;
    }
}
