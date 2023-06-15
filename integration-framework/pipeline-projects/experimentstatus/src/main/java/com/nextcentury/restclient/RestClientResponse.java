package com.nextcentury.restclient;

public class RestClientResponse {
    private int statusCode = -1;

    private String response = new String();

    public RestClientResponse(String response, int statusCode){
        this.response = response;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponse() {
        return response;
    }
}