package cn.edu.sjtu.iiot.system.batchqr;

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
import android.util.Size;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Zihang Yao on 2018/3/20.
 */

public class ResultActivity extends Activity implements View.OnClickListener{
    private ImageView backPreview;
    private RelativeLayout layout;
    private SurfaceView drawPreview;
    private Bitmap imageFile;
    private TextView result_info;
    private final MultiFormatReader multiFormatReader = new MultiFormatReader();
    private ArrayList<LuminanceSource> luminanceSources;
    private HashMap<Integer,Result> rawResults;
    private ArrayList<Rect>boxes;
    private ArrayList<Bitmap>box_imgs;
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
        rawResults = new HashMap<Integer,Result>();
        multiFormatReader.setHints(hints);

        initBoxes();
        initView();
        initData();
        //result_info.setText("检测出"+boxes.size()+"个二维码，扫描出"+qr_num+"个二维码.");
    }

    private void initData(){
        buildLuminanceSource(imageFile.getWidth(),imageFile.getHeight());
        decode();
    }
    private void initView(){
        this.result_info = (TextView) findViewById(R.id.result_msg);
        this.backPreview = (ImageView) findViewById(R.id.src_image);
        Mat src = QrCodeDetector.src_image;
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
        imageFile = Bitmap.createBitmap(src.width(),src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,imageFile);
        //If glasses
        if(isGlasses){
            screenRatio = (double)screenHeight/imageFile.getWidth();
            Bitmap rotateImage = cn.edu.sjtu.iiot.system.batchqr.Utils.
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
        rawResults.put(index,result);
        Button button = findViewById(index);
        button.setBackgroundResource(result == null ?
                R.drawable.shape_button_fail : R.drawable.shape_button);
        button.setText(result == null ? "X" : "√");
        button.setTextColor(result == null ? Color.RED : Color.parseColor("#388e3c"));
    }

    private void initBoxes(){
        String box_info = QrCodeDetector.bbox_raw_info;
        if(box_info.trim().equals("")){
            return;//Can't detect any Qrcodes.
        }
        String[] box_strings = box_info.split(";");
        for(int i=0; i<box_strings.length; i++){
            if(box_strings[i].trim().equals("")){
                continue;
            }
            String[] box_params = box_strings[i].split(" ");
            int x = Integer.valueOf(box_params[0]);
            int y = Integer.valueOf(box_params[1]);
            int width = Integer.valueOf(box_params[2]);
            int height = Integer.valueOf(box_params[3]);
            boxes.add(new Rect(x,y,x+width,y+height));
        }
    }

    private void decode() {
        for (int i = 0; i < boxes.size(); i++) {
            final int index =i;
            new Thread(new Runnable() {
                @Override
                public void run() {
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
        while(result == null && angle!=360){
            Bitmap rotate_img = cn.edu.sjtu.iiot.system.batchqr.
                    Utils.rotateBitmap(bitmap,angle);
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
        imageFile.getPixels(pixels, 0, width, 0, 0, width, height);

        RGBLuminanceSource image = new RGBLuminanceSource(width, height, pixels);
        for (int i = 0; i < boxes.size(); i++) {
            luminanceSources.add(image.crop(boxes.get(i).left, boxes.get(i).top,
                    boxes.get(i).width(),boxes.get(i).height()));
        }
    }

    @Override
    public void onClick(View v){
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

}
