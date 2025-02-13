package cn.jdnjk.simpfun;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class ServerManage extends AppCompatActivity {

    private EditText commandInput;
    private TextView commandOutput;
    private Button simulateEnterButton;
    private ScrollView scrollView;
    private Handler handler = new Handler();
    private OkHttpClient client = new OkHttpClient();
    private WebSocket webSocket;
    private String authToken;
    private String socketUrl;
    private String requestToken;
    private int deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term);

        // 初始化 UI 组件
        commandInput = findViewById(R.id.commandInput);
        commandOutput = findViewById(R.id.commandOutput);
        simulateEnterButton = findViewById(R.id.simulateEnterButton);
        scrollView = findViewById(R.id.scrollView);

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                simulateEnterKey();
                return true;
            }
            return false;
        });

        // 从 SharedPreferences 获取登录 token
        SharedPreferences sp = getSharedPreferences("token", MODE_PRIVATE);
        authToken = sp.getString("token", null);

        // 获取传入的设备 ID
        deviceId = getIntent().getIntExtra("device_id", -1);

        if (authToken != null && deviceId != -1) {
            // 请求 WebSocket 服务器地址
            fetchWebSocketDetails(authToken, deviceId);
        } else {
            Toast.makeText(this, "Token 为空或设备 ID 无效，请重新登录", Toast.LENGTH_SHORT).show();
        }

        // 设置按钮点击事件
        simulateEnterButton.setOnClickListener(v -> simulateEnterKey());
    }

    // 获取 WebSocket 服务器地址，并返回 token
    private void fetchWebSocketDetails(String authToken, int deviceId) {
        String url = "https://api.simpfun.cn/api/ins/" + deviceId + "/ws";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ServerManage.this, "获取 WebSocket 地址失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code");
                        if (code == 200) {
                            // 获取 WebSocket 服务器地址和 token
                            socketUrl = jsonResponse.getJSONObject("data").getString("socket");
                            requestToken = jsonResponse.getJSONObject("data").getString("token");

                            // 连接 WebSocket
                            connectToWebSocket(socketUrl);
                        } else {
                            String msg = jsonResponse.getString("msg");
                            runOnUiThread(() -> {
                                Toast.makeText(ServerManage.this, "错误: " + msg, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(ServerManage.this, "解析 WebSocket 地址失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ServerManage.this, "请求失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // 连接 WebSocket 服务器
    private void connectToWebSocket(String socketUrl) {
        Request request = new Request.Builder().url(socketUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // 连接成功，发送认证信息
                sendAuthMessage();
                // 发送日志请求，获取服务器日志
                sendLogMessage();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonMessage = new JSONObject(text);
                        if (jsonMessage.has("event") && "console output".equals(jsonMessage.getString("event"))) {
                            String output = jsonMessage.getJSONArray("args").getString(0);
                            commandOutput.append(output + "\n");
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ServerManage.this, "WebSocket 连接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                webSocket.close(1000, null);
            }
        });
    }

    // 发送认证消息
    private void sendAuthMessage() {
        try {
            JSONArray argsArray = new JSONArray();
            argsArray.put(requestToken);

            JSONObject authMessage = new JSONObject();
            authMessage.put("event", "auth");
            authMessage.put("args", argsArray);

            webSocket.send(authMessage.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送获取日志的消息
    private void sendLogMessage() {
        try {
            JSONObject logMessage = new JSONObject();
            logMessage.put("event", "send logs");
            logMessage.put("args", new JSONArray());

            webSocket.send(logMessage.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 处理回车键按下事件
    private void simulateEnterKey() {
        String command = commandInput.getText().toString();
        if (!command.isEmpty()) {
            executeCommand(command);
            commandInput.setText("");
        }
    }

    // 发送输入的命令到服务器
    private void executeCommand(String command) {
        if (webSocket == null) {
            Toast.makeText(this, "WebSocket 未连接", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray argsArray = new JSONArray();
            argsArray.put(command);

            JSONObject commandMessage = new JSONObject();
            commandMessage.put("event", "send command");
            commandMessage.put("args", argsArray);

            webSocket.send(commandMessage.toString());
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
