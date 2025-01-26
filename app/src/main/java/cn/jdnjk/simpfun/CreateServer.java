package cn.jdnjk.simpfun;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CreateServer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // 初始化底部导航栏
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 设置底部导航栏监听
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                // 处理导航栏点击事件
                if (item.getItemId() == R.id.nav_home) {
                    // 跳转到主页
                    Intent intent = new Intent(CreateServer.this, Welcome.class);
                    startActivity(intent);  // 启动 HomeActivity
                    return true; // 处理了此事件
                } else {
                    return false; // 如果没有匹配的项，返回 false
                }
            }
        });
    }
}
