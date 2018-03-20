#include <algorithm>
#include <vector>

#include <opencv2/core.hpp>
#include <opencv2/ml.hpp>
#include <opencv2/ximgproc/segmentation.hpp>

#include "qr_feature.hpp"

#define LOG_TAG "batchQRDetection/DetectionBasedTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))


std::string classifier_name("/storage/emulated/1/batchQR_model/qr_svc_model.xml");


class qr_region
{
private:
	cv::Mat img, seg;
	int nb_segs;
	int nb_qrcs;

public:
	qr_region() {load();}
	~qr_region() {}

	void init(cv::Mat& src);
	void get();

	std::vector<cv::Rect> qr_bbox;


private:
	void load();
	void graphseg();
	void graphseg(int minsize);

	int predict(int s);

	cv::Ptr<cv::ximgproc::segmentation::GraphSegmentation> gs;
	cv::Ptr<cv::ml::SVM> svm;

	LBP lbp;

	cv::Mat is_neighbor;

	std::vector<Neighbor> neighbors;
	std::vector<Region> regions;

	std::vector<std::vector<cv::Point> > points;
	std::vector<int> sizes;
	std::vector<int> is_qr_seg;
};





//////////////////////////////////////////////////////////////////////////////
// Private Function
//////////////////////////////////////////////////////////////////////////////

void qr_region::load()
{
	svm = cv::ml::SVM::load(classifier_name.c_str());
	
//	gs = cv::ximgproc::segmentation::createGraphSegmentation();
//	gs->setSigma(0.8);
//	gs->setK(2);
//	gs->setMinSize(1250);

	lbp = LBP();
}

void qr_region::graphseg()
{
	gs->processImage(img, seg);

	double min, max;
	cv::minMaxLoc(seg, &min, &max);
	nb_segs = (int)max + 1;
	
	// std::cout << "[  INFO] total: " << nb_segs << " segments." << std::endl;
}

void qr_region::graphseg(int minsize)
{
	gs->setMinSize(minsize);
	graphseg();
}

int qr_region::predict(int s)
{
	cv::Mat proc_reg;
	img.copyTo(proc_reg, regions[s].mask);
	proc_reg = proc_reg(regions[s].bbox);
	cv::resize(proc_reg, proc_reg, cv::Size(120,120));

	cv::Mat proc_hist_feature(1, 170, CV_32FC1);
    cv::Mat hist1, hist2;
	lbp.calc_hist(proc_reg, hist1, 1, 1, 10);
	lbp.calc_hist(cv::Mat(), hist2, 4, 4, 10);
	hist1.copyTo(proc_hist_feature.colRange(  0,  10));
	hist2.copyTo(proc_hist_feature.colRange(  10,170));

    return svm->predict(proc_hist_feature);
}





//////////////////////////////////////////////////////////////////////////////
// Public Function
//////////////////////////////////////////////////////////////////////////////

void qr_region::init(cv::Mat& src)
{
	img = src.clone();
	graphseg();

	points.resize(nb_segs);
	is_neighbor = cv::Mat::zeros(nb_segs, nb_segs, CV_8UC1);

	const int* pre_p = NULL;
	for(int i=0; i<seg.rows; ++i)
	{
		const int* p = seg.ptr<int>(i);
		for(int j=0; j<seg.cols; ++j)
		{
			points[p[j]].push_back(cv::Point(j,i));
			if(i>0 && j>0)
			{
				is_neighbor.at<char>(p[j], p[j-1]) = 1;
				is_neighbor.at<char>(p[j-1], p[j]) = 1;
				is_neighbor.at<char>(p[j], pre_p[j]) = 1;
				is_neighbor.at<char>(pre_p[j], p[j]) = 1;
				is_neighbor.at<char>(p[j], pre_p[j-1]) = 1;
				is_neighbor.at<char>(pre_p[j-1], p[j]) = 1;
			}
		}
		pre_p = p;
	}

	qr_bbox.clear();
	nb_qrcs = 0;
	is_qr_seg.resize(nb_segs);
	regions.clear();

	for(int s=0; s<nb_segs; ++s){
		is_qr_seg[s] = 0;
		regions.push_back(Region(points[s].size(), cv::boundingRect(points[s]), seg==s));

//		if(regions[s].simlarity>0.75 && predict(s))
//        {
//            is_qr_seg[s] = 1;
//            qr_bbox.push_back(regions[s].bbox);
//            ++nb_qrcs;
//		}
	}

	neighbors.clear();

	for(int s1=0; s1<nb_segs; ++s1)
		for(int s2=s1+1; s2<nb_segs; ++s2)
			if(is_neighbor.at<char>(s1,s2))
			{
				regions[s1].nebs.push_back(s2);
				regions[s2].nebs.push_back(s1);

				int size = regions[s1].size + regions[s2].size;
				cv::Rect bbox = regions[s1].bbox | regions[s2].bbox;

				float simlarity = calc_weight(bbox)*(float)size/bbox.area();
				neighbors.push_back(Neighbor(s1, s2, simlarity));
			}

	std::sort(neighbors.begin(), neighbors.end());	
}

void qr_region::get()
{
	UFS ufs(nb_segs);

	while(neighbors.size())
	{
		Neighbor proc_neighbor = neighbors.back();
		neighbors.pop_back();

		int r1 = ufs.find(proc_neighbor.from);
		int r2 = ufs.find(proc_neighbor.to);

		if(r1==r2) continue;
		if(r1!=proc_neighbor.from || r2!=proc_neighbor.to) continue;
		if(is_qr_seg[r1] || is_qr_seg[r2]) continue;

		int size = regions[r1].size + regions[r2].size;
		cv::Rect bbox = regions[r1].bbox | regions[r2].bbox;
		float soU = calc_weight(bbox) * (float)size / bbox.area();

		if(soU >= std::max(regions[r1].simlarity, regions[r2].simlarity))
		{
			std::vector<int> nebs = regions[r1].nebs;
			nebs.insert(nebs.end(), regions[r2].nebs.begin(), regions[r2].nebs.end());
			std::sort(nebs.begin(), nebs.end());
			nebs.erase(std::unique(nebs.begin(), nebs.end()), nebs.end());

			cv::Mat mask = regions[r1].mask | regions[r2].mask;

			ufs.join(r1,r2);

			int root_of_r12 = ufs.find(r1);
			regions[root_of_r12].size = size;
			regions[root_of_r12].bbox = bbox;
			regions[root_of_r12].simlarity = soU;
			regions[root_of_r12].mask = mask.clone();
			regions[root_of_r12].nebs = nebs;

			// Update the neighbors
			for(int i=0; i<nebs.size(); ++i)
			{
				if(nebs[i]==r1 || nebs[i]==r2) continue;
				int rx = ufs.find(nebs[i]);

				int size_n = size + regions[rx].size;
				cv::Rect bbox_n = bbox|regions[rx].bbox;
				neighbors.push_back(Neighbor(root_of_r12, rx, calc_weight(bbox_n)*(float)size_n/bbox_n.area() ));
			}

			if(soU>0.75 && predict(root_of_r12))
            {
                is_qr_seg[root_of_r12] = 1;
                qr_bbox.push_back(regions[root_of_r12].bbox);
                ++nb_qrcs;
            }

			std::sort(neighbors.begin(), neighbors.end());
		}
	}
}















