package cn.jdnjk.simpfun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput, confirmPasswordInput, inviteCodeInput; // 添加邀请码输入框
    private Button registerButton;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // 假设你有 activity_register.xml

        usernameInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        confirmPasswordInput = findViewById(R.id.confirm_password);
        inviteCodeInput = findViewById(R.id.invite_code);
        registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> attemptRegister());

        Button flatButton = findViewById(R.id.flatButton);
        flatButton.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegister() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String inviteCode = inviteCodeInput.getText().toString().trim(); // 获取邀请码

        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("请输入密码");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("两次输入密码不一致");
            return;
        }

        registerButton.setEnabled(false);
        sendRegistrationRequest(username, password, inviteCode); // 传递邀请码
    }

    private void sendRegistrationRequest(String username, String password, String inviteCode) {
        String url = "https://api.simpfun.cn/api/auth/register";

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("passwd", password);
            json.put("invite_code", inviteCode);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "构造请求失败", Toast.LENGTH_SHORT).show();
            registerButton.setEnabled(true);
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "注册失败，请检查网络", Toast.LENGTH_LONG).show();
                    registerButton.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject result = new JSONObject(response.body().string());
                        int code = result.getInt("code");
                        String msg = result.getString("msg");

                        String token = result.optString("token");
                        String username = usernameInput.getText().toString().trim();
                        String password = passwordInput.getText().toString().trim();

                        runOnUiThread(() -> {
                            if (code == 200) {
                                Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_SHORT).show();

                                SharedPreferences spToken = getSharedPreferences("token", MODE_PRIVATE);
                                SharedPreferences.Editor editorToken = spToken.edit();
                                editorToken.putString("token", token);
                                editorToken.apply();

                                SharedPreferences spUser = getSharedPreferences("username", MODE_PRIVATE);
                                SharedPreferences.Editor editorUser = spUser.edit();
                                editorUser.putString("usname", username);
                                editorUser.putString("uspwd", password);
                                editorUser.apply();

                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                            registerButton.setEnabled(true);
                        });

                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "解析响应失败", Toast.LENGTH_SHORT).show();
                            registerButton.setEnabled(true);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        try {
                            JSONObject errorResponse = new JSONObject(response.body().string());
                            String msg = errorResponse.optString("msg", "服务器返回错误");
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                        } catch (JSONException | IOException e) {
                            Toast.makeText(RegisterActivity.this, "服务器返回错误", Toast.LENGTH_SHORT).show();
                        }
                        registerButton.setEnabled(true);
                    });
                }
            }
        });
    }
}