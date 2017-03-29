package com.yeepay.example;

import org.springframework.stereotype.Component;

/**
 * Created by yp-tc-m-7163 on 2017/3/28.
 *
 */
@Component
public class Database {

    private boolean connected = false;
    private String Url = "http://172.17.106.189:9200";

    public boolean isConnected() {
        return connected;
    }

    public String getUrl() {
        return Url;
    }
}
