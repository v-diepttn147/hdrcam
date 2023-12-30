#include "lowlight.h"

#define TAG "LowLightSDK"
#define PRINT_D(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define PRINT_E(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

double get_us(struct timeval t) { return (t.tv_sec * 1000000 + t.tv_usec); }

LowLightSDK* LowLightSDK::instance = nullptr;

//void rotateImage(cv::Mat& src, cv::Mat& dst, double angle)
//{
//    cv::Point2f pt(src.cols/2., src.rows/2.);
//    cv::Mat r = getRotationMatrix2D(pt, angle, 1.0);
//    cv::Rect2f bbox = cv::RotatedRect(cv::Point2f(), src.size(), angle).boundingRect2f();
//    r.at<double>(0,2) += bbox.width / 2.0 - src.cols / 2.0;
//    r.at<double>(1,2) += bbox.height / 2.0 - src.rows / 2.0;
//    warpAffine(src, dst, r, cv::Size(src.cols, src.rows));
//}
//
//cv::Mat adjustSaturation(cv::Mat image) {
//    cv::cvtColor(image, image, cv::COLOR_RGB2HSV);
//    cv::Mat channels[3];
//    cv::split(image, channels);
//
//    for (int i = 0; i < channels[1].rows; i++) {
//        float* row = channels[1].ptr<float>(i);
//        for (int j = 0; j < channels[1].cols; j++) {
//            row[j] *= 1.2f;
//        }
//    }
//    cv::merge(channels, 3, image);
//    cv::cvtColor(image, image, cv::COLOR_HSV2RGB);
//    return image;
//}

std::vector<char> ReadAsset(AAssetManager *asset_manager, const std::string &name) {
    std::vector<char> buf;
    AAsset *asset = AAssetManager_open(asset_manager, name.c_str(), AASSET_MODE_UNKNOWN);
    if (asset != nullptr) {
        PRINT_D("Open asset %s OK", name.c_str());
        off_t buf_size = AAsset_getLength(asset);
        buf.resize(buf_size + 1, 0);
        auto num_read = AAsset_read(asset, buf.data(), buf_size);
        PRINT_D("Read asset %s OK", name.c_str());

        if (num_read == 0)
            buf.clear();
        AAsset_close(asset);
        PRINT_D("Close asset %s OK", name.c_str());
    }
    return buf;
}

template<typename CharT, typename TraitsT = std::char_traits<CharT> >
struct VectorStreamBuf : public std::basic_streambuf<CharT, TraitsT> {
    explicit VectorStreamBuf(std::vector<CharT> &vec) {
        this->setg(vec.data(), vec.data(), vec.data() + vec.size());
    }
};

class ModelReader : public caffe2::serialize::ReadAdapterInterface {
public:
    explicit ModelReader(const std::vector<char> &buf) : buf_(&buf) {}

    ~ModelReader() override {};

    virtual size_t size() const override {
        return buf_->size();
    }

    virtual size_t read(uint64_t pos, void *buf, size_t n, const char *what)
    const override {
        std::copy_n(buf_->begin() + pos, n, reinterpret_cast<char *>(buf));
        return n;
    }

private:
    const std::vector<char> *buf_;
};

LowLightSDK::LowLightSDK(const char* hdrnet_path) {
    PRINT_D("Init models");
    torch::jit::Module hdrnet_model = torch::jit::load(hdrnet_path);
}

LowLightSDK::LowLightSDK(AAssetManager* mgr, const char* hdrnet_path) {
    PRINT_D("Init models");

    AAsset* hdrnet_model_file = AAssetManager_open(mgr, hdrnet_path, AASSET_MODE_BUFFER);
    auto hdrnet_model_asset = ReadAsset(mgr, hdrnet_path);

    hdrnet_model = torch::jit::load(std::make_unique<ModelReader>(hdrnet_model_asset));
    hdrnet_model.eval();
}

