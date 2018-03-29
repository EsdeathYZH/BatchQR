#include <vector>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

//////////////////////////////////////////////////////////////////////////////
// Disjoint Set
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
// Neighbors and Regions
//////////////////////////////////////////////////////////////////////////////

struct Neighbor
{
	int from;
	int to;
	float simlarity;

	Neighbor(int p1, int p2, float p3){
		from = p1; to = p2; simlarity = p3;
	}

	friend std::ostream& operator<<(std::ostream& os, const Neighbor& n);
	bool operator <(const Neighbor& n) const {
		return simlarity < n.simlarity;
	}
};


inline float calc_weight(cv::Rect bbox){
	return bbox.height>bbox.width ? (float)bbox.width/bbox.height : (float)bbox.height/bbox.width;
}

inline float calc_weight(cv::Rect bbox1, cv::Rect bbox2){
	return bbox1.area()>bbox2.area() ? (float)bbox2.area()/bbox1.area() : (float)bbox1.area()/bbox2.area();
}

struct Region
{
	int size;
	float simlarity;
	cv::Rect bbox;
	cv::Mat  mask;

	std::vector<int> nebs;

	Region(int p1, cv::Rect p2, cv::Mat p3){
		size = p1; bbox = p2; mask = p3.clone();
		simlarity = calc_weight(bbox) * (float)size / bbox.area();
		nebs.clear();
	}

	friend std::ostream& operator<<(std::ostream& os, const Region& r);
	bool operator <(const Region& r) const {
		return simlarity < r.simlarity;
	}

	void merge(const Region& r){
		nebs.insert(nebs.end(), r.nebs.begin(), r.nebs.end());
		// std::sort(nebs.negin(), nebs.end());
		nebs.erase(std::unique(nebs.begin(), nebs.end()), nebs.end());
	}

	int get_nb(){
		return nebs.size();
	}
	// void operator |(const Region& r) const {
	// 	size += r.size;
	// 	bbox |= r.bbox;
	// 	mask |= r.mask;
	// 	simlarity = calc_weight(bbox) * (float)size / bbox.area();
	// }
};

//////////////////////////////////////////////////////////////////////////////
// Local Binary Pattern
//////////////////////////////////////////////////////////////////////////////

class LBP
{
public:
	LBP();
	void calc_hist(cv::Mat src, cv::Mat& hist, int hs, int ws, int dim);
	void calc_hist_mask(cv::Mat src, cv::Mat& hist, int dim, cv::Mat mask);

private:
	cv::Mat lbp;
	uint8_t table[256];
	int get_hop_count(uint8_t code);
	void im2ulbp(cv::Mat src);

};


LBP::LBP()
{
	memset(table, 0, 256);

	uint8_t dim = 0;
	for (int i = 0; i < 256; i++)
		if (get_hop_count(i) <= 2)
			table[i] = ++dim;
}


int LBP::get_hop_count(uint8_t code)
{
	int k = 7;
	int cnt = 0;
	int a[8] = {0};

	while (code)
		a[k] = code & 1, code >>= 1, --k;

	for (k=0;k<7;k++)
		if (a[k]!=a[k + 1]) ++cnt;

	if (a[0]!=a[7]) ++cnt;

	return cnt;
}


void LBP::im2ulbp(cv::Mat src)
{
	if(src.depth()!=0)
	{
		std::cout << "[ Error]: unsupport Image Type." << std::endl;
		return;
	}
	else if(src.channels()==3)
		cv::cvtColor(src, src, cv::COLOR_BGR2GRAY);


	const int cols = src.cols;
	const int rows = src.rows;

	cv::copyMakeBorder(src,src,1,1,1,1,cv::BORDER_REPLICATE);
	
	lbp = cv::Mat::zeros(rows, cols, CV_8UC1);

	cv::MatIterator_<uint8_t> it = src.begin<uint8_t>();
	cv::MatIterator_<uint8_t> it_ = lbp.begin<uint8_t>();

	const int a = src.cols;
	const int b = lbp.cols;

	for(int i=1;i<=rows;i++)
		for(int j=1;j<=cols;j++)
		{
			uint8_t code = 0;
			uint8_t center = *(it+a*i+j);
			code |= (*(it+a*(i-1)+(j-1))>=center) << 7;
			code |= (*(it+a*(i  )+(j-1))>=center) << 6;
			code |= (*(it+a*(i+1)+(j-1))>=center) << 5;
			code |= (*(it+a*(i+1)+(j  ))>=center) << 4;
			code |= (*(it+a*(i+1)+(j+1))>=center) << 3;
			code |= (*(it+a*(i  )+(j+1))>=center) << 2;
			code |= (*(it+a*(i-1)+(j+1))>=center) << 1;
			code |= (*(it+a*(i-1)+(j  ))>=center);
			
			*(it_+b*(i-1)+(j-1)) = table[code];
		}
}


