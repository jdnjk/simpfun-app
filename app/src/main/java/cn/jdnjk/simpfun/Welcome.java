package cn.jdnjk.simpfun;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Welcome extends AppCompatActivity {

    SharedPreferences sp3, sp2;
    Toolbar toolbar;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        sp3 = this.getSharedPreferences("info", MODE_PRIVATE);
        sp2 = this.getSharedPreferences("token", MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateToolbarInfo();
        listView = findViewById(R.id.list_view);
        fetchData();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            // 处理导航栏点击事件
            if (item.getItemId() == R.id.nav_home) {
                return true;
            } else {
                return false;
            }
        });
    }

    // 更新顶栏信息的方法
    private void updateToolbarInfo() {
        String username = sp3.getString("username", "NaN");
        int uid = sp3.getInt("uid", 0);

        String title = String.format("%s | UID: %d", username, uid);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            showMenuDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void showMenuDialog() {
        String[] menuOptions = {
                "打开浏览器",
                "充值",
                "注销",
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("菜单");
        builder.setItems(menuOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 处理菜单选项点击事件
                if (which == 0) {
                    String token = sp2.getString("token", "");
                    String url = "https://simpfun.cn/auth?autologin=" + token;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else if (which == 1) {
                    Intent intent = new Intent(Welcome.this, buypoint.class);
                    startActivity(intent);
                } else if (which == 2) {
                    String token = sp2.getString("token", "");
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder() //完全注销登录
                            .url("https://api.simpfun.cn/api/logout")
                            .header("Authorization", token)
                            .build();
                    Intent intent = new Intent(Welcome.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

private void fetchData() {
        String token = sp2.getString("token", "");
        if (token.isEmpty()) {
            Toast.makeText(this, "未获取到 token，无法加载数据", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.simpfun.cn/api/ins/list")
                .header("Authorization", token)
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
                            data.add(details);
                        }


                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(Welcome.this, android.R.layout.simple_list_item_1, data);
                            listView.setAdapter(adapter);

                            // 设置列表项点击事件
                            listView.setOnItemClickListener((parent, view, position, id) -> {
                                String result = ((TextView) view).getText().toString();

                                int deviceId = Integer.parseInt(result.split(",")[0].split(":")[1].trim());

                                Intent intent = new Intent(Welcome.this, ServerManage.class);
                                intent.putExtra("device_id", deviceId);
                                startActivity(intent);
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
