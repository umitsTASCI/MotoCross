#include "AcousticFftRpmDetector.hpp"
#include <cmath>
#include <algorithm>

namespace oemcluster::acoustic {

namespace {
constexpr double PI = 3.14159265358979323846;
}

AcousticFftRpmDetector::AcousticFftRpmDetector() noexcept {
    initHighPassFilter();
    initHannWindow();
}

void AcousticFftRpmDetector::setEngineConfiguration(
    int cylinderCount,
    double idleRpm,
    double revLimiterRpm
) noexcept {
    cylinderCount_ = std::clamp(cylinderCount, 1, 6);
    minValidRpm_ = std::max(700.0, idleRpm * 0.70);
    maxValidRpm_ = std::min(19000.0, revLimiterRpm * 1.05);
}

void AcousticFftRpmDetector::initHighPassFilter() noexcept {
    // 40 Hz 2. Derece Butterworth High-Pass Filter (fs = 48000 Hz)
    const double w0 = 2.0 * PI * HIGH_PASS_CUTOFF_HZ / SAMPLE_RATE_HZ;
    const double cosW0 = std::cos(w0);
    const double sinW0 = std::sin(w0);
    const double alpha = sinW0 / std::sqrt(2.0);

    const double a0 = 1.0 + alpha;
    b0_ = ((1.0 + cosW0) * 0.5) / a0;
    b1_ = -(1.0 + cosW0) / a0;
    b2_ = ((1.0 + cosW0) * 0.5) / a0;
    a1_ = (-2.0 * cosW0) / a0;
    a2_ = (1.0 - alpha) / a0;

    x1_ = x2_ = y1_ = y2_ = 0.0;
}

void AcousticFftRpmDetector::initHannWindow() noexcept {
    for (size_t i = 0; i < FFT_SIZE; ++i) {
        hannWindow_[i] = 0.5 * (1.0 - std::cos((2.0 * PI * static_cast<double>(i)) /
                                               static_cast<double>(FFT_SIZE - 1)));
    }
}

float AcousticFftRpmDetector::applyHighPass40Hz(float sample) noexcept {
    const double x0 = static_cast<double>(sample);
    const double y0 = b0_ * x0 + b1_ * x1_ + b2_ * x2_ - a1_ * y1_ - a2_ * y2_;
    x2_ = x1_;
    x1_ = x0;
    y2_ = y1_;
    y1_ = y0;
    return static_cast<float>(y0);
}

void AcousticFftRpmDetector::processAudioSamples(const float* pcmSamples, size_t sampleCount) noexcept {
    if (!pcmSamples || sampleCount == 0) return;

    for (size_t i = 0; i < sampleCount; ++i) {
        const float filtered = applyHighPass40Hz(pcmSamples[i]);
        audioRing_[writePos_] = static_cast<double>(filtered);
        writePos_ = (writePos_ + 1) % FFT_SIZE;
        samplesSinceLastFft_++;

        // Her 1024 örnekte bir (~46.8 Hz güncelleme hızı, %75 örtüşmeli STFT) HPS çalıştır
        if (samplesSinceLastFft_ >= 1024) {
            samplesSinceLastFft_ = 0;
            executeFftAndHps();
        }
    }
}

void AcousticFftRpmDetector::radix2CooleyTukeyFft(
    std::array<std::complex<double>, FFT_SIZE>& buf
) noexcept {
    // Bit-reversal permütasyonu
    size_t j = 0;
    for (size_t i = 1; i < FFT_SIZE; ++i) {
        size_t bit = FFT_SIZE >> 1;
        while (j & bit) {
            j ^= bit;
            bit >>= 1;
        }
        j ^= bit;
        if (i < j) {
            std::swap(buf[i], buf[j]);
        }
    }

    // Kelebek (Butterfly) aşamaları
    for (size_t len = 2; len <= FFT_SIZE; len <<= 1) {
        const double ang = -2.0 * PI / static_cast<double>(len);
        const std::complex<double> wlen(std::cos(ang), std::sin(ang));
        for (size_t i = 0; i < FFT_SIZE; i += len) {
            std::complex<double> w(1.0, 0.0);
            const size_t half = len >> 1;
            for (size_t k = 0; k < half; ++k) {
                const std::complex<double> u = buf[i + k];
                const std::complex<double> v = buf[i + k + half] * w;
                buf[i + k] = u + v;
                buf[i + k + half] = u - v;
                w *= wlen;
            }
        }
    }
}

void AcousticFftRpmDetector::executeFftAndHps() noexcept {
    std::array<std::complex<double>, FFT_SIZE> fftBuf{};
    double meanVal = 0.0;
    for (size_t i = 0; i < FFT_SIZE; ++i) {
        meanVal += audioRing_[(writePos_ + i) % FFT_SIZE];
    }
    meanVal /= static_cast<double>(FFT_SIZE);

    double rmsEnergy = 0.0;
    for (size_t i = 0; i < FFT_SIZE; ++i) {
        const size_t idx = (writePos_ + i) % FFT_SIZE;
        const double val = audioRing_[idx] - meanVal;
        rmsEnergy += val * val;
        fftBuf[i] = std::complex<double>(val * hannWindow_[i], 0.0);
    }
    rmsEnergy = std::sqrt(rmsEnergy / static_cast<double>(FFT_SIZE));
    if (rmsEnergy < 1e-4) {
        confidenceScore_ *= 0.85;
        return;
    }

    radix2CooleyTukeyFft(fftBuf);

    constexpr size_t HALF_BINS = FFT_SIZE / 2;
    std::array<double, HALF_BINS> magnitude{};
    for (size_t k = 0; k < HALF_BINS; ++k) {
        magnitude[k] = std::abs(fftBuf[k]) + 1e-9;
    }

    // -------------------------------------------------------------------------
    // Bölüm 3.3 Katman 1: Harmonik Çarpım Spektrumu (HPS)
    // Y(f) = \prod_{m=1}^{4} |X(m * f)|
    // RPM_acoustic = (f_peak * 120) / N_cyl  =>  f = (RPM * N_cyl) / 120
    // -------------------------------------------------------------------------
    const double binHz = SAMPLE_RATE_HZ / static_cast<double>(FFT_SIZE);
    const double minFiringHz = std::max(HIGH_PASS_CUTOFF_HZ, (minValidRpm_ * cylinderCount_) / 120.0);
    const double maxFiringHz = (maxValidRpm_ * cylinderCount_) / 120.0;

    const size_t minBin = std::max<size_t>(2, static_cast<size_t>(std::floor(minFiringHz / binHz)));
    const size_t maxBin = std::min<size_t>((HALF_BINS / 4) - 2, static_cast<size_t>(std::ceil(maxFiringHz / binHz)));

    if (minBin >= maxBin) return;

    size_t peakBin = minBin;
    double maxHpsVal = 0.0;
    double sumHpsVal = 0.0;

    std::array<double, HALF_BINS / 4> hpsSpectrum{};
    for (size_t k = minBin; k <= maxBin; ++k) {
        // Log-HPS geometrik ortalama: sessiz sinyalde taşma/alt-taşma önler
        const double logGeo = 0.25 * (
            std::log(magnitude[k]) +
            std::log(magnitude[2 * k]) +
            std::log(magnitude[3 * k]) +
            std::log(magnitude[4 * k])
        );
        const double y = std::exp(logGeo);
        hpsSpectrum[k] = y;
        sumHpsVal += y;
        if (y > maxHpsVal) {
            maxHpsVal = y;
            peakBin = k;
        }
    }

    // Alt-bin (Sub-bin) parabolik tepe interpolasyonu
    double fineBin = static_cast<double>(peakBin);
    if (peakBin > minBin && peakBin < maxBin) {
        const double alpha = std::log(hpsSpectrum[peakBin - 1] + 1e-18);
        const double beta  = std::log(hpsSpectrum[peakBin]     + 1e-18);
        const double gamma = std::log(hpsSpectrum[peakBin + 1] + 1e-18);
        const double denom = alpha - 2.0 * beta + gamma;
        if (std::abs(denom) > 1e-9) {
            const double delta = 0.5 * (alpha - gamma) / denom;
            fineBin += std::clamp(delta, -0.5, 0.5);
        }
    }

    const double fPeakHz = fineBin * binHz;
    const double rawRpm = (fPeakHz * 120.0) / static_cast<double>(cylinderCount_);

    const double avgHps = sumHpsVal / static_cast<double>(maxBin - minBin + 1);
    const double snrRatio = (avgHps > 1e-18) ? (maxHpsVal / avgHps) : 1.0;

    confidenceScore_ = std::clamp((snrRatio - 3.0) / 18.0, 0.0, 1.0);
    if (confidenceScore_ > 0.25) {
        detectedAcousticRpm_ = (detectedAcousticRpm_ <= 0.0)
            ? rawRpm
            : (0.65 * detectedAcousticRpm_ + 0.35 * rawRpm);
    }
}

} // namespace oemcluster::acoustic

