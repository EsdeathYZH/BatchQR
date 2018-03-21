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


JNIEXPORT void JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess1(
        JNIEnv *env, jobject instance, jlong srcRgb, jstring addpath)
{
    cv::Mat mRgb = *((cv::Mat*)srcRgb);
    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);
    LOGD("[ Size of mRgb]  cols: %d, rows: %d.", mRgb.cols, mRgb.rows);

    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    std::string paths(jnamestr);
    std::vector<cv::Rect> qr_bbox;

    // Actual Algorithm runs here;
    process(mRgb, qr_bbox, paths);

    // Draw some rectangles
    while(qr_bbox.size())
    {
        cv::Rect drawRect = qr_bbox.back();
        qr_bbox.pop_back();

        cv::rectangle(mRgb, drawRect, cv::Scalar(0,255,0), 3);
    }

    cv::imwrite("/storage/emulated/0/batchQR_model/box_results.png", mRgb);
    env->ReleaseStringUTFChars(addpath, jnamestr);
}


JNIEXPORT jstring JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess2(
        JNIEnv *env, jobject instance, jlong srcRgb, jstring addpath)
{
    cv::Mat mRgb = *((cv::Mat*)srcRgb);

    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);
    LOGD("[ Size of mRgb]  cols: %d, rows: %d.", mRgb.cols, mRgb.rows);

    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    std::string paths(jnamestr);
    std::vector<cv::Rect> qr_bbox;

    // Actual Algorithm runs here;
    process(mRgb, qr_bbox, paths);

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

        cv::rectangle(mRgb, drawRect, cv::Scalar(0,255,0), 3);
    }

    cv::imwrite("/storage/emulated/0/batchQR_model/box_results.png", mRgb);

    env->ReleaseStringUTFChars(addpath, jnamestr);
    return env->NewStringUTF(qr_bbox_raw_info.c_str());
}

}


