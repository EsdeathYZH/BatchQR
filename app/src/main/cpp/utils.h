#include <string>
#include <vector>

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>


template <typename T>
std::string to_string(T value);

bool cv_rect_comp_x(const cv::Rect& r1, const cv::Rect& r2);
bool cv_rect_comp_y(const cv::Rect& r1, const cv::Rect& r2);
bool cv_rect_comp_area(const cv::Rect& r1, const cv::Rect& r2);

float resize_within_pixel(cv::Mat src, cv::Mat& dst, int maxSize);

class InValidIndex{};
class InValidInput{};
class ExceedMaxLength{};

class UFS
{
public:
	UFS(int N_);
	int find(int s) const;
	void get(std::vector<int> rs) const;
	void join(int s1, int s2);

private:
	std::vector<int> parents;
	int N;
};

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

void calc_color_hist_mask(cv::Mat src, cv::Mat& hist, int dim, cv::Mat mask);
void calc_color_hist(cv::Mat src, cv::Mat& hist, int hs, int ws, int dim);