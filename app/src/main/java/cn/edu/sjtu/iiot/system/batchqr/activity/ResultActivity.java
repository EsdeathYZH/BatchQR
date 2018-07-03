package cn.edu.sjtu.iiot.system.batchqr.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.edu.sjtu.iiot.system.batchqr.model.Command;
import cn.edu.sjtu.iiot.system.batchqr.utils.JsonParser;
import cn.edu.sjtu.iiot.system.batchqr.utils.NetworkClient;
import cn.edu.sjtu.iiot.system.batchqr.QrCodeDetector;
import cn.edu.sjtu.iiot.system.batchqr.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Zihang Yao on 2018/3/20.
 */

public class ResultActivity extends Activity implements View.OnClickListener{
    private RelativeLayout layout;
    private Bitmap imageFile;
    private TextView result_info;

    private ArrayList<LuminanceSource> luminanceSources;
    private HashMap<Integer,Result> rawResults;
    private ArrayList<Rect>boxes;
    private ArrayList<Bitmap>box_imgs;

    private long buildLuminanceTime;
    private double processTime;
    private HashMap<Integer,Long> decodeTimes;

    private final MultiFormatReader multiFormatReader = new MultiFormatReader();
    private Map<DecodeHintType, Object> hints;
    private int qr_num = 0;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int index = msg.arg1;
            Result result = null;
            if(msg.obj!=null){
                result = (Result) msg.obj;
                qr_num++;
            }
            showResult(index,result);
        }
    };

    private int screenWidth;
    private int screenHeight;
    private boolean isGlasses;
    private double screenRatio;

    public String testlist=null;
    private EditText et_input;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String , String>();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        layout = (RelativeLayout) findViewById(R.id.result_layout);

        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        if(screenHeight<screenWidth) isGlasses = true;

        luminanceSources = new ArrayList<>();
        boxes = new ArrayList<>();
        box_imgs = new ArrayList<>();
        rawResults = new HashMap<>();
        decodeTimes = new HashMap<>();
        multiFormatReader.setHints(hints);

        initSpeech();
        initBoxes();
        initView();
        initData();
