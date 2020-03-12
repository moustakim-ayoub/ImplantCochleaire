package iutbayonne.projet.testpitchdetector;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = findViewById(R.id.image);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        pitchText = findViewById(R.id.pitchText);
        typeFrequence = findViewById(R.id.typeFrequence);
        intitule = findViewById(R.id.intitule);

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
        else if(pitchInHz <= 500 && pitchInHz > 0)
        {
            image.setImageResource(R.drawable.zone_a);
            typeFrequence.setText("Basse fréquence");
        }
        else if(pitchInHz > 500 && pitchInHz <= 1000)
        {
            image.setImageResource(R.drawable.zone_b);
            typeFrequence.setText("Moyenne fréquence");
        }
        else if(pitchInHz > 1000)
        {
            image.setImageResource(R.drawable.zone_c);
            typeFrequence.setText("Haute fréquence");
        }
    }

    public void lancerEcoute(boolean start)
    {
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);

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

        Thread audioThread = new Thread(dispatcher, "Audio Thread");

        if(start)
        {
            audioThread.start();
        }
        else
        {
            audioThread.interrupt();
        }

    }
}
