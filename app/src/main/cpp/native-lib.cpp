#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/native_window_jni.h>

#include <string>
#include <vector>
#include <sstream>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#include "qr_region2.hpp"

float resize_within_pixel(cv::Mat src, cv::Mat& dst, int maxSize)
{
    int cols = src.cols;
    int rows = src.rows;
    if(cols<=maxSize && rows<=maxSize) {
        dst = src;
        return 1.0;
    }

    int maxLength = cols>=rows ? cols : rows;
    float resizeRatio = float(maxSize) / maxLength;
    cv::resize(src, dst, cv::Size(0,0), resizeRatio, resizeRatio);

    return resizeRatio;
}

template <typename T>
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}

extern "C" {
//JNIEXPORT void JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess(
//        JNIEnv *env, jobject instance, jint srcWidth, jint srcHeight, jobject srcBuffer, jobject dstSurface, jstring path0)
//{
//    uint8_t *srcLumaPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(srcBuffer));
//    if (srcLumaPtr== nullptr) {
//        LOGE("blit NULL pointer ERROR.");
//        return;
//    }
//
//    int dstWidth = srcWidth;
//    int dstHeight = srcHeight;
//
//    cv::Mat mYuv(srcHeight+srcHeight/2, srcWidth, CV_8UC1, srcLumaPtr);
//    cv::Mat mRgb;
//
//    cv::cvtColor(mYuv, mRgb, cv::COLOR_YUV2BGR_I420);
//    resize_within_pixel(mRgb, mRgb, 1000);
//    LOGD("[ Size of mRgb]  cols: %d, rows: %d.", mRgb.cols, mRgb.rows);
//    LOGD("[ Size of mYuv]  cols: %d, rows: %d.", mYuv.cols, mYuv.rows);
//
//    const char* jnamestr = env->GetStringUTFChars(path0, NULL);
//    std::vector<std::string> paths;
//    paths.push_back(std::string(jnamestr));
//    std::vector<cv::Rect> bbox;
//    int cnt;
//
//    // Actual Algorithm runs here;
//    process(mRgb, bbox, cnt, paths);
//
//    if(cnt==0) bbox.push_back(cv::Rect(0,0,0,0));
//}

JNIEXPORT void JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess0(
        JNIEnv *env, jobject instance, jlong srcYuv420, jobject dstSurface, jstring addpath)
{
    /*
     * converting the JMat to native cv::Mat & bgr
     */
    cv::Mat mYuv = *((cv::Mat*)srcYuv420), mRgb, mRgbf;
    cv::cvtColor(mYuv, mRgb, cv::COLOR_YUV2BGR_I420);
    cv::transpose(mRgb, mRgbf);
    cv::flip(mRgbf, mRgbf, 1);
    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);

    std::vector<std::string> paths;
    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    paths.push_back(std::string(jnamestr));
    std::vector<cv::Rect> qr_bbox;
    int qr_cnt = 0;

    // Actual Algorithm runs here;
    process(mRgbf, qr_bbox, qr_cnt, paths);
    LOGD("[  INFO]  found %d QRCs.", qr_cnt);


    /*
     * generating the native display session
     */
    uint8_t *srcChromaUVInterleavedPtr = nullptr;
    bool swapDstUV;

    ANativeWindow *win = ANativeWindow_fromSurface(env, dstSurface);
    ANativeWindow_acquire(win);

    ANativeWindow_Buffer buf;

    int dstWidth = mRgb.rows;
    int dstHeight = mRgb.cols;

    ANativeWindow_setBuffersGeometry(win, dstWidth, dstHeight, 0 /*format unchanged*/);

    if (int32_t err = ANativeWindow_lock(win, &buf, NULL)) {
        LOGE("ANativeWindow_lock failed with error code %d\n", err);
        ANativeWindow_release(win);
        return;
    }

    // TextureView buffer, use stride as width
    uint8_t *dstLumaPtr = reinterpret_cast<uint8_t *>(buf.bits);
    cv::Mat dstRgba(dstHeight, buf.stride, CV_8UC4, dstLumaPtr);

    // copy to TextureView surface
    uchar *dbuf;
    uchar *sbuf;
    dbuf = dstRgba.data;
    sbuf = mRgbf.data;

    for(int i=0; i<mRgbf.rows; ++i)
    {
        dbuf = dstRgba.data + i * buf.stride * 4;
        memcpy(dbuf, sbuf, mRgbf.cols * 4);
        sbuf += mRgbf.cols * 4;
    }

    // Draw some rectangles
    while(qr_bbox.size())
    {
        cv::Rect drawRect = qr_bbox.back();
        qr_bbox.pop_back();

        drawRect.x /= resizeRatio;
        drawRect.y /= resizeRatio;
        drawRect.width  /= resizeRatio;
        drawRect.height /= resizeRatio;
        cv::rectangle(dstRgba, drawRect, cv::Scalar(0,255,0));
    }
    cv::rectangle(dstRgba, cv::Rect(), cv::Scalar(0,0,255));

    // LOGE("bob dstWidth=%d height=%d", dstWidth, dstHeight);
    ANativeWindow_unlockAndPost(win);
    ANativeWindow_release(win);

    env->ReleaseStringUTFChars(addpath, jnamestr);
}

