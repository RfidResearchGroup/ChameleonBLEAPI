package com.proxgrind.chameleon.callback;

public interface BaseCallback {

    interface ErrorCallback<T> {
        void onError(T e);
    }
}
