package cn.edu.sjtu.iiot.system.batchqr;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

/**
 * Created by eason on 15/03/2018.
 */

public class QrCodeDetector {

    public static final int MODE_QUICK = 0;
    public static final int MODE_ACCURATE = 1;

    private static final String TAG = "batchQR/QRCodeDetectorSeg";
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    /**
     * The image data stored in OpenCV's Mat data type
     * The output shall be a Surface object for displaying the result
     */
    public static Mat src_image = null;
    public static String bbox_raw_info = null;

    private static Mat imageToMat(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {


                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }

    public static void detectQrCodes(Image img, String paths,int mode) {
        switch(img.getFormat()) {
            case ImageFormat.YUV_420_888: {
                Image.Plane[] planes = img.getPlanes();
                // Spec guarantees that planes[0] is luma and has pixel stride of 1.
                // It also guarantees that planes[1] and planes[2] have the same row and
                // pixel stride.
                if (planes[1].getPixelStride() != 1 && planes[1].getPixelStride() != 2) {
                    throw new IllegalArgumentException(
                            "src chroma plane must have a pixel stride of 1 or 2: got "
                                    + planes[1].getPixelStride());
                }

                Mat mYuv = imageToMat(img);
                src_image = new Mat();
                Imgproc.cvtColor(mYuv, src_image, Imgproc.COLOR_YUV2BGR_I420);
                break;
            }
            case ImageFormat.JPEG: {

                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported src ImageFormat.");
            }


        }


        Core.transpose(src_image, src_image);
        Core.flip(src_image, src_image, 1);

        if(mode==QrCodeDetector.MODE_QUICK){
            bbox_raw_info = JniProcess1(src_image.getNativeObjAddr());
        }else if(mode==QrCodeDetector.MODE_ACCURATE){
            bbox_raw_info = JniProcess2(src_image.getNativeObjAddr(), paths);
        }

        Log.d(TAG, bbox_raw_info);
    }

    /**
     * The native-cpp method to convert Image into OpenCV's cv::Mat data type
     */
    public static native String JniProcess1(long mRgbAddr);
    public static native String JniProcess2(long mRgbAddr, String addPath);
}
