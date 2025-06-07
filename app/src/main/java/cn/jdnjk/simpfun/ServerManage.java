package cn.jdnjk.simpfun;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.fox2code.androidansi.AnsiParser;
import com.fox2code.androidansi.AnsiTextView;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ServerManage extends AppCompatActivity {

    private EditText commandInput;
    private AnsiTextView commandOutput;
    private StringBuilder ansiBuffer = new StringBuilder();
    private Button simulateEnterButton;
    private ScrollView scrollView;
    private TextView serverAddressText;
    private Handler handler = new Handler();
    private OkHttpClient client = new OkHttpClient();
    private WebSocket webSocket;
    private String authToken;
    private String socketUrl;
    private String requestToken;
    private TextView serverNameText;
    private int deviceId;
    private boolean isBufferUpdateScheduled = false;
    private final long RENDER_DELAY = 100;
    private Button btnStart, btnRestart, btnShutdown, btnForceStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term);

        commandInput = findViewById(R.id.commandInput);
        commandOutput = findViewById(R.id.commandOutput);
        simulateEnterButton = findViewById(R.id.simulateEnterButton);
        scrollView = findViewById(R.id.scrollView);
        btnStart = findViewById(R.id.button_start);
        btnRestart = findViewById(R.id.button_restart);
        btnShutdown = findViewById(R.id.button_shutdown);
        btnForceStop = findViewById(R.id.button_forcestop);
        // 设置点击事件
        btnStart.setOnClickListener(v -> powerAction("start"));
        btnRestart.setOnClickListener(v -> powerAction("restart"));
        btnShutdown.setOnClickListener(v -> powerAction("stop"));
        btnForceStop.setOnClickListener(v -> powerAction("kill"));
        serverNameText = findViewById(R.id.serverNameText);
        serverAddressText = findViewById(R.id.serverAddressText);
        setupServerNameEdit();

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                simulateEnterKey();
                return true;
            }
            return false;
        });

        SharedPreferences sp = getSharedPreferences("token", MODE_PRIVATE);
        authToken = sp.getString("token", null);
        deviceId = getIntent().getIntExtra("device_id", -1);

        if (authToken != null && deviceId != -1) {
            fetchWebSocketDetails(authToken, deviceId);
        } else {
            Toast.makeText(this, "Token 为空或设备 ID 无效，请重新登录", Toast.LENGTH_SHORT).show();
        }
        if (authToken != null && deviceId != -1) {
            fetchServerDetails(); // 获取服务器详情
            fetchWebSocketDetails(authToken, deviceId);
        }

        simulateEnterButton.setOnClickListener(v -> simulateEnterKey());
    }

    private void setupServerNameEdit() {
        EditText editText = new EditText(this);
        editText.setVisibility(View.GONE);
        ((ViewGroup) serverNameText.getParent()).addView(editText);

        // 点击文本时显示编辑框
        serverNameText.setOnClickListener(v -> {
            serverNameText.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            editText.setText(serverNameText.getText());
            editText.setSelection(editText.getText().length());
            editText.requestFocus();
        });

        // 设置编辑框的完成动作监听
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitNewName(editText.getText().toString());
                editText.setVisibility(View.GONE);
                serverNameText.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        // 失去焦点时也提交修改
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                submitNewName(editText.getText().toString());
                editText.setVisibility(View.GONE);
                serverNameText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void submitNewName(String newName) {
        if (newName.isEmpty()) return;

        RequestBody formBody = new FormBody.Builder()
                .add("name", newName)
                .build();

        Request request = new Request.Builder()
                .url("https://api.simpfun.cn/api/ins/" + deviceId + "/rename")
                .header("Authorization", authToken)
                .header("User-Agent", "SimpfunAPP/1.1")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServerManage.this,
                        "重命名失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code");
                        runOnUiThread(() -> {
                            if (code == 200) {
                                serverNameText.setText(newName);
                                Toast.makeText(ServerManage.this, "重命名成功",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = null;
                                try {
                                    msg = jsonResponse.getString("msg");
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                Toast.makeText(ServerManage.this, "错误: " + msg,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ServerManage.this,
                                "响应解析失败", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void fetchServerDetails() {
        String url = "https://api.simpfun.cn/api/ins/" + deviceId + "/detail";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authToken)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    serverNameText.setText("未命名实例");
                    serverAddressText.setText("IP：我不到啊");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONObject data = jsonResponse.getJSONObject("data");
                        String name = data.optString("name", null);
                        JSONObject allocation = data.getJSONObject("default_allocation");
                        String ip = allocation.getString("ip");
                        int port = allocation.getInt("port");
                        final String address = "IP：" + ip + ":" + port;

                        runOnUiThread(() -> {
                            if (name != null && !name.isEmpty()) {
                                serverNameText.setText(name);
                            } else {
                                serverNameText.setText("未命名实例");
                            }

                            serverAddressText.setText(address);
                            serverAddressText.setOnClickListener(v -> {
                                android.content.ClipboardManager clipboard =
                                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                android.content.ClipData clip =
                                        android.content.ClipData.newPlainText("服务器地址", ip + ":" + port);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ServerManage.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                            });
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            serverNameText.setText("未命名实例");
                            serverAddressText.setText("IP：解析失败");
                        });
                    }
                }
            }
        });
    }

    private void fetchWebSocketDetails(String authToken, int deviceId) {
        String url = "https://api.simpfun.cn/api/ins/" + deviceId + "/ws";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authToken)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ServerManage.this, "获取 WebSocket 地址失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code");
                        if (code == 200) {
                            socketUrl = jsonResponse.getJSONObject("data").getString("socket");
                            requestToken = jsonResponse.getJSONObject("data").getString("token");
                            connectToWebSocket(socketUrl);
                        } else {
                            String msg = jsonResponse.getString("msg");
                            runOnUiThread(() -> Toast.makeText(ServerManage.this, "错误: " + msg, Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(ServerManage.this, "解析 WebSocket 地址失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ServerManage.this, "请求失败: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void connectToWebSocket(String socketUrl) {
        Request request = new Request.Builder().url(socketUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                sendAuthMessage();
                sendLogMessage();
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JSONObject msg = new JSONObject(text);
                    if ("console output".equals(msg.optString("event"))) {
                        String output = msg.getJSONArray("args").getString(0);
                        output = output.replaceAll("\u001b\\[\\?1h\u001b=", "")
                                .replaceAll("\\[\\?2004h|>....|\\[K", "");
                        ansiBuffer.append(output).append("\n");

                        if (!isBufferUpdateScheduled) {
                            isBufferUpdateScheduled = true;
                            handler.postDelayed(() -> {
                                runOnUiThread(() -> {
                                    AnsiParser.setAnsiText(
                                            commandOutput,
                                            ansiBuffer.toString(),
                                            0
                                    );
                                    // 使用post确保在文本更新后再滚动
                                    scrollView.post(() -> {
                                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                    });
                                });
                                isBufferUpdateScheduled = false;
                            }, RENDER_DELAY);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                runOnUiThread(() -> Toast.makeText(ServerManage.this, "WebSocket 连接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                webSocket.close(1000, null);
            }
        });
    }

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

    private void simulateEnterKey() {
        String command = commandInput.getText().toString();
        if (!command.isEmpty()) {
            executeCommand(command);
            commandInput.setText("");
        }
    }

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

            // 滚动到底部，方便查看命令反馈
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void powerAction(String action) {
        String url = "https://api.simpfun.cn/api/ins/" + deviceId + "/power?action=" + action;

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authToken)
                .header("User-Agent", "SimpfunAPP/1.1")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServerManage.this,
                        "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.getInt("code");
                        final String msg = jsonResponse.getString("msg");
                        runOnUiThread(() -> {
                            if (code == 200) {
                                Toast.makeText(ServerManage.this, "操作成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ServerManage.this, "错误: " + msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(ServerManage.this,
                                "响应解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }
}