JNIEXPORT void JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess1(
        JNIEnv *env, jobject instance, jlong srcYuv420, jstring addpath)
{
    cv::Mat mYuv = *((cv::Mat*)srcYuv420);
    cv::Mat mRgb;

    cv::cvtColor(mYuv, mRgb, cv::COLOR_YUV2BGR_I420);
    cv::transpose(mRgb, mRgb);
    cv::flip(mRgb, mRgb, 1);

    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);
    LOGD("[ Size of mRgb]  cols: %d, rows: %d.", mRgb.cols, mRgb.rows);
    LOGD("[ Size of mYuv]  cols: %d, rows: %d.", mYuv.cols, mYuv.rows);

    std::vector<std::string> paths;
    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    paths.push_back(std::string(jnamestr));
    std::vector<cv::Rect> qr_bbox;
    int qr_cnt = 0;

    // Actual Algorithm runs here;
    process(mRgb, qr_bbox, qr_cnt, paths);

    LOGD("[  INFO]  found %d QRCs.", qr_cnt);

    // Draw some rectangles
    while(qr_bbox.size())
    {
        cv::Rect drawRect = qr_bbox.back();
        qr_bbox.pop_back();

        cv::rectangle(mRgb, drawRect, cv::Scalar(0,255,0), 5);
    }

    cv::imwrite("/storage/emulated/0/batchQR_model/box_results.png", mRgb);
    env->ReleaseStringUTFChars(addpath, jnamestr);
}


JNIEXPORT jstring JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess2(
        JNIEnv *env, jobject instance, jlong srcYuv420, jlong dstRgb, jstring addpath)
{
    cv::Mat mRgb = *((cv::Mat*)srcYuv420);
    // cv::Mat* mdstRgb = ((cv::Mat*)dstRgb);
    // cv::Mat mRgb;

    // cv::cvtColor(mYuv, mRgb, cv::COLOR_YUV2BGR_I420);
    // cv::transpose(mRgb, mRgb);
    // cv::flip(mRgb, mRgb, 1);
    // mdstRgb->create(mRgb.size(), CV_8UC3);
    // memcpy(mdstRgb->data, mRgb.data, mRgb.step * mRgb.rows);

    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);
    LOGD("[ Size of mRgb]  cols: %d, rows: %d.", mRgb.cols, mRgb.rows);
    // LOGD("[ Size of mYuv]  cols: %d, rows: %d.", mYuv.cols, mYuv.rows);

    std::vector<std::string> paths;
    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    paths.push_back(std::string(jnamestr));
    std::vector<cv::Rect> qr_bbox;
    int qr_cnt = 0;

    // Actual Algorithm runs here;
    process(mRgb, qr_bbox, qr_cnt, paths);

    LOGD("[  INFO]  found %d QRCs.", qr_cnt);

    // Draw some rectangles
    std::string qr_bbox_raw_info;
    while(qr_bbox.size())
    {
        cv::Rect drawRect = qr_bbox.back();
        qr_bbox.pop_back();

        qr_bbox_raw_info += std::string(to_string((int)(drawRect.x/resizeRatio))+' '+
                                        to_string((int)(drawRect.y/resizeRatio))+' '+
                                        to_string((int)(drawRect.width/resizeRatio))+' '+
                                        to_string((int)(drawRect.height/resizeRatio))+';');

        cv::rectangle(mRgb, drawRect, cv::Scalar(0,255,0), 5);
    }

    LOGD("[  INFO] bboxs: %s", qr_bbox_raw_info.c_str());
    cv::imwrite("/storage/emulated/0/batchQR_model/box_results.png", mRgb);

    env->ReleaseStringUTFChars(addpath, jnamestr);
    return env->NewStringUTF(qr_bbox_raw_info.c_str());
}

}


