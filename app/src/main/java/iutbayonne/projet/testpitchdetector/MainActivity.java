package iutbayonne.projet.testpitchdetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {

    private ImageView image;
    private ImageButton btnStart;
    private ImageButton btnStop;
    private TextView pitchText;
    private TextView typeFrequence;
    private TextView intitule;
    private Thread audioThread;
    private AudioDispatcher dispatcher;

    private static final String LOG_TAG = "AudioRecordTest";

    /* Code arbitraire désignant la permission d'enregistrer l'audio.
    Sera utilisé lors de la demande et de la réception de la réponse à la demande*/
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private boolean  permissionToRecordAccepted  = false;

    // Permissions à demander. Ici, seulement la permission pour enregistrer le son provenant du micro.
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    /* Méthode qui s'exécute lorsque l'on reçoit les résultats de toutes les
    demandes de permission de l'application. Ici, nous ne nous soucions que de
    la permission pour enregistrer le son provenant du micro */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode)
        {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
        }

        if (!permissionToRecordAccepted)
        {
            Log.e(LOG_TAG, "Permission d'enregistrer non approuvée");
            // L'application se ferme
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Demande la permission d'enregistrer du son à l'utilisateur
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        image = findViewById(R.id.image);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        pitchText = findViewById(R.id.pitchText);
        typeFrequence = findViewById(R.id.typeFrequence);
        intitule = findViewById(R.id.intitule);

        btnStop.setEnabled(false);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intitule.setText("Enregistrement en cours");
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                lancerEcoute(true);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intitule.setText("En attente");
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
                lancerEcoute(false);
            }
        });
    }


    public void processPitch(float pitchInHz)
    {
        pitchText.setText("" + pitchInHz + " Hz");

        if(pitchInHz == -1)
        {
            image.setImageResource(R.drawable.initial);
            pitchText.setText("Aucun son audible");
            typeFrequence.setText("");
        }
        else if(pitchInHz > 0 && pitchInHz <= 150)
        {
            image.setImageResource(R.drawable.zone_a);
            typeFrequence.setText("Basse fréquence");
        }
        else if(pitchInHz > 150 && pitchInHz <= 250)
        {
            image.setImageResource(R.drawable.zone_b);
            typeFrequence.setText("Moyenne fréquence");
        }
        else if(pitchInHz > 400)
        {
            image.setImageResource(R.drawable.zone_c);
            typeFrequence.setText("Haute fréquence");
        }
    }

    public void lancerEcoute(boolean start)
    {
        if(start)
        {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);

            PitchDetectionHandler pdh = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e){
                    final float pitchInHz = res.getPitch();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processPitch(pitchInHz);
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
            dispatcher.addAudioProcessor(pitchProcessor);

            audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();
        }
        else
        {
            audioThread.interrupt();
            dispatcher.stop();
            pitchText.setText("");
        }

    }
}
