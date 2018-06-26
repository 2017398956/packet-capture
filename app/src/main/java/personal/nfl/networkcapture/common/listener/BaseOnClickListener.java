package personal.nfl.networkcapture.common.listener;

import android.view.View;

/**
 * Created by fuli.niu on 2016/8/31.
 */
public class BaseOnClickListener<T> implements View.OnClickListener {

    private T t;

    public BaseOnClickListener() {

    }

    public BaseOnClickListener(T t) {
        this.t = t;
    }

    @Override
    public void onClick(View v) {
        // TraceKeeper.addTrace(v);
        this.onClick(v, t);
    }

    public void onClick(View view, T t) {
    }

    public void setT(T t) {
        this.t = t;
    }

    public T getT() {
        return t;
    }
}

