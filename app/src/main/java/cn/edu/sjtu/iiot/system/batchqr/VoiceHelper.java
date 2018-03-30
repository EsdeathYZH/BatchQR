package cn.edu.sjtu.iiot.system.batchqr;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.channels.AcceptPendingException;

/**
 * Created by SHIYONG on 2018/3/26.
 */

public class VoiceHelper {
    private Context mContext;
    private com.iflytek.cloud.SpeechRecognizer mIat;// 语音听写
    private RecognizerDialog iatDialog;//听写动画
    private int count = 0;
    private String mGrammarId=null;

    private String result = "";

    public void VoiceHelper(Context context){
        mContext = context;
        SpeechUtility.createUtility(context, SpeechConstant.APPID + "=59c1dfaa");
        // 语音听写1.创建SpeechRecognizer对象，第二个参数：本地听写时传InitListener
        mIat = SpeechRecognizer.createRecognizer(context, null);
        // 2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
        // 语音识别应用领域（：iat，search，video，poi，music）
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        // 接收语言中文
        mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");

        // 设置听写引擎（云端）
        //mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 无标点符号
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");

        //开始录入音频后，音频后面部分最长静音时长为2500ms
        mIat.setParameter(SpeechConstant.VAD_EOS, "2500");

        //mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        //mIat.buildGrammar("bnf", readBnfFile() ,	grammarListener);
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mIat.setParameter(SpeechConstant.ASR_NBEST, "5");
        mIat.setParameter(SpeechConstant.ASR_WBEST, "5");
    }

