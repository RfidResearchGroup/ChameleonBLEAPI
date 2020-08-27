package com.proxgrind.chameleon.defined;

/**
 * 在数据有变动时通知某个对象!
 */
public interface DataChangeListener<T> {
    void onChange(T y);
}