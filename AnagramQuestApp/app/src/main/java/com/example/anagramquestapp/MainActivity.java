package com.example.anagramquestapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
    implements AdapterView.OnItemSelectedListener{

    private String selectedDictionary;
    private Game game;
    /* Ip address of the web server that hosts the generation of anagram sequences */
    private static final String IPaddress = "192.168.1.76";
    /* On emulator use 10.0.2.2 ip address to access host machine address ip  */
    private static final String URLtoDictionaries = "http://"+IPaddress+":8888/dictionaries";
    private final String URLtoGenerateAnagramSequence = "http://"+IPaddress+":8888/game/:dictionary/:n";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ProgressBar simpleProgressBar = (ProgressBar) findViewById(R.id.waitForAnagramProgressBar);
        simpleProgressBar.setVisibility(View.INVISIBLE);

        Log.i("URLs","URL to dictionnaries : " + URLtoDictionaries);
        Log.i("URLs","URL to anagram generator : "+ URLtoGenerateAnagramSequence);

        /* Creating of an Array of dictionaries for the app to use */
        ArrayAdapter dictAdapter = new ArrayAdapter(
                this,
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                new ArrayList());

        /* Creation of a spinner to display and select a dictionary to use */
        Spinner dictionarySelect = findViewById(R.id.dictionarySelect);
        dictionarySelect.setOnItemSelectedListener(this);
        dictionarySelect.setAdapter(dictAdapter);

        /* Sending Request to fetch list of dictionaries available */
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(URLtoDictionaries, response -> {
            for (int i = 0; i < response.length(); i++) {
                try {

                    dictAdapter.add(response.getJSONObject(i).getString("language"));

                } catch (JSONException e) {
                    Log.e("JsonRequest", e.toString());
                }
            }
        }, error -> Toast.makeText(MainActivity.this, "Could not connect to : "+URLtoDictionaries+" to fetch dictionary", Toast.LENGTH_SHORT).show()
        );

        queue.add(jsonArrayRequest);
        /* Creation of a Button to send a request to generate a sequence of anagrams */
        Button generateAnagramButton = findViewById(R.id.generateAnagramButton);
        generateAnagramButton.setOnClickListener(
                view -> {
                    EditText maxNumberOfLettersInput = (EditText) findViewById(R.id.maxNumberOfLettersInput);
                    String maxNumberOfLettersString = maxNumberOfLettersInput.getText().toString();

                    Log.i("dict", "Selected dictionary : " +selectedDictionary);

                    int maxNumberOfLetters;

                    try {
                        maxNumberOfLetters = Integer.parseInt(maxNumberOfLettersString);
                        Log.i("maxNumberOfLetters",Integer.toString(maxNumberOfLetters));
                    }catch (NumberFormatException e){
                        Toast.makeText(MainActivity.this, "Please enter a number for the max number of letters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(maxNumberOfLetters<2){
                        Toast.makeText(MainActivity.this, "Please selected a number higher than 2", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(selectedDictionary==null){
                        Toast.makeText(MainActivity.this, "No dictionary selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String url = URLtoGenerateAnagramSequence.replace(":dictionary",selectedDictionary).replace(":n",maxNumberOfLettersString);

                    ArrayList<ArrayList<String>> anagramSequence = new ArrayList<>();
                    JsonArrayRequest jsonArrayRequest1 = new JsonArrayRequest(url, response -> {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                String[] words = response.getJSONObject(i).get("words").toString().split(",");
                                for(int j=0; j<words.length; j++){
                                    words[j] = words[j].toLowerCase();
                                }
                                anagramSequence.add(new ArrayList<>(Arrays.asList(words)));

                            } catch (JSONException e) {
                                Log.e("JsonRequest", e.toString());
                            }
                        }
                        game = new Game(anagramSequence);
                        if(!game.isGameStarted()){
                            Toast.makeText(MainActivity.this, "Anagram sequence is empty, please try to generate a new sequence", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        simpleProgressBar.setVisibility(View.INVISIBLE);

                        Log.i("Sequence",game.printAnagramSequence());
                        showAnagramToGuess();
                    }, error -> Toast.makeText(MainActivity.this, "Could not connect to : "+url+" to generate an anagram sequence", Toast.LENGTH_SHORT).show()
                    );
                    int socketTimeout = 120000;//30 seconds - change to what you want
                    RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
                    jsonArrayRequest1.setRetryPolicy(policy);

                    simpleProgressBar.setVisibility(View.VISIBLE);
                    queue.add(jsonArrayRequest1);

                }
        );


        Button guessButton = findViewById(R.id.guessButton);
        guessButton.setOnClickListener(view -> {
            EditText guessInput = findViewById(R.id.guessInput);
            String guessString = guessInput.getText().toString();

            if(game==null){
                return;
            }
            if(!game.isGameStarted()){
                return;
            }

            Log.i("Guess","Guess : "+guessString+" is correct ? "+game.answerIsCorrect(guessString));
            if(game.answerIsCorrect(guessString)){

                Toast.makeText(MainActivity.this, "Correct answer !" ,Toast.LENGTH_LONG).show();

                if(!game.setNextAnagramToGuess()){

                    Toast.makeText(MainActivity.this, "You WIN !" ,Toast.LENGTH_LONG).show();
                    TextView anagramTextView = findViewById(R.id.anagramToGuess);
                    anagramTextView.setText("");
                    Intent myIntent = new Intent(MainActivity.this, WinActivity.class);
                    MainActivity.this.startActivity(myIntent);


                }else{
                    showAnagramToGuess();
                }
            }else{
                Toast.makeText(MainActivity.this, "Wrong ! Try again !" ,Toast.LENGTH_LONG).show();
            }

        });


    }
    /* Methods of the Spinner */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

        selectedDictionary = adapterView.getSelectedItem().toString();
        Log.i("MyAPP","dictionary selected"+ selectedDictionary);

    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    private void showAnagramToGuess(){

        TextView anagramTextView = findViewById(R.id.anagramToGuess);
        anagramTextView.setText(game.getCurrentAnagramToGuess());

    }

}