package cn.edu.sjtu.iiot.system.batchqr;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
    private final MultiFormatReader multiFormatReader = new MultiFormatReader();
    private ArrayList<LuminanceSource> luminanceSources;
    private ArrayList<Result> rawResults;
    private ArrayList<Rect>boxes;
    private Map<DecodeHintType, Object> hints;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        layout = findViewById(R.id.result_layout);
        luminanceSources = new ArrayList<>();
        boxes = new ArrayList<>();
        rawResults = new ArrayList<Result>();
        initBoxes();
        initView();
        initData();
    }

    private void initData(){
        buildLuminanceSource(imageFile.getWidth(),imageFile.getHeight());
        decode();
    }
    private void initView(){
        this.backPreview = (ImageView) findViewById(R.id.src_image);
        Mat src = QrCodeDetector.src_image;
        if(src == null){
        }
        imageFile = Bitmap.createBitmap(src.width(),src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,imageFile);
        backPreview.setImageBitmap(imageFile);
        addButton();
    }

    private void addButton(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        for (int i = 0; i < boxes.size(); i++) {
            Button button = new Button(this);
            button.setOnClickListener(this);
            button.setId(i);
            button.setBackgroundResource(R.color.transparent);
            button.setText("S");
            button.setTextColor(Color.GREEN);
            button.setBackgroundColor(Color.TRANSPARENT);
            RelativeLayout.LayoutParams params =new RelativeLayout.LayoutParams
                            (RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = boxes.get(i).left;
            params.topMargin = boxes.get(i).top;
            button.setLayoutParams(params);
            layout.addView(button);
        }
    }
    private void initBoxes(){
        String box_info = QrCodeDetector.bbox_raw_info;
        String[] box_strings = box_info.split(";");
        for(int i=0; i<box_strings.length; i++){
            String[] box_params = box_strings[i].split(" ");
            int x = Integer.valueOf(box_params[0]);
            int y = Integer.valueOf(box_params[1]);
            int width = Integer.valueOf(box_params[2]);
            int height = Integer.valueOf(box_params[3]);
            boxes.add(new Rect(x,y,x+width,y+height));
        }
    }
    private void decode() {
        Size size = new Size(imageFile.getWidth(), imageFile.getHeight());

        buildLuminanceSource(size.getWidth(), size.getHeight());

        for (int i = 0; i < boxes.size(); i++) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminanceSources.get(i)));
            try {
                Result result = multiFormatReader.decode(bitmap);
                //Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
                rawResults.add(result);
            } catch (ReaderException re) {
                rawResults.add(null);
                re.printStackTrace();
            } finally {
                multiFormatReader.reset();
            }
        }
    }

    private void buildLuminanceSource(int width, int height) {
        int[] pixels = new int[width * height];
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
