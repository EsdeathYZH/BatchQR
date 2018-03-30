package cn.edu.sjtu.iiot.system.batchqr;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by SHIYONG on 2018/3/20.
 */

public class ResultActivity extends Activity implements View.OnClickListener{
    private ImageView backPreview;
    private RelativeLayout layout;
    private SurfaceView drawPreview;
    private Bitmap imageFile;
    private TextView result_info;
    private final MultiFormatReader multiFormatReader = new MultiFormatReader();
    private ArrayList<LuminanceSource> luminanceSources;
    private ArrayList<Result> rawResults;
    private ArrayList<Rect>boxes;
    private ArrayList<Bitmap>box_imgs;
    private Map<DecodeHintType, Object> hints;
    private int qr_num = 0;

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
        rawResults = new ArrayList<Result>();
        initBoxes();
        initView();
        initData();
        addButton();
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
        if(src == null){
        }
        imageFile = Bitmap.createBitmap(src.width(),src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,imageFile);
        //如果是眼镜
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
    }

    private void addButton(){
        for (int i = 0; i < boxes.size(); i++) {
            Button button = new Button(this);
            button.setOnClickListener(this);
            button.setId(i);
            button.setBackgroundResource(rawResults.get(i)==null?
                    R.drawable.shape_button_fail:R.drawable.shape_button);
            button.setText(rawResults.get(i)==null?"X":"√");
            button.setTextSize(24);
            button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            button.setTextColor(rawResults.get(i)==null?Color.RED:Color.parseColor("#388e3c"));
            RelativeLayout.LayoutParams params =new RelativeLayout.LayoutParams
                            (RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            if(isGlasses){
                params.width = (int)(boxes.get(i).height()*screenRatio);
                params.height = (int)(boxes.get(i).width()*screenRatio);
                params.leftMargin = (int)(boxes.get(i).top*screenRatio);
                params.topMargin =(int)((imageFile.getWidth()-boxes.get(i).left-boxes.get(i).width())*screenRatio);
            }else{
                params.width = (int)(boxes.get(i).width()*screenRatio);
                params.height = (int)(boxes.get(i).height()*screenRatio);
                params.leftMargin = (int)(boxes.get(i).left*screenRatio);
                params.topMargin = (int)(boxes.get(i).top*screenRatio);
            }
            button.setLayoutParams(params);
            layout.addView(button);
        }
        result_info.setText("检测出"+boxes.size()+"个二维码，扫描出"+qr_num+"个二维码.");
    }
    private void initBoxes(){
        String box_info = QrCodeDetector.bbox_raw_info;
        //Toast.makeText(this,box_info,Toast.LENGTH_SHORT).show();
        if(box_info.trim()==""){
            return;//Can't detect any Qrcodes.
        }
        String[] box_strings = box_info.split(";");
        for(int i=0; i<box_strings.length; i++){
            if(box_strings[i].trim()==""){
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
        qr_num = 0;
        Size size = new Size(imageFile.getWidth(), imageFile.getHeight());

        buildLuminanceSource(size.getWidth(), size.getHeight());

        for (int i = 0; i < boxes.size(); i++) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSources.get(i)));
            try {
                Result result = multiFormatReader.decode(bitmap);
                //Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
                rawResults.add(result);
                qr_num++;
            } catch (ReaderException re) {
                Result result = forwardDecode(i);
                if(result!=null) qr_num++;
                rawResults.add(result);
                re.printStackTrace();
            } finally {
                multiFormatReader.reset();
            }
        }
    }

    private Result forwardDecode(int index){
        Bitmap bitmap = box_imgs.get(index);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Result result = null;
        float angle = 30;
        while(result==null && angle!=330){
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
        Result result = rawResults.get(v.getId());
        new AlertDialog.Builder(this)
                .setTitle("QRcode"+v.getId())
                .setMessage(result==null?"failed!":result.getText())
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create().show();
    }

}
