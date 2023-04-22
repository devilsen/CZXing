package me.sam.czxing.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.sam.czxing.R;

/**
 * @author dongsen
 * date: 2019/07/21
 */
public class DelegateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegate);
        String result = getIntent().getStringExtra("result");

        TextView resultTxt = findViewById(R.id.text_view_delegate_result);
        resultTxt.setText(result);
    }
}
