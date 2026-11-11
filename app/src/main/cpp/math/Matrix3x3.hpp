#pragma once

#include <array>
#include <cmath>
#include <algorithm>

namespace oemcluster::math {

struct Vec3d {
    double x{0.0};
    double y{0.0};
    double z{0.0};

    constexpr Vec3d() = default;
    constexpr Vec3d(double x_, double y_, double z_) : x(x_), y(y_), z(z_) {}

    constexpr Vec3d operator+(const Vec3d& rhs) const noexcept {
        return {x + rhs.x, y + rhs.y, z + rhs.z};
    }

    constexpr Vec3d operator-(const Vec3d& rhs) const noexcept {
        return {x - rhs.x, y - rhs.y, z - rhs.z};
    }

    constexpr Vec3d operator*(double scalar) const noexcept {
        return {x * scalar, y * scalar, z * scalar};
    }

    constexpr Vec3d& operator+=(const Vec3d& rhs) noexcept {
        x += rhs.x;
        y += rhs.y;
        z += rhs.z;
        return *this;
    }

    constexpr double dot(const Vec3d& rhs) const noexcept {
        return x * rhs.x + y * rhs.y + z * rhs.z;
    }

    constexpr Vec3d cross(const Vec3d& rhs) const noexcept {
        return {
            y * rhs.z - z * rhs.y,
            z * rhs.x - x * rhs.z,
            x * rhs.y - y * rhs.x
        };
    }

    double normSquared() const noexcept {
        return x * x + y * y + z * z;
    }

    double norm() const noexcept {
        return std::sqrt(normSquared());
    }

    Vec3d normalized(double eps = 1e-9) const noexcept {
        const double n = norm();
        if (n < eps) {
            return {0.0, 0.0, 0.0};
        }
        return {x / n, y / n, z / n};
    }
};

struct Mat3d {
    // Row-major 3x3 matris: m[row][col]
    double m[3][3]{
        {1.0, 0.0, 0.0},
        {0.0, 1.0, 0.0},
        {0.0, 0.0, 1.0}
    };

    static Mat3d identity() noexcept;
    static Mat3d zero() noexcept;
    static Mat3d fromRows(const Vec3d& r0, const Vec3d& r1, const Vec3d& r2) noexcept;
    static Mat3d outerProduct(const Vec3d& a, const Vec3d& b) noexcept;

    Vec3d multiply(const Vec3d& v) const noexcept;
    Mat3d multiply(const Mat3d& rhs) const noexcept;
    Mat3d transposed() const noexcept;
    void addScaledOuterProduct(const Vec3d& v, double scale) noexcept;

    /**
     * Simetrik 3x3 kovaryans matrisi için Jacobi Özdeğer / Özvektör Çözücü.
     * PCA (Temel Bileşenler Analizi) ile motosikletin ileri ivme eksenini (X_b)
     * en büyük özdeğere karşılık gelen özvektör olarak bulur.
     */
    Vec3d largestEigenvectorSymmetric(int maxSweeps = 25) const noexcept;
};

} // namespace oemcluster::math

