package com.example.httprequests;

public class Data {
    private int id;
    private String dataString;
    private String dataValue;
    private int urlId;

    public Data(int id, String dataString, int urlId) {
        this.id = id;
        this.dataString = dataString;
        this.urlId = urlId;
        this.dataValue = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDataString() {
        return dataString;
    }

    public void setDataString(String dataString) {
        this.dataString = dataString;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public int getUrlId() {
        return urlId;
    }

    public void setUrlId(int urlId) {
        this.urlId = urlId;
    }
}
