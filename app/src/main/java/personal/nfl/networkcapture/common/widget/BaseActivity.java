package personal.nfl.networkcapture.common.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.common.listener.BaseOnClickListener;
import personal.nfl.networkcapture.common.util.SoftKeyBoardTool;

public class BaseActivity extends FragmentActivity {

    protected Context context;
    private RelativeLayout app_bar;
    protected ImageView iv_back;
    protected TextView tv_title;
    protected ImageView iv_menu_01;
    protected LinearLayout ll_pad_container;
    protected LinearLayout ll_data_binding;
    private ViewStub vs_no_data = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
    }

    /**
     * 覆写为了 setContentView 的使用习惯不变
     *
     * @param layoutResID
     */
    public void setContentView(int layoutResID) {
        super.setContentView(R.layout.activity_base);
        initActionBarAndViewStub();
        View view = LayoutInflater.from(this).inflate(layoutResID, null);
        if (null != view) {
            ll_pad_container.addView(view);
        } else {
            // throw new CustomException("") ;
        }
    }

    public void setContentView(View view) {
        super.setContentView(R.layout.activity_base);
        initActionBarAndViewStub();
        if (null != view) {
            ll_pad_container.addView(view);
        }
    }

    private void initActionBarAndViewStub() {
        app_bar = findViewById(R.id.app_bar);
        ll_pad_container = findViewById(R.id.ll_pad_container);
        ll_data_binding = findViewById(R.id.ll_data_binding);
        iv_back = findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new BaseOnClickListener() {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                SoftKeyBoardTool.hideSoftKeyBoard(BaseActivity.this);
                finish();
            }
        });
        tv_title = findViewById(R.id.tv_title);
        iv_menu_01 = findViewById(R.id.iv_menu_01) ;
    }


    /**
     * must call after {@link #setContentView(int)}
     *
     * @param title
     */
    protected void setActionBarTitle(String title) {
        tv_title.setText(title);
    }

    /**
     * must call after {@link #setContentView(int)}
     *
     * @param strId
     */
    protected void setActionBarTitle(int strId) {
        tv_title.setText(getText(strId));
    }

    public String getActionBarTitle() {
        if (null == tv_title) {
            return "";
        }
        return tv_title.getText().toString();
    }

    public void setBackListener(BaseOnClickListener customOnClickListener) {
        iv_back.setOnClickListener(customOnClickListener);
    }

    public void showAppBar() {
        app_bar.setVisibility(View.VISIBLE);
    }

    public void hideAppBar() {
        app_bar.setVisibility(View.GONE);
    }

    public boolean isAppBarVisible() {
        return app_bar.getVisibility() == View.VISIBLE;
    }

    public void showNoDataView() {
        if (vs_no_data == null) {
            vs_no_data = findViewById(R.id.vs_no_data);
            vs_no_data.inflate();
        } else {
            vs_no_data.setVisibility(View.VISIBLE);
        }
    }

    public void hideNoDataView() {
        if (vs_no_data != null) {
            vs_no_data.setVisibility(View.GONE);
        }
    }

    public void changeNoDataView(List data) {
        if (null != data && data.size() > 0) {
            hideNoDataView();
        } else {
            showNoDataView();
        }
    }
}
