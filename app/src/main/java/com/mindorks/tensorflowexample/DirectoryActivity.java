package com.mindorks.tensorflowexample;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;


public class DirectoryActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";


    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextToSpeech tts;
    private ImageButton imageSpeaker;

    ImageView imageView;
    TextView nameResult;
    ImageButton imageButton1;
    ImageButton imageButton2;
    ImageButton imageButton3;
    ImageButton btnDes;
    Uri imageUri;
    GridLayout idGrid;
    private static final int PICK_IMAGE=100;
    Bitmap bitmap;
    Dialog dialog;
    String detail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory);
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
        imageButton1=(ImageButton) findViewById(R.id.imageButton1);
        imageButton2 = (ImageButton) findViewById(R.id.imageButton2);
        imageButton3 = (ImageButton) findViewById(R.id.imageButton3);

        imageView=(ImageView) findViewById(R.id.imageView);
        nameResult = (TextView) findViewById(R.id.nameResult);
        nameResult.setMovementMethod(new ScrollingMovementMethod());
        idGrid = (GridLayout) findViewById( R.id.idGrid );
        idGrid.setVisibility( idGrid.INVISIBLE );
        imageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                idGrid.setVisibility( idGrid.INVISIBLE );
                nameResult.setText(null);
                openGallary();
            }
        });
        imageButton2.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageUri != null)
                    detectImage();
                else
                    Toast.makeText( DirectoryActivity.this, "None of the image be selected to recognizing", Toast.LENGTH_SHORT ).show();
            }
        } );
        imageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameResult.setText(null);
                idGrid.setVisibility( idGrid.INVISIBLE );
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,123);
            }
        });
        initTensorFlowAndLoadModel();
        tts= new TextToSpeech(this, this);
        imageSpeaker=(ImageButton) findViewById(R.id.imageSpeaker);
        nameResult=(TextView) findViewById(R.id.nameResult);
        imageSpeaker.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0){
                speakOut();
            }
        });
        dialog = new Dialog( this );
        btnDes = (ImageButton) findViewById( R.id.btnDes );

        btnDes.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setContentView( R.layout.dialog_description );
                dialog.show();
                final TextView textDes;
                textDes=(TextView)dialog.findViewById(R.id.textDes);
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new InputStreamReader(getAssets().open("imagenet_comp_graph_label_strings1.txt"), "UTF-8"));
                    // do reading, usually loop until end of file reading
                    String mLine;
                    while ((mLine = reader.readLine()) != null) {
                        if(mLine.contains(detail)){
                            textDes.setText(mLine);
                            break;
                        }else {
                            textDes.setText(" Don't have Description!");
                        }
                    }
                } catch (IOException e) {
                    //log the exception
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            //log the exception
                        }
                    }
                }
                
                TextView txtClose;
                txtClose = (TextView) dialog.findViewById( R.id.txtClose );
                CircleImageView imageView1= (CircleImageView)dialog.findViewById(R.id.imageView);
                imageView1.setImageURI(imageUri);
                txtClose.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                } );
                
                ImageButton imgBtnSpeaker;
                imgBtnSpeaker=(ImageButton)dialog.findViewById(R.id.imgBtnSpeaker);

                imgBtnSpeaker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text=textDes.getText().toString();
                        tts.setSpeechRate((float) 0.7);
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                    }
                });
            }

        } );
    }
    @Override
    public  void onDestroy(){
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
    @Override
    public  void onInit(int status){
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.ENGLISH);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                imageSpeaker.setEnabled(true);
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
    private void detectImage() {
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap = Bitmap.createScaledBitmap( bitmap, 224,224, false );
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        nameResult.setText( results.toString() );
        detail=results.toString();
        idGrid.setVisibility( idGrid.VISIBLE );
    }
    private void openGallary(){
        Intent gallary=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(gallary,PICK_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK && requestCode==PICK_IMAGE){
            imageUri=data.getData();
            imageView.setImageURI(imageUri);
        }else if(resultCode==RESULT_OK && requestCode == 123){
            Bitmap bitmap=(Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "Title", null);
            imageUri= Uri.parse(path);
        }
    }
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }
    private void speakOut() {
            String text= nameResult.getText().toString();
            tts.setSpeechRate((float) 0.7);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}