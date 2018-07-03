package cn.edu.sjtu.iiot.system.batchqr.utils;

import okhttp3.OkHttpClient;

public class NetworkClient {
    private static OkHttpClient mClient = null;

    public static OkHttpClient getClient(){
        if(mClient == null){
            mClient = new OkHttpClient();
        }
        return mClient;
    }
}
