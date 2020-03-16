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
    private TextView frequence;
    private TextView typeFrequence;
    private TextView intitule;
    private Thread audioThread;
    private AudioDispatcher dispatcher;

    // Tag qui sera utilisé pour afficher des messages dans la console pour le débogage
    private static final String LOG_TAG = "AudioRecordTest";

    // Code arbitraire qui sera utilisé lors de la demande de droits et de la réception de la réponse
    private static final int REQUETE_PERMISSION_ENRERISTRER_AUDIO = 1;

    private boolean permissionDEnregistrerAccordee = false;

    // Permissions à demander. Ici, seulement la permission pour enregistrer le son provenant du micro.
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    /* Méthode qui s'exécute lorsque l'on reçoit les résultats de toutes les demandes
    de permission de l'application. Ici, nous ne nous soucions que de la permission pour
    enregistrer le son provenant du micro. Sans cette permission accordée, l'application se ferme. */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode)
        {
            case REQUETE_PERMISSION_ENRERISTRER_AUDIO:
                permissionDEnregistrerAccordee  = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;

            // On devrait rajouter des case si on avait d'autres permissions à demander
        }

        if (!permissionDEnregistrerAccordee)
        {
            // Envoi d'un message d'erreur dans la console puis fermeture de l'appli
            Log.e(LOG_TAG, "Permission d'enregistrer non approuvée");
            finish();
        }

    }

    // Méthode qui s'exécute au lancement de l'application
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Liaison de la classe à ses ressources XML
        setContentView(R.layout.activity_main);

        // Demande la permission d'enregistrer du son
        ActivityCompat.requestPermissions(this, permissions, REQUETE_PERMISSION_ENRERISTRER_AUDIO);

        // Association des variables à leurs ressources dans le XML
        image = findViewById(R.id.image);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        frequence = findViewById(R.id.frequence);
        typeFrequence = findViewById(R.id.typeFrequence);
        intitule = findViewById(R.id.intitule);

        btnStop.setEnabled(false);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            // Méthode qui s'exécute quand on clique sur le bouton start
            public void onClick(View v) {
                intitule.setText("Enregistrement en cours");
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                lancerEcoute(true);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            // Méthode qui s'exécute quand on clique sur le bouton stop
            public void onClick(View v) {
                intitule.setText("En attente");
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
                lancerEcoute(false);
            }
        });
    }

    // Démarre ou stoppe la récupération et le traitement de la fréquence
    public void lancerEcoute(boolean demarrerEnregistrement)
    {
        if(demarrerEnregistrement)
        {
            // Relie l'AudioDispatcher à l'entrée par défaut du smartphone (micro)
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);

            // Création d'un gestionnaire de détection de fréquence
            PitchDetectionHandler pdh = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e){

                    /* Récupère le fréquence fondamentale du son capté par le micro en Hz.
                       Renvoie -1 si aucun son n'est capté. */
                    final float frequenceDetectee = res.getPitch();

                    /* Le traitement de la fréquence se fait dans la méthode runOnUiThread car notre traitement
                    implique des changements dans l'interface (affichage de la fréquence par exemple)
                    qui ne sont faisable qu'en accédant au thread de l'interface utilisateur (UiThread)*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            traiterFrequence(frequenceDetectee);
                        }
                    });
                }
            };

            /* Ajout du gestionnaire de détection au dispatcher.
            La détection se fera en suivant l'agorithme de Yin */
            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
            dispatcher.addAudioProcessor(pitchProcessor);

            // On lance le dispatcher dans un thread à part
            audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();
        }
        else
        {
            // Arrêt de la récupération du son
            audioThread.interrupt();
            dispatcher.stop();
        }

    }

    // Détermine ce que l'on affiche en fonction de la fréquence détectée
    public void traiterFrequence(float frequenceDetectee)
    {
        if(frequenceDetectee == -1)
        {
            image.setImageResource(R.drawable.initial);
            frequence.setText("Aucun son audible");
            typeFrequence.setText("");
        }
        else
        {
            frequence.setText(frequenceDetectee + " Hz");

            if(frequenceDetectee > 0 && frequenceDetectee <= 150)
            {
                image.setImageResource(R.drawable.zone_a);
                typeFrequence.setText("Basse fréquence");
            }
            else if(frequenceDetectee > 150 && frequenceDetectee <= 250)
            {
                image.setImageResource(R.drawable.zone_b);
                typeFrequence.setText("Moyenne fréquence");
            }
            else
            {
                image.setImageResource(R.drawable.zone_c);
                typeFrequence.setText("Haute fréquence");
            }
        }
    }
}