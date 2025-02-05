package cn.jdnjk.simpfun;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ServerManage extends AppCompatActivity {

    private EditText commandInput;
    private TextView commandOutput;
    private Button simulateEnterButton;
    private ScrollView scrollView;  // 用来获取滚动视图
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term);

        // 初始化 UI 组件
        commandInput = findViewById(R.id.commandInput);
        commandOutput = findViewById(R.id.commandOutput);
        simulateEnterButton = findViewById(R.id.simulateEnterButton);
        scrollView = findViewById(R.id.scrollView); // 获取滚动视图

        // 设置模拟回车按钮的点击事件
        simulateEnterButton.setOnClickListener(v -> simulateEnterKey());
    }

    // 模拟回车键按下事件
    private void simulateEnterKey() {
        // 执行命令
        String command = commandInput.getText().toString();
        executeCommand(command);
        commandInput.setText(""); // 清空输入框
    }

    // 执行命令的方法
    private void executeCommand(String command) {
        // 通过 Handler 分批显示输出，避免卡顿
        String result = command;

        // 使用 Handler 延迟更新
        handler.postDelayed(() -> {
            // 使用 StringBuilder 拼接文本，避免频繁更新 TextView
            commandOutput.append(result + "\n");

            // 滚动到文本底部
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }, 100); // 延迟 100 毫秒更新 UI，避免卡顿
    }
}


