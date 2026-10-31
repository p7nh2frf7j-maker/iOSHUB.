package com.example.helloapk;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 把包名和版本打到界面上 —— 装完 APK 一眼就能确认装对了包
        TextView tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setText("包名：" + getPackageName() + "\n版本：" + versionName());

        Button btn = findViewById(R.id.btnToast);
        btn.setOnClickListener(v ->
                Toast.makeText(this, "APK 打包成功 🎉", Toast.LENGTH_SHORT).show());
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
