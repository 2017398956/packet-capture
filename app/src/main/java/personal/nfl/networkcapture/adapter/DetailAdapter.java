package personal.nfl.networkcapture.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import personal.nfl.networkcapture.R;
import personal.nfl.vpn.utils.SaveDataFileParser;

public class DetailAdapter extends BaseAdapter {

    private Context context;
    private List<SaveDataFileParser.ShowData> data;
    private int requestColor, responseColor;

    public DetailAdapter(Context context, List<SaveDataFileParser.ShowData> data) {
        this.context = context;
        this.data = data;
        requestColor = context.getResources().getColor(R.color.colorAccent);
        responseColor = context.getResources().getColor(R.color.colorPrimaryDark);
    }

    @Override
    public int getCount() {
        return data == null ? 0 : data.size();
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
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.item_conversation, null);
            holder = new Holder(convertView);
        } else {
            holder = (Holder) convertView.getTag();
        }
        SaveDataFileParser.ShowData showData = data.get(position);
        holder.bodyImage.setVisibility(showData.getBodyImage() == null ? View.GONE : View.VISIBLE);
        holder.bodyData.setVisibility((TextUtils.isEmpty(showData.getBodyStr())) ? View.GONE : View.VISIBLE);
        holder.itemView.setBackgroundResource(showData.isRequest() ? R.color.colorAccent_light : R.color.colorPrimaryDark_light);
        holder.headData.setTextColor(showData.isRequest() ? requestColor : responseColor);
        holder.bodyData.setTextColor(showData.isRequest() ? requestColor : responseColor);
        holder.headData.setText(showData.getHeadStr());
        holder.headTitle.setText(showData.isRequest() ? R.string.request_head : R.string.response_head);
        if (showData.getBodyStr() != null) {
            String showStr = " " + showData.getBodyStr();
            // TODO 如果数组太长了的话在部分手机中可能会报错
            holder.bodyData.setText(showStr);
        } else {
            holder.bodyData.setText("");
        }
        if (showData.getBodyImage() != null) {
            holder.bodyImage.setImageBitmap(showData.getBodyImage());
        }
        holder.bodyTitle.setVisibility((showData.isBodyNull() ? View.GONE : View.VISIBLE));
        holder.bodyTitle.setText(showData.isRequest() ? R.string.request_body : R.string.response_body);
        return convertView;
    }

    private class Holder {
        TextView headData;
        TextView bodyData;
        TextView bodyTitle;
        ImageView bodyImage;
        View itemView;
        TextView headTitle;

        public Holder(View view) {
            itemView = view.findViewById(R.id.container);
            headData = (TextView) view.findViewById(R.id.conversation_head_text);
            bodyData = view.findViewById(R.id.conversation_body_text);
            bodyImage = view.findViewById(R.id.conversation_body_im);
            bodyTitle = view.findViewById(R.id.body_title);
            headTitle = view.findViewById(R.id.head_title);
            view.setTag(this);
        }
    }
}