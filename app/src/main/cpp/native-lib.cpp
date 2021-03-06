#include <jni.h>

#include <string>
#include <vector>
#include <time.h>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#include "graphseg_process.hpp"
#include "idft_process.hpp"

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

JNIEXPORT jstring JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess1(
        JNIEnv *env, jobject instance, jlong srcRgb)
{
    double totaltime;
    clock_t start_time = clock();

    cv::Mat mRgb = *((cv::Mat*)srcRgb);
    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);

    std::vector<cv::Rect> qr_bbox;

    // Actual Algorithm runs here;
    idft_process(mRgb, qr_bbox);

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

    clock_t end_time = clock();
    totaltime = (double)(end_time-start_time)/CLOCKS_PER_SEC;

    qr_bbox_raw_info = to_string(totaltime) + "&" + qr_bbox_raw_info;

    cv::imwrite("/storage/emulated/0/batchQR_model/box_results.png", mRgb);

    return env->NewStringUTF(qr_bbox_raw_info.c_str());
}


JNIEXPORT jstring JNICALL Java_cn_edu_sjtu_iiot_system_batchqr_QrCodeDetector_JniProcess2(
        JNIEnv *env, jobject instance, jlong srcRgb, jstring addpath)
{
    cv::Mat mRgb = *((cv::Mat*)srcRgb);
    cv::imwrite("/storage/emulated/0/batchQR_model/src0.png", mRgb);
    float resizeRatio = resize_within_pixel(mRgb, mRgb, 1000);

    const char* jnamestr = env->GetStringUTFChars(addpath, NULL);
    std::string paths(jnamestr);
    std::vector<cv::Rect> qr_bbox;

    // Actual Algorithm runs here;
    graphseg_process(mRgb, qr_bbox, paths);

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


