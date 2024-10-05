package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Corrigi a importação direta do layout

        // Botão para iniciar a GNSSActivity
        Button buttonGnss = findViewById(R.id.button_gnss);
        buttonGnss.setOnClickListener(view -> {
            // Antes de iniciar a GNSSActivity, podemos checar se GNSS está disponível
            if (isGNSSAvailable()) {
                Intent intent = new Intent(getApplicationContext(), GNSSActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "GNSS não está disponível no seu dispositivo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para verificar se o GNSS está disponível no dispositivo
    private boolean isGNSSAvailable() {
        // Aqui podemos verificar se o GPS está habilitado
        return getPackageManager().hasSystemFeature("android.hardware.location.gps");
    }
}
