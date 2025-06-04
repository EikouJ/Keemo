package com.keyboard.keemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button enableKeyboardBtn = findViewById(R.id.btn_enable_keyboard);
        Button selectKeyboardBtn = findViewById(R.id.btn_select_keyboard);

        enableKeyboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open Input Method settings to enable the keyboard
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Enable 'Keemo Keyboard' in the list", Toast.LENGTH_LONG).show();
            }
        });

        selectKeyboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show input method picker to select the keyboard
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                    Toast.makeText(MainActivity.this, "Select 'Keemo Keyboard' from the list", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to show keyboard picker", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}