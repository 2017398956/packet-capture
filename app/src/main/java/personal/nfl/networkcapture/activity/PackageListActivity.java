package personal.nfl.networkcapture.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.adapter.ShowPackageAdapter;
import personal.nfl.networkcapture.bean.parcelable.PackageShowInfo;
import personal.nfl.networkcapture.common.widget.BaseActivity;

/**
 * app 列表界面
 *
 * @author nfl
 */

public class PackageListActivity extends BaseActivity {

    private static final String TAG = "PackageListActivity";
    public static final String SELECT_PACKAGE = "package_select";

    private ListView packageListView;
    private ShowPackageAdapter adapter;
    private List<PackageShowInfo> data = new ArrayList<>();
    private ProgressBar pg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_list);
        initView();
        initData();
        setListener();
        getPackagesInfo();
    }

    private void initView() {
        packageListView = findViewById(R.id.package_list);
        pg = findViewById(R.id.pg);
    }

    private void initData() {
        adapter = new ShowPackageAdapter(this, data);
        packageListView.setAdapter(adapter);
    }

    private void getPackagesInfo() {
        data.clear();
        data.addAll(PackageShowInfo.getPackageShowInfo());
        adapter.notifyDataSetChanged();
        pg.setVisibility(View.GONE);
        if(true){
            return;
        }
        // TODO 使用 Rxjava2 实现
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        data.clear();
                        data.addAll(PackageShowInfo.getPackageShowInfo());
                        emitter.onNext(1);
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        if (integer == 1) {
                            adapter.notifyDataSetChanged();
                            pg.setVisibility(View.GONE);
                        }
                    }
                }).subscribe();
    }

    private void setListener() {
        packageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                if (position != 0) {
                    intent.putExtra(SELECT_PACKAGE, data.get(position - 1));
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

}
