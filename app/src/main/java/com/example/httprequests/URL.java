package com.example.httprequests;

import java.util.ArrayList;
import java.util.List;

public class URL {
    private int id;
    private String urlString;
    private List<Data> data;

    public URL(int id, String urlString) {
        this.id = id;
        this.urlString = urlString;
        this.data = new ArrayList<>();
    }

    public URL(int id, String urlString, List<Data> data) {
        this.id = id;
        this.urlString = urlString;
        this.data = data;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }
}