// hs: height of the cell, ws: width of the cell 
void LBP::calc_hist(cv::Mat src, cv::Mat& hist, int hs, int ws, int dim)
{
	if(!src.empty()) im2ulbp(src);

	int maskh = lbp.rows / hs;
	int maskw = lbp.cols / ws;

	float range[] = {0,59};
	const float* histRange = {range};
	const int histSize = dim;

	hist = cv::Mat::zeros(hs*ws, dim, CV_32FC1);

	for(int i=0; i<hs; ++i)
		for(int j=0; j<ws; ++j)
		{
			cv::Mat proc_hist(dim, 1, CV_32FC1);
			cv::Mat proc_seg = lbp(cv::Rect(j*maskw, i*maskh, maskw, maskh)).clone();
			cv::calcHist(&proc_seg, 1, 0, cv::Mat(), proc_hist, 1, &histSize, &histRange);
			cv::normalize(proc_hist, proc_hist, 1.0, 0.0, cv::NORM_L2);

			proc_hist = proc_hist.t();
			proc_hist.copyTo(hist.row(i*ws+j));
		}

	hist = hist.reshape(0,1);
}


void LBP::calc_hist_mask(cv::Mat src, cv::Mat& hist, int dim, cv::Mat mask)
{
	if(!src.empty()) im2ulbp(src);

	float range[] = {0,59};
	const float* histRange = {range};
	const int histSize = dim;

	hist = cv::Mat::zeros(dim, 1, CV_32FC1);

	cv::calcHist(&lbp, 1, 0, mask, hist, 1, &histSize, &histRange);
	cv::normalize(hist, hist, 1.0, 0.0, cv::NORM_L1);

	hist = hist.t();
}





//////////////////////////////////////////////////////////////////////////////
// Color Histogram
//////////////////////////////////////////////////////////////////////////////

void calc_color_hist(cv::Mat src, cv::Mat& hist, int hs, int ws, int dim)
{
	if(src.channels()!=3)
	{
		std::cout << "[RGB Error]: unsupport image type." << std::endl;
		return;
	}

	int maskh = src.rows / hs;
	int maskw = src.cols / ws;

	float range[] = {0,255};
	const float* histRange = {range};
	const int histSize = dim;

	std::vector<cv::Mat> bgr;
	cv::split(src, bgr);

	hist = cv::Mat::zeros(3*hs*ws, dim, CV_32FC1);
	for(int c=0; c<3; ++c)
		for(int i=0; i<hs; ++i)
			for(int j=0; j<ws; ++j)
			{
				cv::Mat proc_hist(dim, 1, CV_32FC1);
				cv::Mat proc_seg(bgr[c], cv::Rect(j*maskw, i*maskh, maskw, maskh));
				cv::calcHist(&proc_seg, 1, 0, cv::Mat(), proc_hist, 1, &histSize, &histRange);
				cv::normalize(proc_hist, proc_hist, 1.0, 0.0, cv::NORM_L2);
				
				proc_hist = proc_hist.t();
				proc_hist.copyTo(hist.row(c*hs*ws+i*ws+j));
			}

	hist = hist.reshape(0,1);
}


void calc_color_hist_mask(cv::Mat src, cv::Mat& hist, int dim, cv::Mat mask)
{
	if(src.channels()!=3)
	{
		std::cout << "[RGB Error]: unsupport image type." << std::endl;
		return;
	}

	float range[] = {0,255};
	const float* histRange = {range};
	const int histSize = dim;

	std::vector<cv::Mat> bgr;
	cv::split(src, bgr);

	hist = cv::Mat::zeros(3, dim, CV_32FC1);
	for(int c=0; c<3; ++c)
	{
		cv::Mat proc_hist;
		cv::calcHist(&bgr[c], 1, 0, mask, proc_hist, 1, &histSize, &histRange);
		cv::normalize(proc_hist, proc_hist, 1.0, 0.0, cv::NORM_L2);

		proc_hist = proc_hist.t();
		proc_hist.copyTo(hist.row(c));
	}

	hist = hist.reshape(0,1);
}



