package com.proxgrind.chameleon.utils.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;

public class ViewUtils {
    //加载layout文件并且返回view引用
    public static View inflate(Context context, int layID) {
        View v = LayoutInflater.from(context).inflate(layID, null);

        return v;
    }

    public static void measureUnspecified(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }

    // ScrollView自动滑动到底部!
    public static void fullScroll(ScrollView scrollView) {
        scrollView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollView.post(new Runnable() {
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                });
    }

    //给编辑器请求焦点和虚拟键盘
    public static void requestFocusAndShowInputMethod(EditText edt) {
        if (edt == null) return;
        edt.post(new Runnable() {
            @Override
            public void run() {
                edt.setFocusable(true);
                edt.setFocusableInTouchMode(true);
                edt.requestFocus();
                InputMethodManager imm = (InputMethodManager) edt.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
    }
}
