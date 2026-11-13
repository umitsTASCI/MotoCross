#pragma once

#include <array>
#include <complex>
#include <cstddef>

namespace oemcluster::acoustic {

/**
 * Bölüm 3.3 Katman 1: Akustik FFT & Harmonik Çarpım Spektrumu (HPS)
 *
 * - 48 kHz örnekleme frekansı
 * - 40 Hz 2. Derece Butterworth Yüksek Geçiren Filtre (Rüzgar gürültüsü kesici)
 * - 4096-nokta Hann Pencereli Radix-2 FFT + Parabolik Tepe İnterpolasyonu
 * - 4-Harmonik Çarpım Spektrumu: Y(f) = \prod_{m=1}^{4} |X(m * f)|
 * - RPM_acoustic = (f_peak * 120) / N_cyl
 */
class AcousticFftRpmDetector {
public:
    static constexpr size_t FFT_SIZE = 4096;
    static constexpr double SAMPLE_RATE_HZ = 48000.0;
    static constexpr double HIGH_PASS_CUTOFF_HZ = 18.0;

    AcousticFftRpmDetector() noexcept;

    void setEngineConfiguration(int cylinderCount, double idleRpm, double revLimiterRpm) noexcept;

    /**
     * 48 kHz PCM ses örneklerini (float [-1.0 .. +1.0]) besler ve
     * pencere dolduğunda FFT + HPS çalıştırarak akustik RPM'i günceller.
     */
    void processAudioSamples(const float* pcmSamples, size_t sampleCount) noexcept;

    double getDetectedAcousticRpm() const noexcept { return detectedAcousticRpm_; }
    double getSignalConfidence() const noexcept { return confidenceScore_; }
    bool isValid() const noexcept { return confidenceScore_ > 0.35; }

private:
    void initHighPassFilter() noexcept;
    void initHannWindow() noexcept;
    float applyHighPass40Hz(float sample) noexcept;
    void executeFftAndHps() noexcept;
    static void radix2CooleyTukeyFft(std::array<std::complex<double>, FFT_SIZE>& buf) noexcept;

    int cylinderCount_{1};
    double minValidRpm_{1000.0};
    double maxValidRpm_{15000.0};

    // 40 Hz Butterworth Biquad katsayıları ve durum değişkenleri
    double b0_{1.0}, b1_{0.0}, b2_{0.0}, a1_{0.0}, a2_{0.0};
    double x1_{0.0}, x2_{0.0}, y1_{0.0}, y2_{0.0};

    std::array<double, FFT_SIZE> hannWindow_{};
    std::array<double, FFT_SIZE> audioRing_{};
    size_t writePos_{0};
    size_t samplesSinceLastFft_{0};

    double detectedAcousticRpm_{0.0};
    double confidenceScore_{0.0};
};

} // namespace oemcluster::acoustic