    //构建语法监听器
    private GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if(error == null && !TextUtils.isEmpty(grammarId) ){
                //构建语法成功，请保存grammarId用于识别
                mGrammarId=grammarId;

            }else{
                Log.i("BuildGrammarError", "ErrorCode:"+error.getErrorCode());
            }
        }
    };

    private RecognizerListener mRecognizerListener = new RecognizerListener(){

        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d("Result:",results.getResultString ());
            result = parseResult(results);

        }
        //会话发生错误回调接口
        public void onError(SpeechError error) {
            error.getPlainDescription(true); //获取错误码描述
        }
        //开始录音
        public void onBeginOfSpeech() {}
        //音量值0~30
        public void onVolumeChanged(int volume, byte[] bytes){}
        //结束录音
        public void onEndOfSpeech() {}
        //扩展用接口
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {}
    };

    //合法语句: ... mth row ... nth column ...
    private String parseResult(RecognizerResult results) {
        String text = parseIatResult(results.getResultString());
        String[] parsed_text = text.split(" ");
        int row_num_pointer = -1;
        int col_num_pointer = -1;
        int row_num = -1;
        int col_num = -1;
        for(int i=0; i<parsed_text.length; i++) {
            if (parsed_text[i].compareTo("row") == 0 )
                row_num_pointer = i + 1;
            if (parsed_text[i].compareTo("column") == 0 )
                col_num_pointer = i + 1;
        }

        //检测到合法语句
        if(row_num_pointer!=-1 && row_num_pointer!=parsed_text.length && col_num_pointer!=-1 && col_num_pointer!=parsed_text.length && eng2num(parsed_text[row_num_pointer]) != 0 && eng2num(parsed_text[col_num_pointer]) != 0)
        {
            row_num = eng2num(parsed_text[row_num_pointer]);
            col_num = eng2num(parsed_text[col_num_pointer]);
        }

        return (text + ": " + row_num + "_" + col_num + "_"+col_num_pointer +"_"+row_num_pointer);
    }

    private int isRow(String text)
    {
        if(text.compareTo("row") == 0)
            return 1;
        if(text.compareTo("rows") == 0)
            return 1;
        if(text.compareTo("room") == 0)
            return 1;
        if(text.compareTo("role") == 0)
            return 1;
        if(text.compareTo("rule") == 0)
            return 1;
        if(text.compareTo("raw") == 0)
            return 1;
        if(text.compareTo("roll") == 0)
            return 1;
        if(text.compareTo("rome") == 0)
            return 1;
        if(text.compareTo("rose") == 0)
            return 1;
        if(text.compareTo("route") == 0)
            return 1;
        if(text.compareTo("road") == 0)
            return 1;
        if(text.compareTo("no") == 0)
            return 1;
        if(text.compareTo("you") == 0)
            return 1;
        if(text.compareTo("your") == 0)
            return 1;
        if(text.compareTo("you're") == 0)
            return 1;
        if(text.compareTo("comm") == 0)
            return 1;
        return 0;
    }

    private int isCol(String text)
    {
        if(text.compareTo("column") == 0)
            return 1;
        if(text.compareTo("columns") == 0)
            return 1;
        if(text.compareTo("code") == 0)
            return 1;
        if(text.compareTo("cold") == 0)
            return 1;
        if(text.compareTo("call") == 0)
            return 1;
        if(text.compareTo("called") == 0)
            return 1;
        if(text.compareTo("colin") == 0)
            return 1;
        if(text.compareTo("calling") == 0)
            return 1;
        if(text.compareTo("college") == 0)
            return 1;
        if(text.compareTo("color") == 0)
            return 1;
        if(text.compareTo("quorum") == 0)
            return 1;
        if(text.compareTo("corn") == 0)
            return 1;
        if(text.compareTo("cons") == 0)
            return 1;
        if(text.compareTo("commerce") == 0)
            return 1;
        if(text.compareTo("con") == 0)
            return 1;
        if(text.compareTo("coal") == 0)
            return 1;
        if(text.compareTo("corn") == 0)
            return 1;
        if(text.compareTo("cotton") == 0)
            return 1;
        if(text.compareTo("cloud") == 0)
            return 1;
        if(text.compareTo("cholera") == 0)
            return 1;
        if(text.compareTo("climb") == 0)
            return 1;
        if(text.compareTo("claim") == 0)
            return 1;
        if(text.compareTo("o'clock") == 0)
            return 1;
        if(text.compareTo("caught") == 0)
            return 1;
        if(text.compareTo("com") == 0)
            return 1;
        if(text.compareTo("comm") == 0)
            return 1;
        if(text.compareTo("colleagues") == 0)
            return 1;
        return 0;
    }


    private int eng2num(String text)
    {
        int result = 0;

        if(text.compareTo("one") == 0)
            result=1;
        else if(text.compareTo("wine") == 0)
            return 1;
        else if(text.compareTo("two") == 0)
            result=2;
        else if(text.compareTo("to") == 0)
            result=2;
        else if(text.compareTo("too") == 0)
            result=2;
        else if(text.compareTo("three") == 0)
            result=3;
        else if(text.compareTo("four") == 0)
            result=4;
        else if(text.compareTo("for") == 0)
            result=4;
        else if(text.compareTo("five") == 0)
            result=5;
        else if(text.compareTo("flies") == 0)
            result=5;
        else if(text.compareTo("six") == 0)
            result=6;
        else if(text.compareTo("sex") == 0)
            result=6;
        else if(text.compareTo("seven") == 0)
            result=7;
        else if(text.compareTo("eight") == 0)
            result=8;
        else if(text.compareTo("it") == 0)
            result=8;
        else if(text.compareTo("nine") == 0)
            result=9;
        else if(text.compareTo("line") == 0)
            result=9;
        else if(text.compareTo("ten") == 0)
            result=10;
        else if(text.compareTo("eleven") == 0)
            result=11;
        else if(text.compareTo("twelve") == 0)
            result=12;
        else if(text.compareTo("twelfth") == 0)
            result=12;
        else if(text.compareTo("thirteen") == 0)
            result=13;
        else if(text.compareTo("fourteen") == 0)
            result=14;
        else if(text.compareTo("fifteen") == 0)
            result=15;
        else if(text.compareTo("sixteen") == 0)
            result=16;
        else if(text.compareTo("seventeen") == 0)
            result=17;
        else if(text.compareTo("eighteen") == 0)
            result=18;
        else if(text.compareTo("nineteen") == 0)
            result=19;
        else if(text.compareTo("twenty") == 0)
            result=20;

        return result;
    }

    public String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        String[] wanted_text = new String [4];
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            JSONObject obj;

            String tmp=null;
            String[] parsed_text;
            int i, j, k, correct_count;
            for (i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");

                for(j=0; j<items.length();j++){

                    obj = items.getJSONObject(j);
                    tmp = obj.getString("w").toLowerCase();
                    parsed_text = tmp.split(" ");

                    if(parsed_text.length < 4)
                        continue;

                    System.out.println(tmp);
                    for(k = 0;k<parsed_text.length;k++)
                        if(eng2num(parsed_text[k]) != 0)
                            if(k<=2)
                                wanted_text[1] = parsed_text[k];
                            else
                                wanted_text[3] = parsed_text[k];
                        else if(isRow(parsed_text[k]) != 0)
                            wanted_text[0] = "row";
                        else if(isCol(parsed_text[k]) != 0)
                            wanted_text[2] = "column";
                        else if(parsed_text[k].compareTo("colorful") == 0)
                        {
                            wanted_text[2] = "column";
                            wanted_text[3] = "four";
                        }
                        else if(parsed_text[k].compareTo("rotor") == 0)
                        {
                            wanted_text[0] = "row";
                            wanted_text[1] = "two";
                        }
                        else if(parsed_text[k].compareTo("quality") == 0)
                        {
                            wanted_text[2] = "column";
                            wanted_text[3] = "twenty";
                        }

                }

                for(i = 0; i<wanted_text.length;i++)
                    if(wanted_text[i] == null){
                        ret.append(" ");
                    }
                    else
                        ret.append(wanted_text[i] + " ");



            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }
}
