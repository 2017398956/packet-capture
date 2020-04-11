package personal.nfl.networkcapture.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.common.widget.BaseActivity;

/**
 * 关于界面
 *
 * @author nfl
 */

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_about);
        setActionBarTitle(R.string.about_app);
    }
}
