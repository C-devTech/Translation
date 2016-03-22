package com.cdevtech.translation;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

/*
    To fix rendering problems, in build.gradle (Module: app):
    Original:
        compile 'com.android.support:appcompat-v7:23.2.0'
        compile 'com.android.support:design:23.2.0'

   Changed to:
        compile 'com.android.support:appcompat-v7:23.1.1'
        compile 'com.android.support:design:23.1.1'
 */

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener {

    // Define the spoken language we wish to use
    // You must install all of these on your phone for text to speech
    // Settings - Language & Input - Text-to-speech output -
    // Preferred Engine Settings - Install voice data
    private Locale currentSpokenLang = Locale.US;

    // Create the Locale objects for languages not in Android Studio
    private Locale locSpanish = new Locale("es", "MX");
    private Locale locRussian = new Locale("ru", "RU");
    private Locale locPortuguese = new Locale("pt", "BR");
    private Locale locDutch = new Locale("nl", "NL");

    // Stores all the Locales in an Array so they are easily found
    private Locale[] languages = {locDutch, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN,
            locPortuguese, locRussian, locSpanish};

    // Synthesizes text to speech
    private TextToSpeech textToSpeech;

    // Spinner for selecting the spoken language
    private Spinner languageSpinner;

    // Currently selected language in Spinner
    private int spinnerIndex = 0;

    // Will hold all the translations
    private String[] arrayOfTranslations;

    // User entered text to translate
    EditText wordsEnteredEditText;

    // Displays the translated text
    TextView translatedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        wordsEnteredEditText = (EditText) findViewById(R.id.words_entered_edit_text);
        translatedTextView = (TextView) findViewById(R.id.translated_text_view);
        textToSpeech = new TextToSpeech(this, this);

        languageSpinner = (Spinner) findViewById(R.id.lang_spinner);

        // When the Spinner is changed, update the currently selected language to speak in
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSpokenLang = languages[position];

                // Store the selected Spinner index for use elsewhere
                spinnerIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // When the app closes shutdown text to speech
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // Initializes text to speech capability
    @Override
    public void onInit(int status) {
        // Check if TextToSpeech is available
        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(currentSpokenLang);

            // If language data or a specific language isn't available error
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Text To Speech Failed", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Calls for the AsyncTask to execute when the translate JSON button is clicked
    public void onTranslateJsonClick(View view) {

        // If the user entered words to translate then get the JSON data.
        if (!isEmpty(wordsEnteredEditText)) {
            Toast.makeText(this, R.string.getting_translations_toast_text, Toast.LENGTH_LONG).show();

            // Get the text from EditText. Have to do it here since accessing the UI.
            String wordsToTranslate = wordsEnteredEditText.getText().toString().trim();

            // Calls for the method doInBackground to execute
            new SaveTheFeed().execute(wordsToTranslate);

        } else {
            // Post an error message if the didn't enter words.
            Toast.makeText(this, R.string.enter_words_toast_text, Toast.LENGTH_LONG).show();
        }
    }

    // Create an inner class that allows you to perform background operations without
    // locking up the uer interface until finished.
    // The parameters are stating that:
    //  * String - it receives a string parameters
    //  * Void - it doesn't monitor progress
    //  * Void - it won't pass a result to onPostExecute
    class SaveTheFeed extends AsyncTask<String, Void, Void> {

        // Holds JSON data in String format that will be retrieved from the web service
        String jsonString = "";

        // Will hold the translations that will be displayed on the screen
        String stringToPrint = "";

        // Everything that should execute in the background goes here
        // You cannot update the user interface from this method which runs
        // in the background.
        @Override
        protected Void doInBackground(String... params) {

            // Holds the words the user wants to translate
            String wordsToTranslate = params[0];

            // Replace the spaces in the String that was entered with + so they
            // can be passed in a URL
            wordsToTranslate = wordsToTranslate.replace(" ", "+");

            // Client used to grab data from a provided URL
            // HttpURLConnection uses the GET method by default. It will use POST if
            // setDoOutput(true) has been called. Other HTTP methods (OPTIONS, HEAD, PUT, DELETE
            // and TRACE) can be used with setRequestMethod(String)
            HttpURLConnection urlConnection = null;

            // A BufferedReader is used because it is efficient
            BufferedReader reader = null;

            try {
                // Provide the URL for the POST request
                URL url = new URL("http://newjustin.com/translateit.php?action=translations&english_words="
                        + wordsToTranslate);

                // Open a new connection as specified by the URL
                urlConnection = (HttpURLConnection) url.openConnection();

                // Define that the data expected is in JSON format
                urlConnection.setRequestProperty("Content-type", "application/json");

                // Define the request method as POST, default is POST but provides example code
                urlConnection.setRequestMethod("POST");

                // The client calls for the post request to execute and sends the results back
                // Get the content sent, allows you to input a stream of bytes from the URL
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                // The InputStreamReader converts the bytes into characters
                // The JSON data is UTF-8 so they are read with that encoding
                // 8 defines the input buffer size
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                // Storing each line of data in a StringBuilder
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                // Save the results in a String
                jsonString = sb.toString();

                // Create a JSONObject by passing the JSON data
                JSONObject jObject = new JSONObject(jsonString);

                // Get the Array named translations that contains all the translations
                JSONArray jArray = jObject.getJSONArray("translations");

                // Cycle through every translation in the array
                outputTranslations(jArray);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                // Close the bufferedReader which closes the underlying InputStreamReader,
                // closing the InputStreamReader also closes the underlying FileInputStream
                close(reader);

                // Release the URL connection
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Make the TextView scrollable
            translatedTextView.setMovementMethod(new ScrollingMovementMethod());

            // Put the translations into the TextView
            translatedTextView.setText(stringToPrint);

            // Eliminate the "language :" part of the string for the
            // translations
            String stringOfTranslations = stringToPrint.replaceAll("\\w+\\s:","#");

            // Store the translations into an array
            arrayOfTranslations = stringOfTranslations.split("#");
        }

        protected void outputTranslations(JSONArray jsonArray) {
            // Used to get the translation using a key
            String[] languages = {"arabic", "chinese", "danish", "dutch",
                    "french", "german", "italian", "portuguese", "russian",
                    "spanish"};

            // Save all teh translations by getting them with the key
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject translationObject = jsonArray.getJSONObject(i);

                    stringToPrint = stringToPrint + languages[i] + " : " +
                            translationObject.getString(languages[i]) + "\n";

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Calls for the AsyncTask to execute when the translate XML button is clicked
    public void onTranslateXmlClick(View view) {
        // If the user entered words to translate then get the JSON data.
        if (!isEmpty(wordsEnteredEditText)) {
            Toast.makeText(this, R.string.getting_translations_toast_text, Toast.LENGTH_LONG).show();

            // Get the text from EditText. Have to do it here since accessing the UI.
            String wordsToTranslate = wordsEnteredEditText.getText().toString().trim();

            // Calls for the method doInBackground to execute
            new GetXMLData().execute(wordsToTranslate);

        } else {
            // Post an error message if the didn't enter words.
            Toast.makeText(this, R.string.enter_words_toast_text, Toast.LENGTH_LONG).show();
        }
    }

    // Create an inner class that allows you to perform background operations without
    // locking up the uer interface until finished.
    // The parameters are stating that:
    //  * String - it receives a string parameters
    //  * Void - it doesn't monitor progress
    //  * Void - it won't pass a result to onPostExecute
    class GetXMLData extends AsyncTask<String, Void, Void> {

        // Holds XML data in String format that will be retrieved from the web service
        String xmlString  = "";

        // Will hold the translations that will be displayed on the screen
        String stringToPrint  = "";

        // Everything that should execute in the background goes here
        // You cannot update the user interface from this method which runs
        // in the background.
        @Override
        protected Void doInBackground(String... params) {

            // Holds the words the user wants to translate
            String wordsToTranslate = params[0];

            // Replace the spaces in the String that was entered with + so they
            // can be passed in a URL
            wordsToTranslate = wordsToTranslate.replace(" ", "+");

            // Client used to grab data from a provided URL
            // HttpURLConnection uses the GET method by default. It will use POST if
            // setDoOutput(true) has been called. Other HTTP methods (OPTIONS, HEAD, PUT, DELETE
            // and TRACE) can be used with setRequestMethod(String)
            HttpURLConnection urlConnection = null;

            // A BufferedReader is used because it is efficient
            BufferedReader reader = null;

            try {
                // Provide the URL for the POST request
                URL url = new URL("http://newjustin.com/translateit.php?action=xmltranslations&english_words="
                        + wordsToTranslate);

                // Open a new connection as specified by the URL
                urlConnection = (HttpURLConnection) url.openConnection();

                // Define that the data expected is in XML format
                urlConnection.setRequestProperty("Content-type", "text/xml");

                // Define the request method as POST, default is POST but provides example code
                urlConnection.setRequestMethod("POST");

                // The client calls for the post request to execute and sends the results back
                // Get the content sent, allows you to input a stream of bytes from the URL
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                // A BufferedReader is used because it is efficient
                // The InputStreamReader converts the bytes into characters
                // The XML data is UTF-8 so they are read with that encoding
                // 8 defines the input buffer size
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                // Storing each line of data in a StringBuilder
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                // Save the results in a String
                xmlString = sb.toString();

                // Generates an XML parser
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

                // The XML parser that is generated will support XML namespaces
                factory.setNamespaceAware(true);

                // Gathers XML data and provides information on that data
                XmlPullParser xpp = factory.newPullParser();

                // Input the XML data for parsing
                xpp.setInput(new StringReader(xmlString));

                // The event type is either START_DOCUMENT, END_DOCUMENT, START
                // END_TAG, TEXT
                int eventType = xpp.getEventType();

                // Cycle through the XML document until the document ends
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    // Each time you find a new opening tag, the event type will be START_TAG
                    // We want to skip the first tag with the name "translations"
                    if ((eventType == XmlPullParser.START_TAG) && (!xpp.getName().equals("translations"))) {

                        // getName returns the name for the current element with focus
                        stringToPrint = stringToPrint + xpp.getName() + " : ";

                        // getText returns the text for the current event
                    } else if (eventType == XmlPullParser.TEXT) {
                        line = xpp.getText().trim();
                        // Make sure that blank lines are removed before they are added
                        if (line.isEmpty() == false && !line.equals("") && !line.equals("\n")) {
                            stringToPrint = stringToPrint + line + "\n";
                        }
                    }
                    // next puts focus on the next element in the XML doc
                    eventType = xpp.next();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } finally {
                // Close the bufferedReader which closes the underlying InputStreamReader,
                // closing the InputStreamReader also closes the underlying FileInputStream
                close(reader);

                // Release the URL connection
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Make the TextView scrollable
            translatedTextView.setMovementMethod(new ScrollingMovementMethod());

            // Put the translations into the TextView
            translatedTextView.setText(stringToPrint);

            // Eliminate the "language : " part of the string for the
            // translations
            // String stringOfTranslations = stringToPrint.replaceAll("\\w+\\s:\\s","#");
            String stringOfTranslations = stringToPrint.replaceAll("\\w+\\s:\\s","");

            // Store the translations into an array
            // arrayOfTranslations = stringOfTranslations.split("#");
            arrayOfTranslations = stringOfTranslations.split("\n");
        }
    }

    // Converts speech to text
    public void acceptSpeechInput(View view) {
        // Starts an Activity that will convert speech to text
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Use a language model based on free-form speech recognition
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Recognize speech based on the default speech of device
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // Prompt the user to speak
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_input_phrase));

        try{
            startActivityForResult(intent, 100);

        } catch (ActivityNotFoundException e){

            Toast.makeText(this, getString(R.string.stt_not_supported_message), Toast.LENGTH_LONG).show();
        }
    }

    public void readTheText(View view) {
        // Set the voice to use
        textToSpeech.setLanguage(currentSpokenLang);

        // Check that translations are in the array
        if (arrayOfTranslations != null && arrayOfTranslations.length >= 9){

            // There aren't voices for our first 3 languages so skip them
            // QUEUE_FLUSH deletes previous text to read and replaces it
            // with new text
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(arrayOfTranslations[spinnerIndex + 3], TextToSpeech.QUEUE_FLUSH,
                        null, null);
            } else {
                textToSpeech.speak(arrayOfTranslations[spinnerIndex + 3], TextToSpeech.QUEUE_FLUSH,
                        null);
            }
        } else {

            Toast.makeText(this, "Translate Text First", Toast.LENGTH_SHORT).show();
        }
    }

    // The results of the speech recognizer are sent here
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        // 100 is the request code sent by startActivityForResult
        if((requestCode == 100) && (data != null) && (resultCode == RESULT_OK)){

            // Store the data sent back in an ArrayList
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            // Put the spoken text in the EditText
            wordsEnteredEditText.setText(spokenText.get(0));
        }
    }

    // Check if the user entered words to translate
    // Returns false if not empty
    protected boolean isEmpty(EditText editText) {
        // Get the text in the EditText, convert it into a string, and delete white space
        // then check length
        return editText.getText().toString().trim().length() == 0;
    }

    // Gracefully close that which is closeable
    public static void close(Closeable c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
