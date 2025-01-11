package cn.jdnjk.simpfun;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 指定布局
        setContentView(R.layout.activity_register); // 确保这里使用您正确的布局文件

        // 初始化WebView
        webView = findViewById(R.id.webview); // 这行代码需要确保布局文件中有 WebView

        // 启用JavaScript
        WebSettings webSettings = webView.getSettings();
        String defaultUserAgent = webSettings.getUserAgentString();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString(defaultUserAgent + " SimpfunAPP/1.0"); // 设置自定义的UserAgent

        // 允许WebView打开链接而不使用外部浏览器
        webView.setWebViewClient(new WebViewClient());

        // 加载指定的URL
        webView.loadUrl("https://simpfun.cn/auth?type=register");
    }

    // 处理返回键，返回至上一页面而非退出应用
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}