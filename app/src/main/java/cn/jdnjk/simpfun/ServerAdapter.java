package cn.jdnjk.simpfun;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class ServerAdapter extends ArrayAdapter<JSONObject> {
    SharedPreferences sp2;

    public ServerAdapter(Context context, List<JSONObject> servers) {
        super(context, 0, servers);
        sp2 = context.getSharedPreferences("token", Context.MODE_PRIVATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        JSONObject server = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_server_card, parent, false);
        }

        bindData(convertView, server);

        // 设置整个卡片的点击事件
        convertView.setOnClickListener(v -> {
            try {
                int deviceId = server.getInt("id");
                Intent intent = new Intent(getContext(), ServerManage.class);
                intent.putExtra("device_id", deviceId);
                getContext().startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "无法获取设备 ID", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        return convertView;
    }

    private void bindData(View view, JSONObject server) {
        try {
            String name;
            if (server.isNull("name")) {
                name = "未命名实例";
            } else {
                name = server.getString("name").trim();
                if (name.isEmpty()) {
                    name = "未命名实例";
                }
            }
            int id = server.getInt("id");
            String cpu = server.optString("cpu", "0");
            String ram = server.optString("ram", "0");
            String disk = server.optString("disk", "0");

            ((TextView) view.findViewById(R.id.tv_name)).setText(name);
            ((TextView) view.findViewById(R.id.tv_id)).setText("ID: " + id);
            ((TextView) view.findViewById(R.id.tv_config)).setText(
                    "CPU: " + cpu + "核 内存: " + ram + "G 硬盘: " + disk + "GB"
            );

            setupButtons(view, id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupButtons(View view, int deviceId) {
        Button btnActions = view.findViewById(R.id.btn_actions);
        btnActions.setOnClickListener(v -> showActionPopupMenu(v, deviceId));
    }

    private void showActionPopupMenu(View v, int deviceId) {
        Context context = v.getContext();
        PopupMenu popupMenu = new PopupMenu(context, v);

        popupMenu.inflate(R.menu.menu_server_actions); // 加载菜单资源

        popupMenu.setOnMenuItemClickListener(item -> {
            String baseUrl = "https://api.simpfun.cn/api/ins/"  + deviceId + "/power?action=";
            String action = "";

            if (item.getItemId() == R.id.action_start) {
                action = "start";
            } else if (item.getItemId() == R.id.action_restart) {
                action = "restart";
            } else if (item.getItemId() == R.id.action_stop) {
                action = "stop";
            } else if (item.getItemId() == R.id.action_force_stop) {
                action = "kill";
            } else {
                return false;
            }

            sendPowerRequest(baseUrl + action);
            return true;
        });

        popupMenu.show();
    }
    private void sendPowerRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", sp2.getString("token", ""))
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) {
            }
        });
    }
}