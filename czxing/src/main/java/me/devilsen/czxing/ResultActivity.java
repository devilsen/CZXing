package me.devilsen.czxing;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author : dongSen
 * date : 2019/07/21
 * desc : 扫码结果显示界面
 */
public class ResultActivity extends Activity implements View.OnClickListener {

    private TextView resultTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);
        String result = getIntent().getStringExtra("result");

        resultTxt = findViewById(R.id.text_view_scan_result);
        Button copyBtn = findViewById(R.id.button_scan_copy);
        resultTxt.setText(result);

        copyBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("", resultTxt.getText().toString());
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }
}
