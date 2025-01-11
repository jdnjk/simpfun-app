package cn.jdnjk.simpfun;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    EditText name, pwd;
    Button btnlogin, btnreg;
    Mysql mysql;
    SQLiteDatabase db;
    SharedPreferences sp1, sp2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 这里可以添加必要的初始化代码
        sp1 = this.getSharedPreferences("useinfo", this.MODE_PRIVATE);
        sp2 = this.getSharedPreferences("username", this.MODE_PRIVATE);

        // 直接跳转到 LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 可选，结束 MainActivity，防止用户返回到这个 Activity
    }
}