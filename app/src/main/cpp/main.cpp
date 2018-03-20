#include <algorithm>
#include <vector>
#include <cstdio>

#include <opencv2/core.hpp>
#include <opencv2/ml.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/ximgproc/segmentation.hpp>

#include "qr_feature.hpp"
using namespace cv::ximgproc::segmentation;


inline float calc_similarity(cv::Rect bbox, int size)
{
    std::cout << "current bbox: " << bbox.height <<' '<< bbox.width <<' '<< bbox.area() << std::endl;
    float weight = bbox.height>=bbox.width ? (float)bbox.width/bbox.height : (float)bbox.height/bbox.width;
    return weight * size / bbox.area();
}

inline float calc_similarity(cv::Rect bbox1, cv::Rect bbox2, int size1, int size2)
{
    cv::Rect bbox = bbox1|bbox2;
    int size = size1+size2;
    std::cout << "current bbox: " << bbox.height <<' '<< bbox.width <<' '<< bbox.area() << std::endl;
    float weight = bbox.height>=bbox.width ? (float)bbox.width/bbox.height : (float)bbox.height/bbox.width;
    return weight * size / bbox.area();
}

class InValidInput{};

int main(int argc, char* argv[])
{
	if(argc!=2) throw InValidInput();

	cv::Mat src = cv::imread(argv[1]);
	if(src.empty()) throw InValidInput();


	int cols = src.cols;
	int rows = src.rows;

	float resize_ratio = (cols>=rows) ? 1000.0/cols : 1000.0/rows;

	cv::resize(src, src, cv::Size(0,0), resize_ratio, resize_ratio);

	cols = src.cols;
	rows = src.rows;
	std::cout << cols << 'x' << rows << std::endl;

	std::cout << "done.\n";


	std::string classifier_name = "/Users/eason/Desktop/qr_svc_model.xml";

	std::vector<cv::Rect> qr_bbox;
	int qr_cnt;
	
	cv::setUseOptimized(true);
    cv::setNumThreads(4);

    // printf("[  INFO]  creating the GraphSegmentation algorithm...");
    std::cout << "done.\n";
	cv::Ptr<GraphSegmentation> gs = createGraphSegmentation();
    if(!gs)
    {
    	printf("[ Error]	failed to create GraphSegmentation Algorithm.");
    	return -2;
    }
    gs->setSigma(0.8);
    gs->setK(2);
    gs->setMinSize(1250);

    

    cv::Mat seg;
    gs->processImage(src, seg);

    double min, max;
    cv::minMaxLoc(seg, &min, &max);
    int nb_segs = (int)max + 1;

    std::cout << "[  INFO]  total:" << nb_segs << "\tsegments." << std::endl;
    
    // printf("[  INFO]  reading the svm classifier...");
    cv::Ptr<cv::ml::SVM> svm = cv::ml::SVM::load(classifier_name);
    if(svm.empty())
    {
        printf("[ Error]  failed to read the svm classifier.");
        return -3;
    }

    // printf("[  INFO]  creating the lbp feature handler...");
    LBP lbp;

    std::vector<std::vector<cv::Point>> points;
    points.resize(nb_segs);

    std::vector<int> sizes;
    for(int s=0; s<nb_segs; ++s) sizes.push_back(0);

    cv::Mat is_neighbor = cv::Mat::zeros(nb_segs, nb_segs, CV_8UC1);

    const int* previous_p = NULL;
    for(int i=0; i<(int)seg.rows; i++)
    {
        const int* p = seg.ptr<int>(i);
        for(int j=0; j<(int)seg.cols; j++)
        {
            points[p[j]].push_back(cv::Point_2d(j,i));
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

    std::cout << "[  INFO] bounding_box done." << std::endl;

    std::vector<Neighbor> neighbors;
    neighbors.clear();

    for(int s=0; s<nb_segs; ++s)
        neighbors.push_back(Neighbor(s,s,calc_similarity(bboxs[s],sizes[s]) ) );

    std::cout << "[  INFO] neighbours1 done." << std::endl;

    for(int s1=0; s1<nb_segs; ++s1)
        for(int s2=s1+1; s2<nb_segs; ++s2)
            if(is_neighbor.at<char>(s1,s2))
                neighbors.push_back(Neighbor(s1,s2,calc_similarity(bboxs[s1],bboxs[s2],sizes[s1],sizes[s2]) ) );
    
    std::cout << "[  INFO] neighbours2 done." << std::endl;
    std::cout << "[  INFO] total " << neighbors.size() << " neighbors" << std::endl;

    std::sort(neighbors.begin(), neighbors.end());

   

    // clear up the memory of sizes vector
    std::vector<int>().swap(sizes);

    std::vector<int> is_qrseg;
    for(int s=0; s<nb_segs; ++s) is_qrseg.push_back(0);

    qr_bbox.clear();
    qr_cnt = 0;

    std::cout << "[  INFO] begin..." << std::endl;

    while(neighbors.size())
    {
        Neighbor proc_neighbor = neighbors.back();
        neighbors.pop_back();

        int s1 = proc_neighbor.from;
        int s2 = proc_neighbor.to;
        float soU = proc_neighbor.simlarity;

        std::cout << "[  INFO] current pairs soU = " << soU << std::endl;

        // if(soU<=0.6) break;
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
        
        src.copyTo(proc_seg, proc_msk);
        proc_seg = proc_seg(proc_bbox);
        cv::resize(proc_seg, proc_seg, cv::Size(120,120));

        cv::imshow("Debug", proc_seg);
        cv::waitKey(0);

        std::cout << "[  INFO] resize done." << std::endl;

        cv::Mat proc_hist_feature(1, 170, CV_32FC1);
        cv::Mat hist1, hist2;

        lbp.calc_hist(proc_seg, hist1, 1, 1, 10);
        lbp.calc_hist(cv::Mat(), hist2, 4, 4, 10);
        hist1.copyTo(proc_hist_feature.colRange(  0, 10));
        hist2.copyTo(proc_hist_feature.colRange( 10,170));

        std::cout << "[  INFO] hist done." << std::endl;

        float qr_flag = svm->predict(proc_hist_feature);
        // int qr_flag = 0;
        std::cout << "[  INFO] predict results: " << qr_flag << std::endl;
        if(qr_flag && soU>=0.75 && soU<=0.85)
        {
            is_qrseg[s1] = 1;
            is_qrseg[s2] = 1;
            qr_bbox.push_back(proc_bbox);
            ++qr_cnt;
        }
    }

    std::vector<Neighbor>().swap(neighbors);
    std::vector<cv::Rect>().swap(bboxs);
    std::vector<int>().swap(is_qrseg);

    return 0;



}