LowLightSDK::~LowLightSDK() {
    delete instance;
}

jbyteArray LowLightSDK::enhance(JNIEnv *env, jbyteArray input, jint width, jint height) {
    gettimeofday(&start_time, nullptr);
    PRINT_D("Load input tensor (%d %d)", width, height);
    jbyte *inputBuffer = env->GetByteArrayElements(input, JNI_FALSE);
    cv::Mat img_tensor(height, width, CV_8UC3, inputBuffer);

    // test inference hdrnet model
//    cv::Mat img_low;
//    cv::Mat img_high;
//    cv::resize(img_tensor, img_low, cv::Size(256, 256), 0, 0, cv::INTER_AREA);
//    cv::resize(img_tensor, img_high, cv::Size(), 0.5, 0.5, cv::INTER_AREA);
//    img_high = preprocessImage(img_high);
    cv::Mat img;
    cv::resize(img_tensor, img, cv::Size(1500, 1992), 0, 0, cv::INTER_LINEAR);
    img = preprocessImage(img);

    // Convert image to 32 bits, Float, 3 channels
//    img_low.convertTo(img_low, CV_32FC3, 1.f/255);
//    img_high.convertTo(img_high, CV_32FC3, 1.f/255);
    img.convertTo(img, CV_32FC3, 1.f/255);

//    torch::Tensor low = torch::from_blob(img_low.data, {256, 256, 3});
//    torch::Tensor high = torch::from_blob(img_high.data, { img_high.rows, img_high.cols, 3});
    torch::Tensor img_pt = torch::from_blob(img.data, { img.rows, img.cols, 3});

//    low = low.permute({ 2, 0, 1 });
//    high = high.permute({ 2, 0, 1 });
    img_pt = img_pt.permute({ 2, 0, 1 });
//    low.unsqueeze_(0);
//    high.unsqueeze_(0);
    img_pt.unsqueeze_(0);

    PRINT_D("Start forward pass");
    struct timeval start, end;
    gettimeofday(&start, nullptr);

    at::Tensor t_out = hdrnet_model.forward({img_pt}).toTensor();

    gettimeofday(&end, nullptr);
    PRINT_D("Forward pass time: %d ms", (int) (get_us(end) - get_us(start)) / 1000);

    t_out = t_out.squeeze().detach().permute({1, 2, 0}).contiguous();
    t_out = t_out.mul(255.0).clamp(0, 255).to(torch::kU8);
//    cv::Mat resultImg(img.rows, img.cols, CV_8UC3);
    cv::Mat resultImg(1992, 1500, CV_8UC3);
    std::memcpy((void *) resultImg.data, t_out.data_ptr(), sizeof(torch::kU8) * t_out.numel());

    resultImg = postprocessImage(resultImg);
    cv::resize(resultImg, resultImg, cv::Size(outputImageWidth, outputImageHeight), 0, 0, cv::INTER_LINEAR);

    jbyteArray output = env->NewByteArray(outputImageHeight * outputImageWidth * 3);
    jbyte *outputBuffer = env->GetByteArrayElements(output, JNI_FALSE);
    memcpy(outputBuffer, resultImg.data, outputImageHeight * outputImageWidth * 3);

    gettimeofday(&stop_time, nullptr);
    elapsedMs = (int) (get_us(stop_time) - get_us(start_time)) / 1000;
    return output;
}

cv::Mat LowLightSDK::preprocessImage(cv::Mat image) {
    if (image.cols > image.rows) {
        cv::transpose(image, image);
        PRINT_D("Rotate -> (%d %d)", image.cols, image.rows);
        rotate = true;
    }

    return image;
}

cv::Mat LowLightSDK::postprocessImage(cv::Mat image) {
    if (rotate) {
        cv::transpose(image, image);
        PRINT_D("Rotate -> (%d %d)", image.cols, image.rows);
        rotate = false;
    }
    return image;
}

