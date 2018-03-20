/*
 * Android NDK includes for debug and log
 */
#include <android/log.h>
#define LOG_TAG "batchQR/native_cpp_internal"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

#include <algorithm>
#include <vector>

#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/ml.hpp>
#include <opencv2/ximgproc/segmentation.hpp>
#include "qr_feature.hpp"

#define DEBUG_SESSION

using namespace cv::ximgproc::segmentation;


inline float calc_similarity(cv::Rect bbox, int size)
{
    float weight = bbox.height>=bbox.width ? (float)bbox.width/(float)bbox.height : (float)bbox.height/(float)bbox.width;
    return weight * size / bbox.area();
}

inline float calc_similarity(cv::Rect bbox1, cv::Rect bbox2, int size1, int size2)
{
    cv::Rect bbox = bbox1|bbox2;
    int size = size1+size2;
    float weight = bbox.height>=bbox.width ? (float)bbox.width/(float)bbox.height : (float)bbox.height/(float)bbox.width;
    return weight * size / bbox.area();
}


int process(cv::Mat& img, std::vector<cv::Rect>& qr_bbox, int& qr_cnt, std::vector<std::string> paths)
{
    LOGD("[  INFO]  first write the image before processing.");
    cv::imwrite("/storage/emulated/0/batchQR_model/src.png", img);

    cv::setUseOptimized(true);
    cv::setNumThreads(4);

    LOGD("[  INFO]  creating the GraphSegmentation algorithm...");
	cv::Ptr<GraphSegmentation> gs = createGraphSegmentation();
    if(!gs)
    {
    	LOGE("[ Error]	failed to create GraphSegmentation Algorithm.");
    	return -2;
    }
    gs->setSigma(0.8);
    gs->setK(2);
    gs->setMinSize(1250);

    cv::Mat src = img.clone(), seg;
    gs->processImage(src, seg);

    double min, max;
    cv::minMaxLoc(seg, &min, &max);
    int nb_segs = (int)max + 1;

    LOGD("[  INFO]  total: %d\tsegments.", nb_segs);

#ifdef DEBUG_SESSION
    cv::imwrite("/storage/emulated/0/batchQR_model/seg.png", seg);
    // cv::imwrite("/storage/emulated/0/batchQR_model/src.png", src);
#endif
    
    LOGD("[  INFO]  reading the svm classifier...");
    LOGD("[  INFO]  the svm classifier path: %s", paths[0].c_str());
    cv::Ptr<cv::ml::SVM> svm = cv::ml::SVM::load(paths[0]);
    if(svm.empty())
    {
        LOGE("[ Error]  failed to read the svm classifier.");
        return -3;
    }

    LOGD("[  INFO]  creating the lbp feature handler...");
    LBP lbp;

    std::vector<std::vector<cv::Point>> points;
    points.resize(nb_segs);

    std::vector<int> sizes;
    for(int s=0; s<nb_segs; ++s) sizes.push_back(0);

    cv::Mat is_neighbor = cv::Mat::zeros(nb_segs, nb_segs, CV_8UC1);

    const int* previous_p = NULL;
    for(int i=0; i<seg.rows; ++i)
    {
        const int* p = seg.ptr<int>(i);
        for(int j=0; j<seg.cols; ++j)
        {
            points[p[j]].push_back(cv::Point(j,i));
            sizes[p[j]]++;

            if(i>0 && j>0)
            {
                is_neighbor.at<char>(p[j],          p[j-1]) = 1;
                is_neighbor.at<char>(p[j], previous_p[j  ]) = 1;
                is_neighbor.at<char>(p[j], previous_p[j-1]) = 1;

                is_neighbor.at<char>(p[j-1],          p[j]) = 1;
                is_neighbor.at<char>(previous_p[j  ], p[j]) = 1;
                is_neighbor.at<char>(previous_p[j-1], p[j]) = 1;
            }
        }
        previous_p = p;
    }

    std::vector<cv::Rect> bboxs;
    bboxs.resize(nb_segs);
    for(int s=0; s<nb_segs; ++s) bboxs[s] = cv::boundingRect(points[s]);
    std::vector<std::vector<cv::Point>>().swap(points);

    std::vector<Neighbor> neighbors;
    neighbors.clear();
    for(int s=0; s<nb_segs; ++s)
        neighbors.push_back(Neighbor(s,s,calc_similarity(bboxs[s],sizes[s])));

    for(int s1=0; s1<nb_segs; ++s1)
        for(int s2=s1+1; s2<nb_segs; ++s2)
            if(is_neighbor.at<char>(s1,s2))
                neighbors.push_back(Neighbor(s1,s2,calc_similarity(bboxs[s1],bboxs[s2],sizes[s1],sizes[s2]) ) );

    std::sort(neighbors.begin(), neighbors.end());
    LOGD("[  INFO]  total %d neighbors", neighbors.size());
    std::vector<int>().swap(sizes);


    std::vector<int> is_qrseg;
    for(int s=0; s<nb_segs; ++s) is_qrseg.push_back(0);

    qr_bbox.clear();
    qr_cnt = 0;
    LOGD("[  INFO]  begin...");
    while(neighbors.size())
    {
        Neighbor proc_neighbor = neighbors.back();
        neighbors.pop_back();

        int s1 = proc_neighbor.from;
        int s2 = proc_neighbor.to;
        float soU = proc_neighbor.simlarity;

        if(soU<=0.6) break;
        if(is_qrseg[s1] || is_qrseg[s2]) continue;

        cv::Mat proc_msk, proc_seg;
        cv::Rect proc_bbox;
        int cs = (s1==s2);
        switch(cs)
        {
            case 1:
                proc_bbox = bboxs[s1];
                proc_msk = (seg==s1);
                break;
            case 0:
                proc_bbox = bboxs[s1]|bboxs[s2];
                proc_msk = (seg==s1)|(seg==s2);
                break;
            default: break;
        }
        
        img.copyTo(proc_seg, proc_msk);
        proc_seg = proc_seg(proc_bbox);
        cv::resize(proc_seg, proc_seg, cv::Size(120,120));

        cv::Mat proc_hist_feature(1, 170, CV_32FC1);
        cv::Mat hist1, hist2;

        lbp.calc_hist(proc_seg, hist1, 1, 1, 10);
        lbp.calc_hist(cv::Mat(), hist2, 4, 4, 10);
        hist1.copyTo(proc_hist_feature.colRange(  0, 10));
        hist2.copyTo(proc_hist_feature.colRange( 10,170));

        int qr_flag = (int)svm->predict(proc_hist_feature);
        if(qr_flag && soU>=0.75 && soU<=0.85)
        {
            is_qrseg[s1] = 1;
            is_qrseg[s2] = 1;
            qr_bbox.push_back(proc_bbox);
            ++qr_cnt;
            LOGD("[  INFO] predict results: %d, find one QRC.", qr_flag);
        }
    }

    std::vector<Neighbor>().swap(neighbors);
    std::vector<cv::Rect>().swap(bboxs);
    std::vector<int>().swap(is_qrseg);

    return 0;
}