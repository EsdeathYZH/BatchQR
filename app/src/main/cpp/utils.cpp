#include "utils.h"

template <typename T>
std::string to_string(T value)
{
    std::ostringstream os;
    os << value;
    return os.str();
}

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

bool cv_rect_comp_x(const cv::Rect& r1, const cv::Rect& r2) {return r1.x < r2.x;}
bool cv_rect_comp_y(const cv::Rect& r1, const cv::Rect& r2) {return r1.y < r2.y;}
bool cv_rect_comp_area(const cv::Rect& r1, const cv::Rect& r2) {return r1.area() < r2.area();}



