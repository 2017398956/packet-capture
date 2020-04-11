package personal.nfl.networkcapture.common.widget;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment 基类
 * @author nfl
 */

public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(getLayout(), container, false);
        return layout;
    }

    /**
     * @return 返回 fragment 所需的 layout id 。
     */
    protected abstract int getLayout();

    public void onVisible() {

    }

    public void onInVisible() {

    }
}
