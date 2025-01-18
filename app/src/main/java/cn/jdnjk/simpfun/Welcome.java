package cn.jdnjk.simpfun;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class Welcome extends AppCompatActivity {
    SharedPreferences sp1;
    TextView topBarInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 获取 SharedPreferences，用于读取保存的用户信息
        sp1 = this.getSharedPreferences("userInfo", MODE_PRIVATE);
        topBarInfo = this.findViewById(R.id.topBarInfo);
        String username = sp1.getString("username", "NaN");  // 获取用户名,获取不到就摆烂
        // 获取并格式化顶栏信息
        int points = sp1.getInt("point", 0);  // 获取积分
        int diamonds = sp1.getInt("diamond", 0);  // 获取钻石
        int uid = sp1.getInt("id", 0);  // 获取 uid

        // 设置顶栏信息
        topBarInfo.setText("用户名: " + username + " 积分: " + points + " 钻石: " + diamonds + " uid: " + uid);
    }
}
