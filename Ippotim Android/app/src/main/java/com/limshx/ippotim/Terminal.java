package com.limshx.ippotim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;

import android.webkit.WebView;
import com.limshx.ippotim.kernel.Adapter;

public class Terminal extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    boolean running;
    private Paint paint;
    static int fontSize;
    private float gap;
    private int linesCount;
    private float preX, preY;
    private boolean turningPage;
    private int index;
    private boolean hasOutput;
    private float baseX = 0;
    private float newestChange;
    private boolean changedBaseX;
    private ArrayList<StringBuilder> list; // 把输出缓冲到rawList而不进行切分处理可以彻底解放内核，解决运行瓶颈。用ArrayList<StringBuilder>而不是ArrayList<String>会省去很多麻烦。
    private long preClickTime = 0;
    Adapter adapter;
    InfoBox[] infoBox = new InfoBox[2];

    Terminal(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        //holder.setFormat(PixelFormat.TRANSPARENT); // 顶层绘制SurfaceView设成透明
        setZOrderOnTop(true);
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 长按恢复infoBox[1]的对话框
                if (null != infoBox[1]) {
                    infoBox[1].getAlertDialog().show();
                }
                return true;
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initPaint();
        updateLinesCount();
        list = new ArrayList<>();
        list.add(new StringBuilder());
        new Thread(new Runnable() {
            @Override
            public void run() {
                running = true;
                while (running) {
                    if (hasOutput) {
                        hasOutput = false;
                        drawText(list.size());
                    }
                    // 必须睡一下，不然会卡一下才弹出包含SurfaceView的对话框
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                // 跑完再抢救一下，可能没到底
                drawText(list.size());
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                adapter.run();
                // 之前以为执行完这一句index应该是0，但实测却应该是最终arrayList的长度，于是以为index随时跟arrayList的长度保持更新之类似引用，其实是run()的过程中Terminal就在不断绘制输出数据，运行完得到了最终的arrayList才执行这一句，所以得到的就是最终的arrayList的size。
                index = list.size();
                // 在这里就知道跑完了，之前居然没想到，跑去看有没有新Output也是菜
                running = false;
            }
        }).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        updateLinesCount();
        index = list.size();
        drawText(index);

        // 屏幕旋转后刷新跳转输入对话框的表示跳转范围的标题。
        if (null != infoBox[0]) {
            infoBox[0].getAlertDialog().setTitle("0~" + getPagesCount() + " :");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // running很有必要，不然绘图守护线程无法停止
        running = false;
        adapter.stop();
    }

    public boolean performClick() {
        return super.performClick();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (running && null == infoBox[1]) {
            return false;
        }
        performClick();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP: {
                turningPage = false;
                if (changedBaseX) {
                    changedBaseX = false;
                    baseX += newestChange;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                preX = motionEvent.getX();
                preY = motionEvent.getY();
                long currentClickTime = System.currentTimeMillis();
                if (currentClickTime - preClickTime < 300) {
                    WebView webView = new WebView(getContext());
                    webView.loadUrl("file:///" + MainActivity.homeDirectory + "ippotim.output");
                    MainActivity.view(webView);
                }
                preClickTime = currentClickTime;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (turningPage) {
                    return false;
                }

                if (2 == motionEvent.getPointerCount()) {
                    changedBaseX = true;
                    float x = motionEvent.getX();
                    drawText(index, x - preX);
                } else {
                    if (!changedBaseX) {
                        float y = motionEvent.getY();
                        if (fontSize * 5 < y - preY || y - preY < -fontSize * 5) {
                            turningPage = true;
                            final int direction = y > preY ? -1 : 1;
                            index = index + direction * linesCount;
                            if (index - linesCount < 0) {
                                index = linesCount;
                            }
                            if (index > list.size()) {
                                index = list.size();
                            }
                            drawText(index);
                        }
                    }
                }
                break;
            }
        }
        // 这里直接返回true就无法触发长按事件了
        return super.onTouchEvent(motionEvent);
    }

    private void updateLinesCount() {
        linesCount = (int) (getHeight() / gap);
    }

    void getOutput(String s) {
        if (s.equals("\n")) {
            if (list.size() == Integer.MAX_VALUE) {
                list.remove(0);
            }
            list.add(new StringBuilder());
        } else {
            list.get(list.size() - 1).append(s);
        }
        hasOutput = true;
        // 被动式刷新得等绘制完才返回，有瓶颈，会很慢。
//        drawText(arrayList.size());
    }

    int getPagesCount() {
        int pagesCount = list.size() / linesCount;
        if (list.size() > linesCount * pagesCount) {
            pagesCount += 1;
        }
        return pagesCount > 0 ? pagesCount - 1 : 0;
    }

    void jumpToPage(int pageNo) {
        index = (pageNo + 1) * linesCount;
        if (index - linesCount < 0) {
            index = linesCount;
        }
        if (index > list.size()) {
            index = list.size();
        }
        drawText(index);
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        paint.setAntiAlias(true);
        paint.setTextSize(fontSize);
        // 本来直接用fontSize就可以了，但绘制emoji会有重叠，所以乘以1.16加大间隔。
        gap = fontSize * 1.16f;
    }

    private void drawText(int focusPoint) {
        drawText(focusPoint, 0);
    }

    private void drawText(int focusPoint, float x) {
        Canvas canvas = holder.lockCanvas(new Rect(0, 0, getWidth(), getHeight()));
        // 这句很必要，不然运行中关闭输出对话框就会空指针异常。
        if (null == canvas) {
            return;
        }
        canvas.drawColor(Color.WHITE);
        float y = gap;
        for (int i = focusPoint <= linesCount ? 0 : focusPoint - linesCount; i < focusPoint; i++) {
            canvas.drawText(list.get(i).toString(), baseX + x, y, paint);
            y += gap;
        }
        newestChange = x;
        holder.unlockCanvasAndPost(canvas);// 解锁画布，提交画好的图像
    }
}