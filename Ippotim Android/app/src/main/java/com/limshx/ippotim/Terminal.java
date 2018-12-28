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
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import com.limshx.ippotim.kernel.Adapter;

public class Terminal extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    boolean running;
    private Paint paint;
    static int fontSize;
    private float gap;
    private int linesCount;
    private float preX, preY;
    private boolean readyForGo = true;
    private int index;
    private boolean hasOutput;
    private float baseX = 0;
    private float newestChange;
    private boolean changedBaseX = false;
    private boolean hasSoftWrap;
    private ArrayList<StringBuilder> list; // 把输出缓冲到rawList而不进行切分处理可以彻底解放内核，解决运行瓶颈。用ArrayList<StringBuilder>而不是ArrayList<String>会省去很多麻烦。
    private long preClickTime = 0;
    private Context context;
    private TextView textView;
    Adapter adapter;
    InfoBox[] infoBox = new InfoBox[2];

    public Terminal(Context context) {
        super(context);
        this.context = context;
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

        if (null != textView) {
            updateTextView();
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
                readyForGo = true;
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
                    textView = new TextView(context);
                    textView.setTextIsSelectable(true);
                    // textView.setTextColor(Color.BLACK);
                    // textView.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
                    // textView.setTextSize(fontSize);
                    // 装ScrollView里可以滚得更快或者说快得多
                    ScrollView scrollView = new ScrollView(context);
                    scrollView.addView(textView);
                    updateTextView();
                    new InfoBox(null, "Cancel", "OK", scrollView, context) {
                        @Override
                        void onNegative() {

                        }

                        @Override
                        void onPositive() {

                        }
                    }.showDialog();
                }
                preClickTime = currentClickTime;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!readyForGo) {
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
                            readyForGo = false;
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

    private void updateTextView() {
        textView.setText("");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < linesCount; i++) {
            int focusPoint = i + index - linesCount;
            if (focusPoint >= 0) {
                stringBuilder.append(list.get(focusPoint));
                stringBuilder.append("\n");
            }
        }
        textView.append(stringBuilder.deleteCharAt(stringBuilder.length() - 1));
    }

    private void updateLinesCount() {
        linesCount = (int) (getHeight() / gap);
    }

    private boolean hasEmoji(String s) {
        // D83D是emoji的首个双字节，十进制是55357，避免或者说防止emoji被截断显示为两个乱码
        // char是占两个字节，中文可以用两个字节表示，不怕变乱码，或者说想变乱码都难
        return s.charAt(s.length() - 1) == 0xD83D;
    }

    void getOutput(String s) {
        if (s.equals("\n")) {
            // 如果刚因为缓存满了自动换行了又来了个换行符，则直接返回
            if (hasSoftWrap) {
                hasSoftWrap = false;
                return;
            }
            if (list.size() == Integer.MAX_VALUE) {
                list.remove(0);
            }
            list.add(new StringBuilder());
        } else {
            int cachedStringLength = list.get(list.size() - 1).length();
            // s是单次output输出者，是人工一个个放上去的，一般不会太大，所以缓存大小设1000认为是比较合理的，真的很大那也没办法了，或许可以让用户自己定，不过应该也不会有人有这种需求，毕竟是教学语言。实际情况是1000是可以接受的，左右平移和TextView都不卡，那就先酱紫。
            int maxCachedStringLength = 1000;
            if (maxCachedStringLength <= cachedStringLength + s.length()) {
                hasSoftWrap = true;
                int freeSpace = maxCachedStringLength - cachedStringLength;
                // 适配表情符号之双字节患者
                if (hasEmoji(s.substring(0, freeSpace))) {
                    freeSpace += 1;
                }
                list.get(list.size() - 1).append(s, 0, freeSpace);
                list.add(new StringBuilder());
                String remainingString = s.substring(freeSpace);
                if (!remainingString.equals("")) {
                    getOutput(remainingString);
                }
            } else {
                if (hasSoftWrap) {
                    hasSoftWrap = false;
                }
                list.get(list.size() - 1).append(s);
            }
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