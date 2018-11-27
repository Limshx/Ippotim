package com.limshx.ippotim;

import Kernel.Adapter;
import Kernel.GraphicsOperations;
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
import android.widget.EditText;
import android.widget.Toast;

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

        this.setOnLongClickListener(new OnLongClickListener() {
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
                if (!doneScale) {
                    doneScale = true;
                }
                baseValue = 0;
                doLongClick = true;
                break;
            case MotionEvent.ACTION_DOWN:
                preX = motionEvent.getX();
                preY = motionEvent.getY();
                long currentClickTime = System.currentTimeMillis();
                if (currentClickTime - preClickTime < 300) {
                    create("Structure");
                } else {
                    adapter.setXY((int) preX, (int) preY);
                    adapter.click();
                    doRepaint();
                }
                preClickTime = currentClickTime;
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
        int px = getMeasuredWidth();
        int py = getMeasuredHeight();
        double scale = getScale();
        adapter = new Adapter(graphicsOperation, px / 2, py / 2, scale);
        Terminal.fontSize = (int) (24 / scale);
    }

    protected void onDraw(Canvas canvas) {
        this.canvas = canvas;
        if (null == adapter) {
            initKernel(this);
//            doRepaint();
        }

//        paint.setColor(Color.WHITE);
//        canvas.drawRect(0, 0, px, py, paint);
//        canvas.drawPaint(paint);
//        canvas.drawColor(Color.WHITE);
        adapter.paintEverything();
    }

    double getScale() {
        return Math.sqrt((1280 * 720d) / (displayMetrics.widthPixels * displayMetrics.heightPixels));
    }

    @Override
    public void create(final String type) { // 这里不能sleep()，不然会阻塞主线程卡死，getInput()没事是因为不是主线程
        String title;
        switch (type) {
            case "Function":
                title = "Input a function name :";
                break;
            case "Structure":
                title = "Input a structure name :";
                break;
            case "Member": // 一起判断
            case "Modify":
                title = "Input a sentence :";
                break;
            default:
                title = null;
                break;
        }
        InfoBox infoBox = new InfoBox(title, "Cancel", "OK", new EditText(context), context) {
            @Override
            void onNegative() {
                if (type.equals("Member")) {
                    adapter.insert("");
                }
            }
            @Override
            void onPositive() {
                String input = ((EditText) getView()).getText().toString();
                switch (type) {
                    case "Function":
                        adapter.createFunction(input);
                        break;
                    case "Structure":
                        adapter.createStructure(input);
                        break;
                    case "Member":
                        adapter.insert(input);
                        break;
                    case "Modify":
                        adapter.modify(input);
                    default:
                        break;
                }
                doRepaint();
            }
        };
        infoBox.showDialog();
        if (type.equals("Modify")) {
            ((EditText) infoBox.getView()).setText(adapter.getRectangleContent());
        }
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

    void doRepaint() {
        invalidate();
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        paint.setColor(Color.BLACK);
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
    public int getTextLength(String s, int fontSize) {
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
                InfoBox infoBox = new InfoBox("Input a value :", "Number", "String", new EditText(context), context) {
                    @Override
                    void onNegative() {
                        try {
                            input = Integer.parseInt(((EditText) getView()).getText().toString());
                            inputted = true;
                            getAlertDialog().cancel();
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Not an integer!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    void onPositive() {
                        input = ((EditText) getView()).getText().toString();
                        inputted = true;
                    }
                };
                infoBox.showDialog(false, true);
            }
        });
        waitForInput();
        return input;
    }
}
