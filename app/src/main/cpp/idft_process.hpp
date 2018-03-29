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


class InValidIndex{};

bool cv_rect_comp_x(const cv::Rect& r1, const cv::Rect& r2) {return r1.x < r2.x;}
bool cv_rect_comp_y(const cv::Rect& r1, const cv::Rect& r2) {return r1.y < r2.y;}
bool cv_rect_comp_area(const cv::Rect& r1, const cv::Rect& r2) {return r1.area() < r2.area();}

//////////////////////////////////////////////////////////////////////////////
// UFS
//////////////////////////////////////////////////////////////////////////////

class UFS
{
public:
	UFS(int N_): N(N_) {
		std::vector<int>(N,-1).swap(parents);
	}

	int find(int p) const {
		if(parents[p]<0) return p;
		else return find(parents[p]);
	}
	void get(std::vector<int>& rs) const {
		std::vector<int>().swap(rs);
		for(int i=0; i<N; ++i) if(parents[i]<0) rs.push_back(i);
	}
	void join(int p1, int p2) {
		int r1 = find(p1);
		int r2 = find(p2);
		if(r1==r2) return;
		if(parents[r1]>parents[r2]) parents[r1] = r2;
		else{
			if(parents[r1]==parents[r2]) --parents[r1];
			parents[r2] = r1;
		}
	}

private:
	std::vector<int> parents;
	int N;
};


//////////////////////////////////////////////////////////////////////////////
// IDFT Transform for each cells
//////////////////////////////////////////////////////////////////////////////

void seg_idft(cv::Mat src, cv::Mat& dstMap, std::vector<cv::Rect>& dst, int dSize)
{
	cv::setUseOptimized(true);
    cv::setNumThreads(4);

	int cols = src.cols; // y
	int rows = src.rows; // x
	cv::cvtColor(src, src, cv::COLOR_BGR2GRAY);

	// make sure all cells in dSizexdSize sizes
	int padcol = cols % dSize ? dSize - cols % dSize : 0;
	int padrow = rows % dSize ? dSize - rows % dSize : 0;
	cv::copyMakeBorder(src, src, 0, padrow, 0, padcol, cv::BORDER_CONSTANT, cv::Scalar::all(0));

	cv::Rect dRect(0,0,dSize,dSize);
	dstMap = cv::Mat::zeros(src.rows/dSize, src.cols/dSize, CV_32SC1);
	std::vector<cv::Rect>().swap(dst);

	for(int i=0, idx=0; i<src.rows/dSize; ++i)
		for(int j=0; j<src.cols/dSize; ++j) {
			cv::Rect pRect(dRect + cv::Point(j*dSize,i*dSize));
			cv::Mat re = src(pRect), im = cv::Mat::zeros(dSize,dSize,CV_32F);
			re.convertTo(re, CV_32F);

			cv::Mat proc[] = {re, im}, resp;
			cv::merge(proc, 2, resp);
			cv::dft(resp, resp, cv::DFT_INVERSE|cv::DFT_SCALE);
			cv::split(resp, proc);
			cv::magnitude(proc[0], proc[1], resp);

			cv::threshold(resp, resp, 1.0, 1, cv::THRESH_BINARY);
			float dense = cv::sum(resp)[0]/(float)(dSize*dSize) >= 0.30 ? cv::sum(resp)[0]/(float)(dSize*dSize) : 0.0;
			if(dense) {
				dst.push_back(pRect);
				dstMap.at<int>(i,j) = ++idx;
			}
		}

	dstMap += cv::Scalar::all(-1);
}

inline void process_rects(cv::Rect& r1, cv::Rect& r2)
{
	cv::Rect rn = r1 & r2;

	if(rn.width<rn.height) {
		if(r1.x == rn.x) {
			r1.x += rn.width;
			r1.width -= rn.width;
			r2.width -= rn.width;					
		}
		else {
			r2.x += rn.width;
			r2.width -= rn.width;
			r1.width -= rn.width;
		}
	}
	else {
		if(r1.y == rn.y) {
			r1.y += rn.height;
			r1.height -= rn.height;
			r2.height -= rn.height;
		}
		else {
			r2.y += rn.height;
			r1.height -= rn.height;
			r2.height -= rn.height;
		}
	}		
}

//////////////////////////////////////////////////////////////////////////////
// IDFT Transform for each cells
//////////////////////////////////////////////////////////////////////////////

int idft_process(cv::Mat& img, std::vector<cv::Rect>& qr_bbox)
{
	LOGD("[  INFO]  cols: %d, rows: %d.", img.cols, img.rows);
    LOGD("[  INFO]  first write the image before processing.");
    cv::imwrite("/storage/emulated/0/batchQR_model/src.png", img);

    cv::Mat src = img.clone();
    cv::setUseOptimized(true);
    cv::setNumThreads(4);

    const int dSize = 25;
    const cv::Size minSize(dSize,dSize);
    const cv::Rect maxRect(cv::Point(0,0), src.size());

    LOGD("[  INFO]  processing IDFT for each cells...");

    cv::Mat idx_map, idx_map_;
    std::vector<cv::Rect> idx_rect;
    seg_idft(src, idx_map, idx_rect, dSize);
    cv::copyMakeBorder(idx_map, idx_map_, 0, 1, 0, 1, cv::BORDER_CONSTANT, cv::Scalar::all(-1));


    LOGD("[  INFO]  processing cells...");
	UFS ufs(idx_rect.size());
	for(int i=0; i<idx_map.rows; ++i) {
		const int* p1 = idx_map_.ptr<int>(i);
		const int* p2 = idx_map_.ptr<int>(i+1);

		for(int j=0; j<idx_map.cols; ++j) {
			if(p1[j]>=0 && p1[j+1]>=0) ufs.join(p1[j],p1[j+1]);
			if(p1[j]>=0 && p2[j]>=0) ufs.join(p1[j],p2[j]);
		}
	}

	for(int s=0; s<idx_rect.size(); ++s)
		idx_rect[ufs.find(s)] |= idx_rect[s];

	std::vector<int> rs;
	ufs.get(rs);


	LOGD("[  INFO]  post processing clusters for removing overlay.");
	std::vector<cv::Rect>().swap(qr_bbox);
	for(int r=0; r<rs.size(); ++r) {
		cv::Rect pRect = idx_rect[rs[r]];
		if(pRect.size() == minSize) continue;
		if(pRect.height/pRect.width>2 || pRect.width/pRect.height>2) continue;

		pRect += cv::Point(-1*dSize,-1*dSize);
		pRect += cv::Size(2*dSize,2*dSize);
		pRect &= maxRect;
		
		qr_bbox.push_back(pRect);
	}

	std::sort(qr_bbox.begin(), qr_bbox.end(), cv_rect_comp_area);

	for(int i=0; i<qr_bbox.size(); ++i) {
		cv::Rect& r1 = qr_bbox[i];
		for(int j=i+1; j<qr_bbox.size(); ++j) {
			cv::Rect& r2 = qr_bbox[j];
			cv::Rect rn = r1&r2;
			if(rn.area()==0) continue;
			process_rects(r1, r2);
		}
	}

	return 0;
}




