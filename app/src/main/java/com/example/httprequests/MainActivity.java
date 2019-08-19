package com.example.httprequests;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private DatabaseHelper databaseHelper;
    private List<URL> urls;
    private URL currentURL;
    private Spinner urlSpinner;
    Button addDataButton;
    private String[] methodsArray;
    private String currentMethod;
    private enum Action{ADD_URL, UPDATE_URL, DELETE_URL, ADD_DATA, DELETE_DATA, CLEAR_DATABASE}
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this, null, null, 0);

        bindButtons();
        populateURLSpinner(0);

        setMethodsArray(getResources().getStringArray(R.array.methods_array));
        populateMethodSpinner();

        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.addURLButton:
                addURL();
                break;

            case R.id.updateURLButton:
                updateURL();
                break;

            case R.id.deleteURLButton:
                deleteURL();
                break;

            case R.id.addDataButton:
                addData(getCurrentURL());
                break;

            case R.id.clearDatabaseButton:
                clearDatabase();
                break;

            case R.id.executeMethodButton:
                executeRequest();
                break;

            default:
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(parent.getId()) {
            case R.id.urlSpinner:
                setCurrentURL(urls.get(position));
                setData(getCurrentURL());
                break;

            case R.id.methodSpinner:
                TextView responseTextView = findViewById(R.id.responseTextView);
                setCurrentMethod(methodsArray[position]);
                responseTextView.setText(getCurrentMethod());
                break;

            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**************************************************************************
     * Helper methods.
     *************************************************************************/
    private void bindButtons() {
        Button addURLButton = findViewById(R.id.addURLButton);
        addURLButton.setOnClickListener(this);

        Button updateURLButton = findViewById(R.id.updateURLButton);
        updateURLButton.setOnClickListener(this);

        Button deleteURLButton = findViewById(R.id.deleteURLButton);
        deleteURLButton.setOnClickListener(this);

        addDataButton = findViewById(R.id.addDataButton);
        addDataButton.setOnClickListener(this);

        Button executeMethodButton = findViewById(R.id.executeMethodButton);
        executeMethodButton.setOnClickListener(this);

        Button clearDatabaseButton = findViewById(R.id.clearDatabaseButton);
        clearDatabaseButton.setOnClickListener(this);
    }

    private void createAlertDialog(String title, String message, final Action action) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(title);
        alert.setMessage(message);

        // Only add the EditText if necessary.
        final EditText editText = new EditText(this);
        switch(action) {
            case ADD_URL:
                final String urlPrefix = getResources().getString(R.string.url_prefix);
                editText.setText(urlPrefix);
                alert.setView(editText);
                break;

            case UPDATE_URL:
                editText.setText(getCurrentURL().getUrlString());
                alert.setView(editText);
                break;

            case ADD_DATA:
                alert.setView(editText);
                break;

            default:
                break;
        }

        alert.setPositiveButton(title, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String response = "";
                switch (action) {
                    case ADD_URL:
                        response = databaseHelper.createURL(editText.getText().toString());
                        populateURLList();
                        populateURLSpinner(urls.size() - 1);
                        break;

                    case UPDATE_URL:
                        response = databaseHelper.updateURL(getCurrentURL(), editText.getText().toString());
                        populateURLSpinner(urlSpinner.getSelectedItemPosition());
                        break;

                    case DELETE_URL:
                        response = databaseHelper.deleteURL(getCurrentURL());
                        populateURLSpinner(0);
                        break;

                    case ADD_DATA:
                        response = databaseHelper.createData(getCurrentURL(), editText.getText().toString());
                        populateURLSpinner(getURLPosition(getCurrentURL()));
                        break;

                    case DELETE_DATA:

                        break;

                    case CLEAR_DATABASE:
                        SQLiteDatabase database = databaseHelper.getWritableDatabase();
                        response = databaseHelper.clearTables(database);
                        database.close();
                        populateURLSpinner(0);
                        break;

                    default:
                        response = "Ain't done nothin'.";
                        break;
                }
                Toast toast = Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alert.show();
    }

    private void populateURLSpinner(int position) {
        populateURLList();

        // Create a list of strings from the URLs list.
        List<String> urlStrings = new ArrayList<>();
        for(int i = 0; i < urls.size(); i++) {
            String url = urls.get(i).getUrlString();
            urlStrings.add(url);
        }

        // Set the spinner adapter and strings from the urls strings list.
        urlSpinner = findViewById(R.id.urlSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, urlStrings);
        urlSpinner.setAdapter(arrayAdapter);
        urlSpinner.setOnItemSelectedListener(this);

        // Set the spinner's position.
        position = position < urls.size() ? position : 0;
        urlSpinner.setSelection(position);
    }

    private void populateMethodSpinner() {
        Spinner methodSpinner = findViewById(R.id.methodSpinner);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.methods_array, android.R.layout.simple_spinner_item);
        methodSpinner.setAdapter(arrayAdapter);
        methodSpinner.setOnItemSelectedListener(this);
        setCurrentMethod(methodsArray[0]);
    }

    // Set the data for a given URL.
    private void setData(URL url) {
        setCurrentURL(url);
        final URL finalURL = url;

        // Outer LinearLayout.
        LinearLayout linearLayout = findViewById(R.id.dataLinearLayout);
        linearLayout.removeAllViews();

        for(int i = 0; i < url.getData().size(); i++) {
           final Data datum = url.getData().get(i);

           // Add a button to delete each datum.
           Button button = new Button(this);
           button.setText(getResources().getString(R.string.delete_data_button));
           button.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   databaseHelper.deleteData(finalURL, datum);
                   Toast toast = Toast.makeText(getApplicationContext(),
                           "Deleted " + datum.getDataString() + " from " + finalURL.getUrlString() + ".",
                           Toast.LENGTH_SHORT);
                   populateURLSpinner(getURLPosition(finalURL));
               }
           });

           // Horizontal LinearLayout to hold each datum's TextView for its key, EditText for its
           // value, and its delete button.
           LinearLayout innerLinearLayout = new LinearLayout(this);
           innerLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
           innerLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
           innerLinearLayout.addView(button);

           TextView dataTextView = new TextView(this);
           dataTextView.setText(url.getData().get(i).getDataString());
           innerLinearLayout.addView(dataTextView);

           EditText dataEditText = new EditText(this);
           dataEditText.addTextChangedListener(new TextWatcher() {
               @Override
               public void beforeTextChanged(CharSequence s, int start, int count, int after) {

               }

               @Override
               public void onTextChanged(CharSequence s, int start, int before, int count) {
                   datum.setDataValue(s.toString());
               }

               @Override
               public void afterTextChanged(Editable s) {
                    datum.setDataValue(s.toString());
               }
           });
           innerLinearLayout.addView(dataEditText);

           linearLayout.addView(innerLinearLayout);
        }
        // Tack the add data button on at the end.
        linearLayout.addView(addDataButton);
    }

    private int getURLPosition(URL url) {
        int position = 0;
        for(int i = 0; i < urls.size(); i++) {
            if(url.equals(urls.get(i))) {
                position = i;
                break;
            }
        }
        return position;
    }

    private void executeRequest() {
        switch(getCurrentMethod()) {
            case "GET String":
                getVolleyString();
                break;

            case "GET JSON":
                getJSON();
                break;

            case "POST JSON":
                postJSON();
                break;

            case "UPDATE JSON":
                updateJSON();
                break;

            case "DELETE JSON":
                deleteJSON();
                break;

            default:
                break;
        }

    }

    /**************************************************************************
     * Database interaction methods.
     *************************************************************************/
    private void populateURLList() {
        this.urls = new ArrayList<>();
        this.urls = databaseHelper.readAllURLs();
    }

    /* TODO If your enum was a class, you could probably condense these.*/

    private void addURL() {
        final String title = "Add URL";
        final String message = "Enter a URL to add: ";
        final Action action = Action.ADD_URL;
        createAlertDialog(title, message, action);
    }

    private void updateURL() {
        final String title = "Update URL";
        final String message = "Update URL " + getCurrentURL().getUrlString() + "?";
        final Action action = Action.UPDATE_URL;
        createAlertDialog(title, message, action);
    }

    private void deleteURL() {
        final String title = "Delete URL";
        final String message = "Delete URL " + getCurrentURL().getUrlString() + "?";
        final Action action = Action.DELETE_URL;
        createAlertDialog(title, message, action);
    }

    private void addData(URL url) {
        final String title = "Add datum";
        final String message = "Enter a datum to add: ";
        final Action action = Action.ADD_DATA;
        createAlertDialog(title, message, action);
        setData(getCurrentURL());
    }

    private void deleteData(URL url, Data datum) {

    }

    private void clearDatabase() {
      final String title = "Clear Database";
      final String message = "Do you really want to delete all URLs and Data from the database?";
      final Action action = Action.CLEAR_DATABASE;
      createAlertDialog(title, message, action);
    }

    /**************************************************************************
     * Volley methods.
     *************************************************************************/
    private void getVolleyString() {
        final TextView responseTextView = findViewById(R.id.responseTextView);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, getCurrentURL().getUrlString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        responseTextView.setText(response);
                        responseTextView.setMovementMethod(new ScrollingMovementMethod());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                responseTextView.setText(error.toString());
            }
        });
        requestQueue.add(stringRequest);
    }

    private void getJSON() {
        final TextView responseTextView = findViewById(R.id.responseTextView);
        responseTextView.setText("Get JSON");
    }

    private void postJSON() {
        final TextView responseTextView = findViewById(R.id.responseTextView);
        responseTextView.setText("");

        final URL url = getCurrentURL();

        JSONObject jsonObject = new JSONObject();

        try {
            for(int i = 0; i < url.getData().size(); i++) {
                Data datum = url.getData().get(i);
                if (datum.getDataValue() != null) {
                    jsonObject.put(datum.getDataString(), datum.getDataValue());
                }
            }

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url.getUrlString(), jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        responseTextView.setText(response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                responseTextView.setText(error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        requestQueue.add(request);
    }

    // Currently have no need to implement update.
    private void updateJSON() {
        final TextView responseTextView = findViewById(R.id.responseTextView);
        final URL url = getCurrentURL();

        responseTextView.setText("");
        for(int i = 0; i < url.getData().size(); i++) {
            Data datum = url.getData().get(i);
            if (datum.getDataValue() != null) {
                responseTextView.append(datum.getDataString() + " : " + datum.getDataValue() + "\n");
            }
        }

    }

    // Currently have no need to implement delete.
    private void deleteJSON() {
        final TextView responseTextView = findViewById(R.id.responseTextView);
        responseTextView.setText("Delete JSON");
    }

    /**************************************************************************
     * Get and set methods.
     *************************************************************************/
    public URL getCurrentURL() {
        return currentURL;
    }

    public void setCurrentURL(URL currentURL) {
        this.currentURL = currentURL;
    }

    public void setMethodsArray(String[] methodsArray) {
        this.methodsArray = methodsArray;
    }

    public String getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(String currentMethod) {
        this.currentMethod = currentMethod;
    }
}
