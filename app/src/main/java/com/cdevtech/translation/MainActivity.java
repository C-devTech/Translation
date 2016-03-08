package com.cdevtech.translation;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/*
    To fix rendering problems, in build.gradle (Module: app):
    Original:
        compile 'com.android.support:appcompat-v7:23.2.0'
        compile 'com.android.support:design:23.2.0'

   Changed to:
        compile 'com.android.support:appcompat-v7:23.1.1'
        compile 'com.android.support:design:23.1.1'
 */

public class MainActivity extends AppCompatActivity {

    EditText translatedEditText;
    TextView translationTextView;

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

        translatedEditText = (EditText) findViewById(R.id.editText);
        translationTextView = (TextView) findViewById(R.id.translateTextView);
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

    // Calls for the AsyncTask to execute when the translate button is clicked
    public void onTranslateClick(View view) {

        // If the user entered words to translate then get the JSON data.
        if (!isEmpty(translatedEditText)) {
            Toast.makeText(this, R.string.getting_translations_toast_text, Toast.LENGTH_LONG).show();

            // Get the text from EditText. Have to do it here since accessing the UI.
            String wordsToTranslate = translatedEditText.getText().toString().trim();

            // Calls for the method doInBackground to execute
            new SaveTheFeed().execute(wordsToTranslate);

        } else {
            // Post an error message if the didn't enter words.
            Toast.makeText(this, R.string.enter_words_toast_text, Toast.LENGTH_LONG).show();
        }
    }

    // Check if the user entered words to translate
    // Returns false if not empty
    protected boolean isEmpty(EditText editText) {
        // Get the text in the EditText, convert it into a string, and delete white space
        // then check length
        return editText.getText().toString().trim().length() == 0;
    }

    // Create an inner class that allows you to perform background operations without
    // locking up the uer interface until finished.
    // The parameters are stating that:
    //  * String - it receives a string parameters
    //  * Void - it doesn't monitor progress
    //  * Void - it won't pass a result to onPostExecute
    class SaveTheFeed extends AsyncTask<String, Void, Void> {

        // Holds JSON data in String format, initially empty
        String jsonString = "";

        // Will hold the translations that will be displayed on the screen
        String result = "";

        // Everything that should execute in the background goes here
        // You cannot update the user interface from this method which runs
        // in the background.
        @Override
        protected Void doInBackground(String... params) {
            String wordsToTranslate = params[0];

            // Replace the spaces in the String that was entered with + so they
            // can be passed in a URL
            wordsToTranslate = wordsToTranslate.replace(" ", "+");

            // Client used to grab data from a provided URL
            // HttpURLConnection uses the GET method by default. It will use POST if
            // setDoOutput(true) has been called. Other HTTP methods (OPTIONS, HEAD, PUT, DELETE
            // and TRACE) can be used with setRequestMethod(String)
            HttpURLConnection urlConnection = null;

            // Provides the URL for the post request
            URL url = null;

            // Allows you to input a stream of bytes from the URL
            InputStream inputStream = null;

            try {
                // Provide the URL for the POST request
                url = new URL("http://newjustin.com/translateit.php?action=translations&english_words="
                        + wordsToTranslate);

                // Open a new connection as specified by the URL
                urlConnection = (HttpURLConnection) url.openConnection();

                // Define that the data expected is in JSON format
                urlConnection.setRequestProperty("Content-type", "application/json");

                // Define the request method as POST, default is POST but provides example code
                urlConnection.setRequestMethod("POST");

                // The client calls for the post request to execute and sends the results back
                // Get the content sent
                inputStream = new BufferedInputStream(urlConnection.getInputStream());

                // A BufferedReader is used because it is efficient
                // The InputStreamReader converts the bytes into characters
                // The JSON data is UTF-8 so they are read with that encoding
                // 8 defines the input buffer size
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"), 8);

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
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            // Put the translations into the TextView
            translationTextView.setText(result);
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

                    result = result + languages[i] + " : " +
                            translationObject.getString(languages[i]) + "\n";

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
