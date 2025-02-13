package cn.jdnjk.simpfun;

import android.net.Uri;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    EditText name, pwd;
    Button btnlogin, btnreg;
    CheckBox agreeCheckbox;
    SharedPreferences sp1, sp2;

    // 定义 OkHttpClient
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(); // Handler 用于执行延迟操作

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        name = this.findViewById(R.id.name);            // 用户名输入框
        pwd = this.findViewById(R.id.pwd);              // 密码输入框
        btnlogin = this.findViewById(R.id.login);       // 登录按钮
        btnreg = this.findViewById(R.id.reg);           // 注册按钮
        agreeCheckbox = findViewById(R.id.agreeCheckbox);

        // 设置协议文本为可点击链接
        TextView textView = findViewById(R.id.agreeCheckbox);
        SpannableString spannableString = new SpannableString("我同意软件许可协议和简幻欢许可协议");

        // 设置 "软件许可协议" 可点击
        ClickableSpan softwareLicenseClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 打开软件许可协议的网页
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://127.0.0.1"));
                startActivity(intent);
            }
        };

        // 设置 "简幻欢许可协议" 可点击
        ClickableSpan simpfunLicenseClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 打开简幻欢许可协议的网页
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.yuque.com/simpfun/sfe/tos"));
                startActivity(intent);
            }
        };

        // 初始化 SharedPreferences
        sp2 = this.getSharedPreferences("token", this.MODE_PRIVATE);
        sp1 = this.getSharedPreferences("username", this.MODE_PRIVATE);

        // 从 SharedPreferences 中加载账号和密码
        name.setText(sp1.getString("usname", ""));
        pwd.setText(sp1.getString("uspwd", ""));
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        spannableString.setSpan(softwareLicenseClick, 4, 9, 0);  // "软件许可协议"
        spannableString.setSpan(simpfunLicenseClick, 11, 17, 0);  // "简幻欢许可协议"

        btnlogin.setOnClickListener(new View.OnClickListener() {                // 登录事件
            @Override
            public void onClick(View v) {
                String username = name.getText().toString().trim(); // 获取用户输入的用户名，去除前后空白
                String password = pwd.getText().toString().trim(); // 获取用户输入的密码，去除前后空白

                // 检查账号和密码是否为空
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "账号和密码不能为空！", Toast.LENGTH_SHORT).show();
                    return; // 直接返回，不执行后续逻辑
                }
                if (!agreeCheckbox.isChecked()) {
                    Toast.makeText(LoginActivity.this, "请先同意软件许可协议和简幻欢许可协议！", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 禁用按钮
                btnlogin.setEnabled(false);

                // 调用方法执行登录请求
                login(username, password);
            }
        });

        btnreg.setOnClickListener(new View.OnClickListener() {                  // 注册事件
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this, RegisterActivity.class);   // 跳转到注册页面
                startActivity(intent);
            }
        });
    }

    // 登录请求方法
    private void login(String username, String password) {
        String url = "https://api.simpfun.cn/api/auth/login"; // API URL
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        String json = "{\"username\":\"" + username + "\", \"passwd\":\"" + password + "\"}"; // JSON 数据

        RequestBody body = RequestBody.create(mediaType, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() { // 异步请求
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // 登录请求失败提示
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "登录请求失败！", Toast.LENGTH_LONG).show();
                    btnlogin.setEnabled(true); // 重新启用按钮
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 处理成功的响应
                    String responseData = response.body().string();
                    try {
                        // 解析响应的 JSON 数据
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code"); // 获取状态码
                        String msg = jsonResponse.getString("msg"); // 获取消息
                        String token = jsonResponse.optString("token"); // 获取 token

                        runOnUiThread(() -> {
                            if (code == 200) {
                                // 登录成功
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();

                                // 保存账号和密码到 SharedPreferences
                                SharedPreferences.Editor editor = sp1.edit();
                                editor.putString("usname", username);
                                editor.putString("uspwd", password);
                                editor.apply(); // 保存数据

                                saveToken(token);

                                // 跳转到 Welcome 页面
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                // 登录失败，显示返回的消息
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                            }

                            // 解决重复
                            handler.postDelayed(() -> btnlogin.setEnabled(true), 5000);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "解析错误！", Toast.LENGTH_SHORT).show();
                            btnlogin.setEnabled(true); // 解析错误时重新启用按钮
                        });
                    }
                } else {
                    // 非成功响应
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_LONG).show();
                        btnlogin.setEnabled(true); // 请求失败时重新启用按钮
                    });
                }
            }
        });
    }

    // 保存 token 的方法示例
    private void saveToken(String token) {
        SharedPreferences.Editor editor = sp2.edit();
        editor.putString("token", token);
        editor.apply(); // 使用 apply() 保存数据
    }
}