package cn.edu.sjtu.iiot.system.batchqr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static java.util.Collections.sort;

/**
 * Created by zhyao on 18-3-4.
 */

public class Utils {
    public static void qr_sort(ArrayList<Rect> qr_bbox, ArrayList<ArrayList<Rect>> qr_bbox_sorted){
        int nb_bbox = qr_bbox.size();
        Collections.sort(qr_bbox, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                return o1.y<o2.y?1:-1;
            }
        });

        ArrayList<Neighbor> edges = new ArrayList<>();
        for(int i=0; i<nb_bbox-1; i++){
            edges.add(new Neighbor(i,i+1,(qr_bbox.get(i+1).y-qr_bbox.get(i).y)));
        }
        Collections.sort(edges, new Comparator<Neighbor>() {
            @Override
            public int compare(Neighbor o1, Neighbor o2) {
                return (o1.simlarity-o2.simlarity)>0?1:-1;
            }
        });

        int sum = (int) edges.get(edges.size()-1).simlarity;

        UFS qr_cluster = new UFS(nb_bbox);
        int[] map = qr_cluster.set;

        for(int i=0; i<edges.size(); i++){
            sum += edges.get(i).simlarity;
            int thresh = 2*sum/(i+1);

            if(edges.get(i).simlarity<=thresh){
                int p1 = edges.get(i).from;
                int p2 = edges.get(i).to;

                qr_cluster.join(p1,p2);
            }
        }

        int[] idx = new int[nb_bbox];
        for(int i=0; i<nb_bbox; i++){
            idx[i] = -1;
        }

        qr_bbox_sorted.clear();
        int nb_cluster = 0;
        for(int i=0; i<nb_bbox; i++){
            int root = qr_cluster.find(i);
            if(idx[root] == -1){
                idx[root] = nb_cluster++;
                ArrayList<Rect> temp = new ArrayList<>();
                temp.add(qr_bbox.get(i));
                qr_bbox_sorted.add(temp);
            }else {
                qr_bbox_sorted.get(idx[root]).add(qr_bbox.get(i));
            }
        }

        for(int i=0; i<nb_cluster; i++){
            Collections.sort(qr_bbox_sorted.get(i), new Comparator<Rect>() {
                @Override
                public int compare(Rect o1, Rect o2) {
                    return o1.x<o2.x?1:-1;
                }
            });
        }
    }
    public static void saveToSDCard(Context context,int id,String name) throws Exception{
        InputStream inStream = context.getResources().openRawResource(id);
        File file = new File(Environment.getExternalStorageDirectory(), name);
        FileOutputStream fileOutputStream = new FileOutputStream(file);//存入SDCard
        byte[] buffer = new byte[10];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int len = 0;
        while((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] bs = outStream.toByteArray();
        fileOutputStream.write(bs);
        outStream.close();
        inStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public static String saveBitmap(String bitName,Bitmap mBitmap) {
        File file = new File(Environment.getExternalStorageDirectory(),bitName + ".jpg");

//        try {
//            f.createNewFile();
//        } catch (IOException e) {
//            Log.d("SAVEFILE","在保存图片时出错：" + e.toString());
//        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        } catch (Exception e) {
            return "create_bitmap_error";
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitName + ".jpg";
    }

    public static Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        return newBM;
    }
}
