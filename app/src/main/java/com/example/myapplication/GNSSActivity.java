package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.BarChart;  // Importando a biblioteca para o gráfico
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class GNSSActivity extends AppCompatActivity implements SensorEventListener {
    private LocationManager locationManager;  // Gerenciador de localização
    private LocationProvider locationProvider;  // Provedor de localização
    private static final int REQUEST_LOCATION = 1;  // Código de solicitação de permissão de localização
    private SharedPreferences sharedPreferences;  // Armazena preferências do usuário
    private SensorManager sensorManager;  // Gerenciador de sensores
    private Sensor rotationVectorSensor;  // Sensor de rotação
    private float[] rotationMatrix = new float[9];  // Matriz de rotação
    private float[] orientationAngles = new float[3];  // Ângulos de orientação
    private ImageView compassArrow;  // Referência para a seta da bússola
    private BarChart chart; // Declaração do gráfico

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.esfera_celeste_layout);

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("SatellitePreferences", Context.MODE_PRIVATE);

        // Inicializar o sensor de rotação
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Inicializar a seta de bússola
        compassArrow = findViewById(R.id.compassArrow);

        // Inicializar o gráfico
        chart = findViewById(R.id.chartSNR);  // Adicionando o gráfico

        // Inicializar o LocationManager e verificar permissões
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        obtemLocationProvider_Permission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar o listener do sensor de rotação
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Desregistrar o listener do sensor ao pausar a atividade
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // Atualiza a matriz de rotação e obtém os ângulos de orientação
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float azimuthInDegrees = (float) Math.toDegrees(orientationAngles[0]);
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360;
            }

            // Atualiza a seta da bússola e exibe o rumo
            rotateCompassArrow(azimuthInDegrees);
            mostraRumo(azimuthInDegrees);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não utilizado
    }

    private void rotateCompassArrow(float azimuthInDegrees) {
        // Rotaciona a seta da bússola com base no azimute
        compassArrow.setRotation(azimuthInDegrees);
    }

    // Verifica e solicita permissões de localização
    public void obtemLocationProvider_Permission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            startLocationAndGNSSUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtemLocationProvider_Permission();
        } else {
            Toast.makeText(this, "Sem permissão para acessar o sistema de posicionamento", Toast.LENGTH_SHORT).show();
            finish();  // Finaliza a atividade caso a permissão não seja concedida
        }
    }

    // Inicia as atualizações de localização e GNSS
    public void startLocationAndGNSSUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.1f, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                mostraLocation(location);
            }
        });

        locationManager.registerGnssStatusCallback(new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                mostraGNSS(status);
                mostraGNSSGrafico(status);
                atualizaGraficoSatelites(status);  // Chamada para atualizar o gráfico
            }
        });
    }

    // Exibe as informações de localização na interface
    public void mostraLocation(Location location) {
        TextView textView = findViewById(R.id.textviewLocation_id);
        if (location != null) {
            String info = "Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude();
            textView.setText(info);
        } else {
            textView.setText("Localização não disponível");
        }
    }

    // Exibe informações de GNSS na interface
    public void mostraGNSS(GnssStatus status) {
        // Aqui você pode exibir informações detalhadas sobre os satélites captados
        TextView textView = findViewById(R.id.textSatelliteQuality_id);
        StringBuilder sb = new StringBuilder();
        sb.append("Número de Satélites: ").append(status.getSatelliteCount()).append("\n");

        for (int i = 0; i < status.getSatelliteCount(); i++) {
            int svid = status.getSvid(i);
            int constellationType = status.getConstellationType(i);
            boolean usedInFix = status.usedInFix(i);
            sb.append("SVID: ").append(svid)
                    .append(" Constelação: ").append(constellationType)
                    .append(" Usado na correção: ").append(usedInFix ? "Sim" : "Não").append("\n");
        }

        textView.setText(sb.toString());
    }

    // Atualiza o gráfico de GNSS na EsferaCelesteView
    public void mostraGNSSGrafico(GnssStatus status) {
        EsferaCelesteView esferaCelesteView = findViewById(R.id.esferacelesteview_id);
        if (esferaCelesteView != null) {
            esferaCelesteView.setNewStatus(status);  // Atualiza a view com o novo status GNSS
        }
    }

    // Atualiza o gráfico de SNR dos satélites
    public void atualizaGraficoSatelites(GnssStatus status) {
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < status.getSatelliteCount(); i++) {
            float snr = status.getCn0DbHz(i); // Força do sinal
            entries.add(new BarEntry(i, snr));
        }

        BarDataSet dataSet = new BarDataSet(entries, "SNR dos Satélites");
        BarData barData = new BarData(dataSet);
        chart.setData(barData);
        chart.invalidate(); // Redesenha o gráfico com os dados atualizados
    }

    // Exibe o rumo da bússola
    private void mostraRumo(float azimuth) {
        TextView textView = findViewById(R.id.textDisplacement_id);
        textView.setText("Rumo: " + azimuth + "°");
    }

    // Método para salvar as preferências do usuário
    public void saveSatellitePreferences(boolean gpsChecked, boolean galileoChecked, boolean glonassChecked,
                                         boolean unknownChecked, boolean usedInFixChecked, boolean notUsedInFixChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("gpsChecked", gpsChecked);
        editor.putBoolean("galileoChecked", galileoChecked);
        editor.putBoolean("glonassChecked", glonassChecked);
        editor.putBoolean("unknownChecked", unknownChecked);
        editor.putBoolean("usedInFix", usedInFixChecked);
        editor.putBoolean("notUsedInFix", notUsedInFixChecked);
        editor.apply();
    }

    // Método para recuperar as preferências de satélite do usuário
    public boolean getSatellitePreference(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }
}
