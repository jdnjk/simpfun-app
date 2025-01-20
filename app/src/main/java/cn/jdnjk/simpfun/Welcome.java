package cn.jdnjk.simpfun;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;

public class Welcome extends AppCompatActivity {
    SharedPreferences sp3;
    TextView topBarInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);  // 设置布局

        // 获取 SharedPreferences，用于读取保存的用户信息
        sp3 = this.getSharedPreferences("info", MODE_PRIVATE);
        topBarInfo = this.findViewById(R.id.topBarInfo);
        String username = sp3.getString("username", "NaN");  // 获取用户名,获取不到就摆烂
        // 获取并格式化顶栏信息
        int points = sp3.getInt("point", 0);  // 获取积分
        int diamonds = sp3.getInt("diamond", 0);  // 获取钻石
        int uid = sp3.getInt("uid", 0);  // 获取 uid

        // 设置顶栏信息
        topBarInfo.setText("用户名: " + username + " 积分: " + points + " 钻石: " + diamonds + " uid: " + uid);

        // 初始化底部导航栏
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 设置底部导航栏监听
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                // 处理导航栏点击事件
                if (item.getItemId() == R.id.nav_home) {
                    // 跳转到主页
                    Intent intent = new Intent(Welcome.this, HomeActivity.class);
                    startActivity(intent);  // 启动 HomeActivity
                    return true; // 处理了此事件
                } else {
                    return false; // 如果没有匹配的项，返回 false
                }
            }
        });
    }
}


