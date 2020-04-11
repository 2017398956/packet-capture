package personal.nfl.networkcapture.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.activity.ConnectionListActivity;
import personal.nfl.networkcapture.common.listener.BaseOnClickListener;
import personal.nfl.vpn.VPNConstants;

/**
 * 历史抓包记录
 *
 * @author nfl
 */
public class HistoryListAdapter extends RecyclerView.Adapter {

    private Context context;
    private List<String> data;

    public HistoryListAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = View.inflate(context, R.layout.item_select_date, null);
        return new CommonHolder(inflate);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((CommonHolder) holder).date.setText(data.get(position));
        holder.itemView.setOnClickListener(new BaseOnClickListener() {
            @Override
            public void onClick(View v) {
                String fileDir = VPNConstants.CONFIG_DIR + data.get(position);
                ConnectionListActivity.openActivity(context, fileDir);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    private class CommonHolder extends RecyclerView.ViewHolder {
        TextView date;

        public CommonHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
        }
    }
}
