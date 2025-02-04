package cn.jdnjk.simpfun;

import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar; // 修改为正确的导入
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Welcome extends AppCompatActivity {
    SharedPreferences sp3, sp2;
    Toolbar toolbar; // 确保使用 androidx.appcompat.widget.Toolbar
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);  // 设置布局

        // 获取 SharedPreferences，用于读取保存的用户信息
        sp3 = this.getSharedPreferences("info", MODE_PRIVATE);
        sp2 = this.getSharedPreferences("token", MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);  // 使用 androidx.appcompat.widget.Toolbar

        String username = sp3.getString("username", "NaN");  // 获取用户名
        updateToolbarInfo();

        // 绑定控件
        listView = findViewById(R.id.list_view);

        // 请求数据并绑定到 ListView
        fetchData();

        // 初始化底部导航栏
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 设置底部导航栏监听
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            // 处理导航栏点击事件
            if (item.getItemId() == R.id.nav_home) {
                return true; // 处理了此事件
            } else {
                return false; // 如果没有匹配的项，返回 false
            }
        });
    }

    // 更新顶栏信息的方法
    private void updateToolbarInfo() {
        String username = sp3.getString("username", "NaN");
        int points = sp3.getInt("point", 0);
        int diamonds = sp3.getInt("diamond", 0);
        int uid = sp3.getInt("uid", 0);

        String title = String.format("%s | 积分: %d | 钻石: %d | UID: %d", username, points, diamonds, uid);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    private void fetchData() {
        // 获取 token 用于请求头
        String token = sp2.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "未获取到 token，无法加载数据", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.simpfun.cn/api/ins/list")
                .header("Authorization", token)  // 使用从 SharedPreferences 获取的 token
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(Welcome.this, "请求失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(Welcome.this, "请求失败，状态码：" + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                // 解析 JSON 数据
                try {
                    String jsonResponse = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    int code = jsonObject.getInt("code");
                    if (code == 200) {
                        JSONArray list = jsonObject.getJSONArray("list");
                        List<String> data = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            String name = item.isNull("name") ? "未知" : item.getString("name");
                            String details = "ID: " + item.getInt("id") + ", CPU核心数: " + item.getString("cpu") + ", 内存: " + item.getString("ram") + "G" + ", 容量: " + item.getString("disk") + "GB";
                            data.add(details); // 将设备信息添加到列表中
                        }

                        // 更新 UI 线程
                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(Welcome.this, android.R.layout.simple_list_item_1, data);
                            listView.setAdapter(adapter);

                            // 设置列表项点击事件
                            listView.setOnItemClickListener((parent, view, position, id) -> {
                                String result = ((TextView) view).getText().toString();
                                Toast.makeText(Welcome.this, "您选择的设备是：" + result, Toast.LENGTH_LONG).show();
                            });
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(Welcome.this, "数据加载失败", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(Welcome.this, "解析数据失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}



