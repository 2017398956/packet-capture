package personal.nfl.networkcapture.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.networkcapture.common.util.StringUtil;
import personal.nfl.vpn.nat.NatSession;
import personal.nfl.vpn.processparse.AppInfo;
import personal.nfl.vpn.utils.TimeFormatUtil;

/**
 * 所抓 app 列表的适配器
 *
 * @author nfl
 */

public class ConnectionAdapter extends BaseAdapter {

    private final Context context;
    private List<NatSession> netConnections;
    private Drawable defaultDrawable;

    public ConnectionAdapter(Context context, List<NatSession> netConnections) {
        this.context = context;
        this.netConnections = netConnections;
        this.defaultDrawable = context.getResources().getDrawable(R.drawable.sym_def_app_icon);
    }

    @Override
    public int getCount() {
        return netConnections == null ? 0 : netConnections.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.item_connection, null);
            holder = new Holder(convertView);
        } else {
            holder = (Holder) convertView.getTag();
        }
        NatSession natSession = netConnections.get(position);
        holder.tv_app_name.setText(natSession.getAppInfo() != null ? natSession.getAppInfo().leaderAppName : context.getString(R.string.unknow));
        holder.iv_app_icon.setImageDrawable(natSession.getAppInfo() != null && natSession.getAppInfo().pkgs != null ?
                AppInfo.getIcon(context, natSession.getAppInfo().pkgs.getAt(0)) : defaultDrawable);
        holder.tv_url.setText(null);
        boolean isTcp = NatSession.TCP.equals(natSession.getType());
        holder.tv_ssl.setVisibility(isTcp && natSession.isHttpsSession ? View.VISIBLE : View.INVISIBLE);
        holder.tv_url.setText(isTcp ?
                (TextUtils.isEmpty(natSession.getRequestUrl()) ? natSession.getRemoteHost() : natSession.getRequestUrl())
                : null);
        holder.tv_url.setVisibility(holder.tv_url.getText().length() > 0 ? View.VISIBLE : View.INVISIBLE);
        holder.tv_net_state.setText(natSession.getIpAndPort());
        holder.tv_capture_time.setText(TimeFormatUtil.formatHHMMSSMM2(natSession.getRefreshTime()));
        holder.tv_net_size.setText(StringUtil.getSocketSize(natSession.bytesSent + natSession.getReceiveByteNum()));
        return convertView;
    }

    private class Holder {
        ImageView iv_app_icon;
        TextView tv_app_name;
        TextView tv_net_state;
        TextView tv_url;
        TextView tv_capture_time;
        TextView tv_net_size;
        TextView tv_ssl;

        Holder(View view) {
            iv_app_icon = view.findViewById(R.id.iv_app_icon);
            tv_app_name = view.findViewById(R.id.tv_app_name);
            tv_net_state = view.findViewById(R.id.tv_net_state);
            tv_url = view.findViewById(R.id.tv_url);
            tv_capture_time = view.findViewById(R.id.tv_capture_time);
            tv_net_size = view.findViewById(R.id.tv_net_size);
            tv_ssl = view.findViewById(R.id.tv_ssl);
            view.setTag(this);
        }
    }

}
