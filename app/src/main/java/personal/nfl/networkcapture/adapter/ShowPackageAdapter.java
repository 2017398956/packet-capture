package personal.nfl.networkcapture.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.bean.parcelable.PackageShowInfo;

/**
 * @author nfl
 */
public class ShowPackageAdapter extends BaseAdapter {

    private Context context;
    private List<PackageShowInfo> data;
    private PackageManager packageManager;

    public ShowPackageAdapter(Context context, List<PackageShowInfo> data) {
        this.context = context;
        this.data = data;
        this.packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return data == null ? 1 : data.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.item_select_package, null);
            holder = new Holder(convertView);
        } else {
            holder = (Holder) convertView.getTag();
        }
        if (position == 0) {
            holder.appName.setText(R.string.all);
            holder.icon.setImageDrawable(null);
        } else {
            holder.appName.setText(data.get(position - 1).appName == null ? data.get(position - 1).packageName : data.get(position - 1).appName);
            holder.icon.setImageDrawable(data.get(position - 1).applicationInfo.loadIcon(packageManager));
        }
        return convertView;
    }

    private class Holder {
        TextView appName;
        ImageView icon;
        View baseView;

        public Holder(View view) {
            baseView = view;
            appName = view.findViewById(R.id.app_name);
            icon = view.findViewById(R.id.select_icon);
            view.setTag(this);
        }
    }
}