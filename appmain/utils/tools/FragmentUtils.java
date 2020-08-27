package com.proxgrind.chameleon.utils.tools;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.proxgrind.chameleon.R;

import java.util.List;

/*
 * 一些碎片开发使用上的工具!
 * */
public class FragmentUtils {

    //隐藏所有的碎片!
    public static void hides(FragmentManager manager, Fragment exclude) {
        List<Fragment> fragmentList = manager.getFragments();
        //迭代隐藏所有的碎片!
        for (Fragment fragment : fragmentList)
            //排除指定的碎片!
            if (fragment != exclude)
                manager.beginTransaction().hide(fragment).commitAllowingStateLoss();
    }

    public static void showAndAddBackStack(FragmentActivity activity, Fragment fragment) {
        if (activity == null || fragment == null) return;
        FragmentManager manager = activity.getSupportFragmentManager();
        manager.beginTransaction()
                .add(R.id.mainContainer, fragment)
                .addToBackStack(fragment.getTag()).commit();
    }

    public static void runOnUiThread(Activity activity, Runnable runnable) {
        if (activity != null)
            activity.runOnUiThread(runnable);
    }
}
