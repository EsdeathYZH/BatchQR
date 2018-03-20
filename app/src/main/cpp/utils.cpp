#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

void resize_within_pixel(cv::Mat src, cv::Mat& dst, int maxSize)
{
	int cols = src.cols;
	int rows = src.rows;
	if(cols<=maxSize && rows<=maxSize) {
		dst = src; return;
	}

	int maxLength = cols>=rows ? cols : rows;
	float resizeRatio = float(maxSize) / maxLength;
	cv::resize(src, dst, cv::Size(0,0), resizeRatio, resizeRatio);
}