//        long sum = 0,success_sum = 0;
//        for(int i=0;i<decodeTimes.size();i++){
//            if(rawResults.get(i)!=null) success_sum += decodeTimes.get(i);
//            sum += decodeTimes.get(i);
//        }+decode_sec+"毫秒,扫描成功的平均扫描用时"
//        +success_sec+"毫秒"
//        double decode_sec = ((double)sum)/decodeTimes.size();
//        double success_sec = ((double)success_sum)/decodeTimes.size();

        result_info.setText("检测出"+boxes.size()+"个二维码，扫描出"+qr_num+
                "个二维码,build luminance用时"+buildLuminanceTime+"毫秒,process用时"
        +processTime+"秒，平均decode用时");
    }

    private void initData(){
        buildLuminanceSource(imageFile.getWidth(),imageFile.getHeight());
        decode();
    }

    private void initSpeech() {
        // 请勿在 “ =”与 appid 之间添加任务空字符或者转义符
        SpeechUtility. createUtility( this, SpeechConstant. APPID + "=5ad54379" );
    }

    private void initView(){
        this.result_info = (TextView) findViewById(R.id.result_msg);
        ImageView backPreview = (ImageView) findViewById(R.id.src_image);
        et_input = (EditText) findViewById(R.id.et_input );
        Button btn_startspeech = (Button) findViewById(R.id.btn_startspeech );
        btn_startspeech .setOnClickListener(this) ;

        Mat src = QrCodeDetector.src_image;
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
        imageFile = Bitmap.createBitmap(src.width(),src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,imageFile);
        //If glasses
        if(isGlasses){
            screenRatio = (double)screenHeight/imageFile.getWidth();
            Bitmap rotateImage = cn.edu.sjtu.iiot.system.batchqr.utils.Utils.
                    rotateBitmap(imageFile,270);
            RelativeLayout.LayoutParams params =new RelativeLayout.LayoutParams
                    (RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = 0;
            params.leftMargin = 0;
            params.height = screenHeight;
            backPreview.setLayoutParams(params);
            backPreview.setImageBitmap(rotateImage);
        }
        else{
            screenRatio = (double)screenWidth/imageFile.getWidth();
            backPreview.setImageBitmap(imageFile);
        }
        //Add buttons
        for(int index = 0;index<boxes.size();index++) {
            Button button = new Button(this);
            button.setOnClickListener(this);
            button.setId(index);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setTextSize(24);
            button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
                    (RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            if (isGlasses) {
                params.width = (int) (boxes.get(index).height() * screenRatio);
                params.height = (int) (boxes.get(index).width() * screenRatio);
                params.leftMargin = (int) (boxes.get(index).top * screenRatio);
                params.topMargin = (int) ((imageFile.getWidth() - boxes.get(index).left - boxes.get(index).width()) * screenRatio);
            } else {
                params.width = (int) (boxes.get(index).width() * screenRatio);
                params.height = (int) (boxes.get(index).height() * screenRatio);
                params.leftMargin = (int) (boxes.get(index).left * screenRatio);
                params.topMargin = (int) (boxes.get(index).top * screenRatio);
            }
            button.setLayoutParams(params);
            layout.addView(button);
        }
    }

    private void showResult(int index,Result result){
        rawResults.put(index,new Result(String.valueOf(decodeTimes.get(index))
                ,null,null,null));
        Button button = findViewById(index);
        button.setBackgroundResource(result == null ?
                R.drawable.shape_button_fail : R.drawable.shape_button);
        button.setText(result == null ? "X" : "√");
        button.setTextColor(result == null ?
                Color.RED : Color.parseColor("#388e3c"));
    }

    private void initBoxes(){
        String box_info = QrCodeDetector.bbox_raw_info;
        String[] box_time = box_info.split("&");
        if(box_time.length<=1){
            return;//Can't detect any Qrcodes.
        }
        processTime = Double.valueOf(box_time[0]);
        String[] box_strings = box_time[1].split(";");
        for(int i=0; i<box_strings.length; i++){
            if(box_strings[i].trim().equals("")){
                continue;
            }
            String[] box_params = box_strings[i].split(" ");
            int x = Integer.valueOf(box_params[0]);
            int y = Integer.valueOf(box_params[1]);
            int width = Integer.valueOf(box_params[2]);
            int height = Integer.valueOf(box_params[3]);
            if(width>0 && height>0){
                boxes.add(new Rect(x,y,x+width,y+height));
            }
        }
    }

    private void decode() {
        for (int i = 0; i < boxes.size(); i++) {
            final int index =i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long start_time = System.currentTimeMillis();
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSources.get(index)));
                    Result result = null;
                    try {
                        result = multiFormatReader.decode(bitmap);
                        //Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
                    } catch (ReaderException re) {
                        result = forwardDecode(index);
                        re.printStackTrace();
                    } finally {
                        multiFormatReader.reset();
                    }
                    long end_time = System.currentTimeMillis();
                    //记录一下扫码时间
                    decodeTimes.put(index,end_time-start_time);
                    Message msg = new Message();
                    msg.arg1 = index;
                    msg.obj = result;
                    mHandler.sendMessage(msg);
                }
            }).start();
        }
    }

    private Result forwardDecode(int index){
        Bitmap bitmap = box_imgs.get(index);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Result result = null;
        float angle = 30;
        while(angle!=360){
            Bitmap rotate_img = cn.edu.sjtu.iiot.system.batchqr.utils.Utils.rotateBitmap(bitmap,angle);
            int[] pixels = new int[rotate_img.getWidth() * rotate_img.getHeight()];
            rotate_img.getPixels(pixels, 0, rotate_img.getWidth(), 0, 0,
                    rotate_img.getWidth(), rotate_img.getHeight());
            RGBLuminanceSource luminanceSource = new RGBLuminanceSource(
                    rotate_img.getWidth(), rotate_img.getHeight(), pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
            try {
                result = multiFormatReader.decode(binaryBitmap);
                break;
            } catch (ReaderException re) {
                angle+=30;
                result = null;
            }
        }
        return result;
    }

    private void buildLuminanceSource(int width, int height) {
        int[] pixels = new int[width * height];
        for(int i = 0; i < boxes.size(); i++){
            box_imgs.add(Bitmap.createBitmap(
                    imageFile,boxes.get(i).left,boxes.get(i).top,
                    boxes.get(i).width(),boxes.get(i).height()));
        }

        long start_time = System.currentTimeMillis();
        imageFile.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource image = new RGBLuminanceSource(width, height, pixels);
        for (int i = 0; i < boxes.size(); i++) {
            luminanceSources.add(image.crop(boxes.get(i).left, boxes.get(i).top,
                    boxes.get(i).width(),boxes.get(i).height()));
        }
        long end_time = System.currentTimeMillis();
        buildLuminanceTime = end_time-start_time;
    }

    @Override
    public void onClick(View v){
        //if user click speech button
        if(v.getId()==R.id.btn_startspeech){
            startSpeechDialog();
            return;
        }

        //if user click QRcode button
        final Result result = rawResults.get(v.getId());
        if(result == null){
            new AlertDialog.Builder(this)
                    .setTitle("扫描失败！")
                    .setMessage("Decode failed!")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create().show();
        }else {
            OkHttpClient client = NetworkClient.getClient();
            Request request = new Request.Builder()
                    .url("http://47.106.75.68:8080/get?id=" + result.getText())
                    .build();
            Response response = null;
            try {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(ResultActivity.this)
                                        .setTitle("网络出错！")
                                        .setMessage("Request failed!")
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .create().show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        String qr_name = "",qr_date = "",qr_detail = "";
                        if(response!=null && response.isSuccessful()){
                            try {
                                JSONObject qrcode = new JSONObject(response.body().string());
                                qr_name = qrcode.getString("name");
                                qr_date = qrcode.getString("date");
                                qr_detail = qrcode.getString("detail");
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        final String name = qr_name,date = qr_date,detail = qr_detail;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new AlertDialog.Builder(ResultActivity.this)
                                        .setTitle(name.equals("")?result.getText():name)
                                        .setMessage(detail+"\n\n"+date)
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .create().show();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    private void startSpeechDialog() {
        //1. 创建RecognizerDialog对象
        RecognizerDialog mDialog = new RecognizerDialog(this, new MyInitListener()) ;
        //2. 设置accent、 language等参数
        //mDialog.setParameter(SpeechConstant. LANGUAGE, "zh_cn" );// 设置中文
        mDialog.setParameter(SpeechConstant.LANGUAGE, "en_us" );//设置英文
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin" );
        mDialog.setParameter(SpeechConstant.ASR_PTT, "0");//去除标点
        //mDialog.setParameter(SpeechConstant.VAD_EOS, "2500");//最长静音等待时间为2.5s
        // 若要将UI控件用于语义理解，必须添加以下参数设置，设置之后 onResult回调返回将是语义理解
        // 结果
        // mDialog.setParameter("asr_sch", "1");
        // mDialog.setParameter("nlp_version", "2.0");
        //3.设置回调接口
        mDialog.setListener( new MyRecognizerDialogListener()) ;
        //4. 显示dialog，接收语音输入
        mDialog.show() ;
    }

    private void  show_text(){
        Command str_deal=new Command();
        str_deal.get_text=testlist.split(" ");
        str_deal.get_command();
        // 设置输入框的文本
        if(str_deal.id==-1 && str_deal.name==null){
            et_input.setText(testlist+"    no find something");
        }
        else if(str_deal.id!=-1){
            et_input.setText(testlist+"    find ID number:"+String.valueOf(str_deal.id));
        }
        else if(str_deal.name!=null){
            et_input.setText(testlist+"   find name or something:"+str_deal.name);
        }
        et_input.setSelection(et_input.length()) ;//把光标定位末尾
        testlist=null;

    }


    class MyRecognizerDialogListener implements RecognizerDialogListener {

        /**
         * @param results
         * @param isLast  是否说完了
         */
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String result = results.getResultString(); //为解析的
            showTip(result) ;
            System. out.println(" 没有解析的 :" + result);

            String text = JsonParser.parseIatResult(result) ;//解析过后的
            System. out.println(" 解析后的 :" + text);

            String sn = null;
            // 读取json结果中的 sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString()) ;
                sn = resultJson.optString("sn" );
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mIatResults .put(sn, text) ;//没有得到一句，添加到

            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults .get(key));
            }
            testlist=resultBuffer.toString();
            show_text();
        }

        @Override
        public void onError(SpeechError speechError) {

        }
    }

    class MyInitListener implements InitListener {

        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败 ");
            }

        }
    }

    private void showTip (String data) {
        Toast.makeText( this, data, Toast.LENGTH_SHORT).show() ;
    }

}
