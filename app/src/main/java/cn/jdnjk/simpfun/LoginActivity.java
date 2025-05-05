package cn.jdnjk.simpfun;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.InputType;
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
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    EditText name, pwd;
    Button btnlogin, btnreg;
    CheckBox agreeCheckbox;
    SharedPreferences sp1, sp2;

    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        name = this.findViewById(R.id.name);
        pwd = this.findViewById(R.id.pwd);
        btnlogin = this.findViewById(R.id.login);
        btnreg = this.findViewById(R.id.reg);
        agreeCheckbox = findViewById(R.id.agreeCheckbox);

        // 无框跳过按钮
        Button flatButton = findViewById(R.id.flatButton);
        flatButton.setOnClickListener(v -> showTokenInputDialog());

        TextView textView = findViewById(R.id.agreeCheckbox);
        SpannableString spannableString = new SpannableString("我同意软件许可协议和简幻欢许可协议");

        ClickableSpan softwareLicenseClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://127.0.0.1"));
                startActivity(intent);
            }
        };

        ClickableSpan simpfunLicenseClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.yuque.com/simpfun/sfe/tos"));
                startActivity(intent);
            }
        };

        sp2 = this.getSharedPreferences("token", this.MODE_PRIVATE);
        sp1 = this.getSharedPreferences("username", this.MODE_PRIVATE);

        name.setText(sp1.getString("usname", ""));
        pwd.setText(sp1.getString("uspwd", ""));
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        spannableString.setSpan(softwareLicenseClick, 4, 9, 0);
        spannableString.setSpan(simpfunLicenseClick, 11, 17, 0);

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = name.getText().toString().trim();
                String password = pwd.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "账号和密码不能为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!agreeCheckbox.isChecked()) {
                    Toast.makeText(LoginActivity.this, "请先同意软件许可协议和简幻欢许可协议！", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnlogin.setEnabled(false);
                login(username, password);
            }
        });

        btnreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void login(String username, String password) {
        String url = "https://api.simpfun.cn/api/auth/login";
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String json = "{\"username\":\"" + username + "\", \"passwd\":\"" + password + "\"}";

        RequestBody body = RequestBody.create(mediaType, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "登录请求失败！", Toast.LENGTH_LONG).show();
                    btnlogin.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code");
                        String msg = jsonResponse.getString("msg");
                        String token = jsonResponse.optString("token");

                        runOnUiThread(() -> {
                            if (code == 200) {
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor editor = sp1.edit();
                                editor.putString("usname", username);
                                editor.putString("uspwd", password);
                                editor.apply();
                                saveToken(token);
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                            handler.postDelayed(() -> btnlogin.setEnabled(true), 5000);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "解析错误！", Toast.LENGTH_SHORT).show();
                            btnlogin.setEnabled(true);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_LONG).show();
                        btnlogin.setEnabled(true);
                    });
                }
            }
        });
    }

    private void saveToken(String token) {
        SharedPreferences.Editor editor = sp2.edit();
        editor.putString("token", token);
        editor.apply();
    }

    private void showTokenInputDialog() {
        EditText input = new EditText(this);
        input.setHint("请输入 token");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("手动填写 Token")
                .setView(input)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String token = input.getText().toString().trim();
                        if (!token.isEmpty()) {
                            saveToken(token);
                            Toast.makeText(LoginActivity.this, "Token 已保存", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, Welcome.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Token 不能为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
