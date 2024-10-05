package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class EsferaCelesteView extends View {
    private GnssStatus newStatus;  // Estado GNSS atual
    private Paint paint;           // Pincel para desenhar no Canvas
    private int r;                 // Raio da esfera celeste
    private int height, width;     // Altura e largura do View
    private GNSSActivity gnssActivity; // Referência à atividade GNSS

    // Construtor da View EsferaCelesteView
    public EsferaCelesteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();  // Inicializa o pincel

        // Verifica se o contexto passado é uma instância de GNSSActivity
        if (context instanceof GNSSActivity) {
            gnssActivity = (GNSSActivity) context;
        } else {
            throw new IllegalArgumentException("Contexto precisa ser GNSSActivity");
        }

        // Define o clique para abrir o diálogo de seleção de satélites
        setOnClickListener(v -> showSatelliteSelectionDialog());
    }

    // Exibe o diálogo de seleção de satélites
    private void showSatelliteSelectionDialog() {
        if (gnssActivity == null) {
            return;  // Verifica se a atividade GNSS foi corretamente inicializada
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Localização do usuário");

        // Infla o layout do diálogo
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);
        builder.setView(dialogView);

        // Recupera os CheckBoxes do layout
        CheckBox gpsCheckBox = dialogView.findViewById(R.id.gpsCheckBox);
        CheckBox galileoCheckBox = dialogView.findViewById(R.id.galileoCheckBox);
        CheckBox glonassCheckBox = dialogView.findViewById(R.id.glonassCheckBox);
        CheckBox unknownCheckBox = dialogView.findViewById(R.id.unknownCheckBox);
        CheckBox usedInFixCheckBox = dialogView.findViewById(R.id.usedInFixCheckBox);
        CheckBox notUsedInFixCheckBox = dialogView.findViewById(R.id.notUsedInFixCheckBox);

        // Carrega as preferências salvas
        gpsCheckBox.setChecked(gnssActivity.getSatellitePreference("gpsChecked", true));
        galileoCheckBox.setChecked(gnssActivity.getSatellitePreference("galileoChecked", true));
        glonassCheckBox.setChecked(gnssActivity.getSatellitePreference("glonassChecked", true));
        unknownCheckBox.setChecked(gnssActivity.getSatellitePreference("unknownChecked", true));
        usedInFixCheckBox.setChecked(gnssActivity.getSatellitePreference("usedInFix", true));
        notUsedInFixCheckBox.setChecked(gnssActivity.getSatellitePreference("notUsedInFix", true));

        // Configura o botão "Salvar" no diálogo
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            gnssActivity.saveSatellitePreferences(
                    gpsCheckBox.isChecked(),
                    galileoCheckBox.isChecked(),
                    glonassCheckBox.isChecked(),
                    unknownCheckBox.isChecked(),
                    usedInFixCheckBox.isChecked(),
                    notUsedInFixCheckBox.isChecked()
            );
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        // Exibe o diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Sobrescreve o método de desenho da View
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Coleta as dimensões da tela
        width = getMeasuredWidth();
        height = getMeasuredHeight();

        // Define o raio da esfera celeste
        r = (int) (Math.min(width, height) / 2 * 0.9);

        // Configura o pincel para desenhar a esfera
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);

        // Desenha círculos concêntricos
        canvas.drawCircle(computeXc(0), computeYc(0), r, paint);
        int radius45 = (int) (r * Math.cos(Math.toRadians(45)));
        int radius60 = (int) (r * Math.cos(Math.toRadians(60)));
        canvas.drawCircle(computeXc(0), computeYc(0), radius45, paint);
        canvas.drawCircle(computeXc(0), computeYc(0), radius60, paint);

        // Desenha os eixos
        canvas.drawLine(computeXc(0), computeYc(-r), computeXc(0), computeYc(r), paint);
        canvas.drawLine(computeXc(-r), computeYc(0), computeXc(r), computeYc(0), paint);

        // Configura o pincel para desenhar os satélites
        paint.setStyle(Paint.Style.FILL);

        // Desenha os satélites de acordo com as preferências
        desenharSatelites(canvas);
    }

    // Método para desenhar os satélites
    private void desenharSatelites(Canvas canvas) {
        if (newStatus == null) {
            return;  // Não desenha se não houver status de satélite
        }

        // Recupera as preferências do usuário
        boolean gpsSelected = gnssActivity.getSatellitePreference("gpsChecked", true);
        boolean galileoSelected = gnssActivity.getSatellitePreference("galileoChecked", true);
        boolean glonassSelected = gnssActivity.getSatellitePreference("glonassChecked", true);
        boolean unknownSelected = gnssActivity.getSatellitePreference("unknownChecked", true);
        boolean usedInFixSelected = gnssActivity.getSatellitePreference("usedInFix", true);
        boolean notUsedInFixSelected = gnssActivity.getSatellitePreference("notUsedInFix", true);

        // Percorre os satélites e desenha os que atendem às condições
        for (int i = 0; i < newStatus.getSatelliteCount(); i++) {
            int constellationType = newStatus.getConstellationType(i);
            boolean usedInFix = newStatus.usedInFix(i);
            boolean shouldDraw = false;

            // Configura as cores com base na constelação
            switch (constellationType) {
                case GnssStatus.CONSTELLATION_GPS:
                    paint.setColor(Color.RED);
                    shouldDraw = gpsSelected;
                    break;
                case GnssStatus.CONSTELLATION_GLONASS:
                    paint.setColor(Color.YELLOW);
                    shouldDraw = glonassSelected;
                    break;
                case GnssStatus.CONSTELLATION_GALILEO:
                    paint.setColor(Color.GREEN);
                    shouldDraw = galileoSelected;
                    break;
                default:
                    paint.setColor(Color.GRAY);
                    shouldDraw = unknownSelected;
                    break;
            }

            // Verifica se o satélite deve ser desenhado com base no uso em fixação
            if (usedInFix && !usedInFixSelected) continue;
            if (!usedInFix && !notUsedInFixSelected) continue;

            // Desenha o satélite se todas as condições forem atendidas
            if (shouldDraw) {
                float az = newStatus.getAzimuthDegrees(i);
                float el = newStatus.getElevationDegrees(i);
                float x = (float) (r * Math.cos(Math.toRadians(el)) * Math.sin(Math.toRadians(az)));
                float y = (float) (r * Math.cos(Math.toRadians(el)) * Math.cos(Math.toRadians(az)));
                canvas.drawCircle(computeXc(x), computeYc(y), 10, paint);
                paint.setTextSize(30);
                String satID = newStatus.getSvid(i) + (usedInFix ? " - Sim" : " - Não");
                canvas.drawText(satID, computeXc(x) + 10, computeYc(y) + 10, paint);
            }
        }
    }

    // Método auxiliar para calcular a coordenada X
    private int computeXc(double x) {
        return (int) (x + width / 2);
    }

    // Método auxiliar para calcular a coordenada Y
    private int computeYc(double y) {
        return (int) (-y + height / 2);
    }

    // Método para definir o novo status de GNSS
    public void setNewStatus(GnssStatus newStatus) {
        this.newStatus = newStatus;
        invalidate();  // Redesenha a View
    }
}
