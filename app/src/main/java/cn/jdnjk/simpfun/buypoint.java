// PaymentActivity.java
package cn.jdnjk.simpfun;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class buypoint extends Activity {
    private RadioGroup paymentMethodGroup, pointSelectionGroup;
    private Button payButton;

    // 积分和价格对应表
    private final int[] points = {200, 600, 3300, 12000, 40000};
    private final int[] prices = {5, 12, 60, 200, 600};

    private int selectedPoints = points[0];
    private int selectedPrice = prices[0];
    private String selectedPaymentMethod = "WeChat"; // 默认微信支付
    private String orderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        pointSelectionGroup = findViewById(R.id.pointSelectionGroup);
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        payButton = findViewById(R.id.payButton);

        // 监听积分选择
        pointSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int index = group.indexOfChild(findViewById(checkedId));
            if (index >= 0) {
                selectedPoints = points[index];
                selectedPrice = prices[index];
            }
        });

        // 监听支付方式选择
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioWeChat) {
                selectedPaymentMethod = "WeChat";
            } else if (checkedId == R.id.radioAlipay) {
                selectedPaymentMethod = "Alipay";
            }
        });

        // 点击支付按钮
        payButton.setOnClickListener(v -> processPayment());
    }

    private void processPayment() {
        if ("WeChat".equals(selectedPaymentMethod)) {
            Toast.makeText(this, "暂不支持微信支付", Toast.LENGTH_LONG).show();
            return;
        }

        String paymentMethodCode = selectedPaymentMethod.equals("WeChat") ? "wx_pay_2" : "ali_pay_1";
        sendPaymentRequest(selectedPoints, paymentMethodCode);
    }

    private void sendPaymentRequest(int point, String method) {
        new Thread(() -> {
            try {
                SharedPreferences sharedPreferences = getSharedPreferences("token", MODE_PRIVATE);
                String token = sharedPreferences.getString("token", "");

                if (token.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "未找到有效的 token", Toast.LENGTH_LONG).show());
                    return;
                }

                URL url = new URL("https://api.simpfun.cn/api/recharge");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("authorization", token);
                conn.setDoOutput(true);

                String urlParameters = "point=" + point + "&method=" + method;

                OutputStream os = conn.getOutputStream();
                os.write(urlParameters.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    try {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.getInt("code") == 200) {
                            String rawUrl = jsonResponse.getString("url");
                            orderId = jsonResponse.getString("order_id");

                            if ("ali_pay_1".equals(method)) {
                                // 支付宝支付时拼接支付宝 URL
                                String encodedUrl = Uri.encode(rawUrl); // 对 URL 进行编码
                                String aliPayUrl = "alipays://platformapi/startapp?appId=20000917&url=" + encodedUrl;

                                // 打开支付宝支付页面
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(aliPayUrl));
                                startActivity(intent);
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "支付请求失败: " + responseCode, Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("PaymentError", "支付请求异常", e);
                runOnUiThread(() -> Toast.makeText(this, "支付请求异常", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
