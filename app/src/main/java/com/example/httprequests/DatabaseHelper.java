package com.example.httprequests;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database name.
    private static final String DATABASE_NAME = "URLDatabase";

    // Shared columns.
    private static final String ID = "id";

    // URLs table.
    private static final String URLS_TABLE_NAME = "URLs";
    private static final String URL_STRING = "url";

    // Data table.
    private static final String DATA_TABLE_NAME = "Data";
    private static final String DATA_STRING = "data";
    private static final String DATA_URL_ID = "urlId";


    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /******************************************************
     * Database methods.
     ******************************************************/
    private void createTables(SQLiteDatabase db) {
        final String CREATE_URL_TABLE = "CREATE TABLE IF NOT EXISTS " + URLS_TABLE_NAME + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                URL_STRING + " VARCHAR(255) NOT NULL);";


        final String CREATE_DATA_TABLE = "CREATE TABLE IF NOT EXISTS " + DATA_TABLE_NAME + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DATA_STRING + " VARCHAR(255) NOT NULL, " +
                DATA_URL_ID + " INTEGER, " +
                "FOREIGN KEY (" + DATA_URL_ID + ")" + " REFERENCES " + URLS_TABLE_NAME + "(" + ID + "));";


        db.execSQL(CREATE_URL_TABLE);
        db.execSQL(CREATE_DATA_TABLE);
    }

    private void dropTables(SQLiteDatabase db) {
        final String DROP_DATA_TABLE = "DROP TABLE IF EXISTS " + DATA_TABLE_NAME + ";";
        final String DROP_URL_TABLE = "DROP TABLE IF EXISTS " + URLS_TABLE_NAME + ";";

        db.execSQL(DROP_DATA_TABLE);
        db.execSQL(DROP_URL_TABLE);
    }

    public String clearTables(SQLiteDatabase db) {
        final String response = "All URLs and data have been removed from the database.";
        dropTables(db);
        createTables(db);

        return response;
    }

    /******************************************************
     * URL CRUD methods.
     ******************************************************/
    // Create.
    public String createURL(String url) {
        String response;

        if(isEmpty(url)) {
            response = "Empty URLs are not allowed.";
        }

        else if(isNotUnique(url, getURLStrings(readAllURLs()))) {
            response = url + " was not added. " + url + " already exists in the database.";
        }

        else {
            SQLiteDatabase database = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(URL_STRING, url);
            database.insert(URLS_TABLE_NAME, null, contentValues);
            database.close();

            response = "Added " + url + " to the database.";
        }
        return response;
    }

    // Read.
    public List<URL> readAllURLs() {
        List<URL> urls = new ArrayList<>();
        final String query = "SELECT * FROM " + URLS_TABLE_NAME;
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor cursor = database.rawQuery(query, null);

        while(cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String urlString = cursor.getString(1);
            URL url = new URL(id, urlString);

            List<Data> data = readURLData(url);
            if(!data.isEmpty()) {
                url.setData(data);
            }
            urls.add(url);
        }

        cursor.close();
        database.close();
        return urls;
    }

    // Update.
    public String updateURL(URL url, String updateURL) {
        String response;
        if(isEmpty(updateURL)) {
            response = "Unable to update " + url.getUrlString() + ". " +
                    "Empty URLs are not allowed.";
        }
        else if(isNotUnique(updateURL, getURLStrings(readAllURLs()))) {
            response = "Unable to update " + url.getUrlString() + " with " + updateURL +
                    ". " + updateURL + " already exists in the database.";
        }

        else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(URL_STRING, updateURL);
            SQLiteDatabase database = this.getWritableDatabase();
            database.update(URLS_TABLE_NAME, contentValues, ID + "=" + url.getId(), null);
            response = "Updated " + url.getUrlString() + " with " + updateURL + ".";
        }
        return response;
    }

    // Delete
    public String deleteURL(URL url) {
        String response ="";
        SQLiteDatabase database = getWritableDatabase();
        List<Data> data = readURLData(url);
        database.close();

        // Delete all data that belongs to the URL before deleting the URL.
        for(int i = 0; i < data.size(); i++) {
            String deletedData = deleteData(url, data.get(i));
        }

        database = getWritableDatabase();
        final String query = "DELETE FROM " + URLS_TABLE_NAME +
                " WHERE " + URLS_TABLE_NAME + "." + ID + " = " + url.getId();
        database.execSQL(query);
        database.close();

        response = "Deleted " + url.getUrlString() + " from the database.";

        return response;
    }

    /******************************************************
     * Data CRUD methods.
     ******************************************************/
    // Create.
    public String createData(URL url, String datum) {
        String response;
        if(isEmpty(datum)) {
            response = "Empty data are not allowed.";
        }

        else if(isNotUnique(datum, getDataStrings(url))) {
            response = datum + " already exist for " + url.getUrlString() + ".";
        }
        else {
            SQLiteDatabase database = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(DATA_STRING, datum);
            contentValues.put(DATA_URL_ID, url.getId());
            database.insert(DATA_TABLE_NAME, null, contentValues);
            database.close();

            response = "Added " + datum + " to URL, " + url.getUrlString() + ".";
        }
        return response;
    }

    // Read.
    public List<Data> readURLData(URL url) {
        List<Data> data = new ArrayList<>();
        final String query = "SELECT * FROM " + DATA_TABLE_NAME + " WHERE " + DATA_URL_ID + " = " + url.getId();
        SQLiteDatabase database = this.getReadableDatabase();
        Cursor cursor = database.rawQuery(query, null);

        while(cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String dataString = cursor.getString(1);
            Data datum = new Data(id, dataString, url.getId());
            data.add(datum);
        }

        cursor.close();
        database.close();

        return data;
    }

    // Update.
    // Probably won't need to write this method.

    // Delete.
    public String deleteData(URL url, Data datum) {
        String response;
        final String query = "DELETE FROM " + DATA_TABLE_NAME +
                " WHERE " + DATA_URL_ID + " = " + url.getId() +
                " AND " + DATA_TABLE_NAME + "." + ID + " = " + datum.getId();

        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL(query);
        database.close();

        response = "Deleted " + datum.getDataString() + " from URL: " + url.getUrlString() + ".";
        return response;
    }

    /******************************************************
     * Helper methods.
     ******************************************************/
    private boolean isNotUnique(String stringToCheck, List<String> stringList) {
        for(int i = 0; i < stringList.size(); i++) {
            if(stringToCheck.equals(stringList.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmpty(String stringToCheck) {
        String emptyString = "";
        return emptyString.equals(stringToCheck);
    }

    private List<String> getURLStrings(List<URL> urls) {
        List<String> urlStrings = new ArrayList<>();

        for(int i = 0; i < urls.size(); i++) {
            String urlString = urls.get(i).getUrlString();
            urlStrings.add(urlString);
        }
        return urlStrings;
    }

    private List<String> getDataStrings(URL url) {
        List<Data> dataList = url.getData();
        List<String> dataStrings = new ArrayList<>();

        for(int i = 0; i < dataList.size(); i++) {
            String datumString = dataList.get(i).getDataString();
            dataStrings.add(datumString);
        }
        return dataStrings;
    }

}
