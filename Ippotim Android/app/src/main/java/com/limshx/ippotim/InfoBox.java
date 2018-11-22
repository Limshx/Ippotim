package com.limshx.ippotim;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

abstract class InfoBox {  // 当初这个是放在MainActivity里，想让DrawTable共用，鬼使神差地以为这个必须定义为static，可是Context和EditText这些不能static，这样就陷入了一个死局。其实内部类直接A.B这样调用即可，不需要定义为static，当然像现在这样直接提取出来也算是突破极限破了之前的死局。
    private String title;
    private String positive;
    private String negative;
    private View view;
    private AlertDialog.Builder adb;
    private AlertDialog alertDialog;

    abstract void onPositive();

    abstract void onNegative();

    InfoBox(String title, String positive, String negative, View view, Context context) {
        this.title = title;
        this.positive = positive;
        this.negative = negative;
        this.view = view;
        adb = new AlertDialog.Builder(context);
    }

    View getView() {
        return view;
    }

    AlertDialog.Builder getAdb() {
        return adb;
    }

    AlertDialog getAlertDialog() {
        return alertDialog;
    }

    void showDialog() {
        showDialog(true);
    }

    void showDialog(final boolean negativeCancel) {
        View.OnClickListener[] onClickListener = new View.OnClickListener[2];
        onClickListener[0] = new View.OnClickListener() {
            @Override
            public void onClick(View view) { // OnClickListener这种其实就是或者说相当于函数指针，只不过使用对象的形式传递，或者说这就是java中函数指针的实现方式之一
                onPositive(); // 这就是函数指针
                alertDialog.cancel();
            }
        };

        onClickListener[1] = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNegative();
                if (negativeCancel) {
                    alertDialog.cancel();
                }
            }
        };

        adb.setView(view).setTitle(title).setPositiveButton(positive, null).setNegativeButton(negative, null);
        alertDialog = adb.show();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(onClickListener[0]);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(onClickListener[1]);
    }
}