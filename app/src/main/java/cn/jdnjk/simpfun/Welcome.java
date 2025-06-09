package cn.jdnjk.simpfun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 初始化 SharedPreferences
        sp3 = getSharedPreferences("info", MODE_PRIVATE);
        sp2 = getSharedPreferences("token", MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateToolbarInfo();

        // 初始化控件
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        listView = findViewById(R.id.list_view);

        // 设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);

        fetchData(); // 初始加载数据

        // 底部导航栏点击事件（目前仅保留 home）
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                return true;
            }
            return false;
        });
    }

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
        String[] menuOptions = {"打开浏览器", "充值", "注销"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("菜单");
        builder.setItems(menuOptions, (dialog, which) -> {
            if (which == 0) {
                String token = sp2.getString("token", "");
                String url = "https://simpfun.cn/auth?autologin=" + token;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else if (which == 1) {
                Intent intent = new Intent(Welcome.this, buypoint.class);
                startActivity(intent);
            } else if (which == 2) {
                SharedPreferences.Editor editor = sp2.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(Welcome.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        builder.create().show();
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
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(Welcome.this, "请求失败", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(Welcome.this, "请求失败，状态码：" + response.code(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                    return;
                }

                try {
                    String jsonResponse = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    int code = jsonObject.getInt("code");

                    if (code == 200) {
                        JSONArray list = jsonObject.getJSONArray("list");
                        List<JSONObject> serverList = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            serverList.add(list.getJSONObject(i));
                        }

                        runOnUiThread(() -> {
                            updateListView(serverList);
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(Welcome.this, "数据加载失败", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                } catch (Exception e) {
                    Log.e("JSON_PARSE", "解析失败", e);
                    runOnUiThread(() -> {
                        Toast.makeText(Welcome.this, "解析数据失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void updateListView(List<JSONObject> serverList) {
        ServerAdapter adapter = new ServerAdapter(this, serverList);
        listView.setAdapter(adapter);
    }
}