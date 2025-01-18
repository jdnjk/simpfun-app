package cn.jdnjk.simpfun;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sp1, sp2, sp3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp2 = this.getSharedPreferences("token", this.MODE_PRIVATE);
        sp3 = this.getSharedPreferences("info", this.MODE_PRIVATE);

        // 获取存储的 token
        String authorizationToken = sp2.getString("token", null);  // 从 SharedPreferences 获取 token

        // 判断 token 是否存在且有效
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            // token 存在，进行授权验证
            verifyAuthorizationToken(authorizationToken);
        } else {
            // token 不存在或无效，跳转到登录页面
            navigateToLogin();
        }
    }

    private void verifyAuthorizationToken(String authorizationToken) {
        // 创建 OkHttpClient 实例
        OkHttpClient client = new OkHttpClient();

        // 设置请求头部带上 Authorization 信息
        Request request = new Request.Builder()
                .url("https://api.simpfun.cn/api/auth/info")
                .header("Authorization", authorizationToken)  // 使用从 SharedPreferences 获取的 token
                .build();

        // 异步请求验证授权信息
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败处理
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "请求失败，请稍后再试", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    // 判断返回的 code 是否为 200
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            int code = jsonObject.getInt("code");
                            if (code == 200) {
                                // 授权信息有效，保存用户信息
                                saveUserInfo(jsonObject.getJSONObject("info"));
                                navigateToHomePage();
                            } else if (code == 403) {
                                // 账号验证失败，跳转到登录页面
                                Toast.makeText(MainActivity.this, "账号验证失败，请重新登录", Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            } else {
                                // 其他错误，提示用户
                                Toast.makeText(MainActivity.this, "未知错误，请稍后再试", Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        }
                    });
                } else {
                    // 请求失败
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "网络错误，请稍后再试", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    });
                }
            }
        });
    }

    private void saveUserInfo(JSONObject userInfo) {
        // 保存用户信息到 SharedPreferences
        SharedPreferences.Editor editor = sp3.edit();
        try {
            editor.putInt("id", userInfo.getInt("id"));
            editor.putString("username", userInfo.getString("username"));
            editor.putInt("point", userInfo.getInt("point"));
            editor.putInt("diamond", userInfo.getInt("diamond"));
            editor.putLong("queue_time", userInfo.getLong("queue_time"));
            editor.putBoolean("verified", userInfo.getBoolean("verified"));
            editor.putBoolean("is_dev", userInfo.getBoolean("is_dev"));
            editor.putLong("create_time", userInfo.getLong("create_time"));
            editor.putLong("qq", userInfo.getLong("qq"));
            editor.putBoolean("is_pro", userInfo.getBoolean("is_pro"));
            editor.putBoolean("pro_valid", userInfo.getBoolean("pro_valid"));
            editor.putString("announcement_title", userInfo.getJSONObject("announcement").getString("title"));
            editor.putString("announcement_text", userInfo.getJSONObject("announcement").getString("text"));
            editor.apply();  // 使用 apply() 保存数据
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        // 跳转到登录页面
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 防止返回到 MainActivity
    }

    private void navigateToHomePage() {
        // 跳转到主页
        Intent intent = new Intent(MainActivity.this, Welcome.class);
        startActivity(intent);
        finish(); // 防止返回到 MainActivity
    }
}