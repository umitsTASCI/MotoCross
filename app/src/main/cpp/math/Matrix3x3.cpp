#include "Matrix3x3.hpp"

namespace oemcluster::math {

Mat3d Mat3d::identity() noexcept {
    Mat3d res{};
    res.m[0][0] = 1.0; res.m[0][1] = 0.0; res.m[0][2] = 0.0;
    res.m[1][0] = 0.0; res.m[1][1] = 1.0; res.m[1][2] = 0.0;
    res.m[2][0] = 0.0; res.m[2][1] = 0.0; res.m[2][2] = 1.0;
    return res;
}

Mat3d Mat3d::zero() noexcept {
    Mat3d res{};
    for (int r = 0; r < 3; ++r) {
        for (int c = 0; c < 3; ++c) {
            res.m[r][c] = 0.0;
        }
    }
    return res;
}

Mat3d Mat3d::fromRows(const Vec3d& r0, const Vec3d& r1, const Vec3d& r2) noexcept {
    Mat3d res{};
    res.m[0][0] = r0.x; res.m[0][1] = r0.y; res.m[0][2] = r0.z;
    res.m[1][0] = r1.x; res.m[1][1] = r1.y; res.m[1][2] = r1.z;
    res.m[2][0] = r2.x; res.m[2][1] = r2.y; res.m[2][2] = r2.z;
    return res;
}

Mat3d Mat3d::outerProduct(const Vec3d& a, const Vec3d& b) noexcept {
    Mat3d res{};
    res.m[0][0] = a.x * b.x; res.m[0][1] = a.x * b.y; res.m[0][2] = a.x * b.z;
    res.m[1][0] = a.y * b.x; res.m[1][1] = a.y * b.y; res.m[1][2] = a.y * b.z;
    res.m[2][0] = a.z * b.x; res.m[2][1] = a.z * b.y; res.m[2][2] = a.z * b.z;
    return res;
}

Vec3d Mat3d::multiply(const Vec3d& v) const noexcept {
    return {
        m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z,
        m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z,
        m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z
    };
}

Mat3d Mat3d::multiply(const Mat3d& rhs) const noexcept {
    Mat3d res = Mat3d::zero();
    for (int r = 0; r < 3; ++r) {
        for (int c = 0; c < 3; ++c) {
            res.m[r][c] = m[r][0] * rhs.m[0][c] +
                          m[r][1] * rhs.m[1][c] +
                          m[r][2] * rhs.m[2][c];
        }
    }
    return res;
}

Mat3d Mat3d::transposed() const noexcept {
    Mat3d res{};
    for (int r = 0; r < 3; ++r) {
        for (int c = 0; c < 3; ++c) {
            res.m[r][c] = m[c][r];
        }
    }
    return res;
}

void Mat3d::addScaledOuterProduct(const Vec3d& v, double scale) noexcept {
    m[0][0] += scale * v.x * v.x;
    m[0][1] += scale * v.x * v.y;
    m[0][2] += scale * v.x * v.z;
    m[1][0] += scale * v.y * v.x;
    m[1][1] += scale * v.y * v.y;
    m[1][2] += scale * v.y * v.z;
    m[2][0] += scale * v.z * v.x;
    m[2][1] += scale * v.z * v.y;
    m[2][2] += scale * v.z * v.z;
}

Vec3d Mat3d::largestEigenvectorSymmetric(int maxSweeps) const noexcept {
    Mat3d A = *this;
    Mat3d V = Mat3d::identity();

    for (int sweep = 0; sweep < maxSweeps; ++sweep) {
        // En büyük köşegen dışı elemanı bul (p, q)
        int p = 0, q = 1;
        double maxOffDiag = std::abs(A.m[0][1]);
        if (std::abs(A.m[0][2]) > maxOffDiag) {
            maxOffDiag = std::abs(A.m[0][2]);
            p = 0; q = 2;
        }
        if (std::abs(A.m[1][2]) > maxOffDiag) {
            maxOffDiag = std::abs(A.m[1][2]);
            p = 1; q = 2;
        }

        if (maxOffDiag < 1e-11) {
            break;
        }

        const double app = A.m[p][p];
        const double aqq = A.m[q][q];
        const double apq = A.m[p][q];

        const double tau = (aqq - app) / (2.0 * apq);
        const double t = (tau >= 0.0)
            ? (1.0 / (tau + std::sqrt(1.0 + tau * tau)))
            : (-1.0 / (-tau + std::sqrt(1.0 + tau * tau)));
        const double c = 1.0 / std::sqrt(1.0 + t * t);
        const double s = t * c;

        A.m[p][p] = app - t * apq;
        A.m[q][q] = aqq + t * apq;
        A.m[p][q] = 0.0;
        A.m[q][p] = 0.0;

        for (int r = 0; r < 3; ++r) {
            if (r != p && r != q) {
                const double arp = A.m[r][p];
                const double arq = A.m[r][q];
                A.m[r][p] = c * arp - s * arq;
                A.m[p][r] = A.m[r][p];
                A.m[r][q] = s * arp + c * arq;
                A.m[q][r] = A.m[r][q];
            }
        }

        for (int r = 0; r < 3; ++r) {
            const double vrp = V.m[r][p];
            const double vrq = V.m[r][q];
            V.m[r][p] = c * vrp - s * vrq;
            V.m[r][q] = s * vrp + c * vrq;
        }
    }

    // En büyük özdeğere sahip sütunu seç
    int bestCol = 0;
    double bestVal = A.m[0][0];
    if (A.m[1][1] > bestVal) {
        bestVal = A.m[1][1];
        bestCol = 1;
    }
    if (A.m[2][2] > bestVal) {
        bestCol = 2;
    }

    Vec3d eigenVec{V.m[0][bestCol], V.m[1][bestCol], V.m[2][bestCol]};
    return eigenVec.normalized();
}

} // namespace oemcluster::math

