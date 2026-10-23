#include "YieldCalculator.hpp"
#include <algorithm>
#include <cmath>

namespace finpay {

static double round2(double v) {
    return std::round(v * 100.0) / 100.0;
}

FixedIncomeSimulation YieldCalculator::simulateFixedIncome(
    double investedAmount,
    double annualSelicPercent,
    double annualCdiPercent
) {
    double p = std::max(0.0, investedAmount);
    double selic = std::max(0.0, annualSelicPercent) / 100.0;
    double cdi = std::max(0.0, annualCdiPercent) / 100.0;

    // Taxa equivalente mensal: (1 + anual)^(1/12) - 1
    double monthlyCdi = std::pow(1.0 + cdi, 1.0 / 12.0) - 1.0;

    // 1. Tesouro Selic / CDB 100% CDI (Alíquota média de IR regressivo: 17,5% em 1 ano, 22,5% no 1º mês)
    double selicMonthlyGross = p * monthlyCdi;
    double selicMonthlyNet = selicMonthlyGross * (1.0 - 0.175);
    double selicAnnualGross = p * cdi;
    double selicAnnualNet = selicAnnualGross * (1.0 - 0.175);

    // 2. LCI/LCA Isenta de IR (92% do CDI líquido, ou equivalente a CDB 110% CDI líquido)
    double lciMonthlyNet = p * (monthlyCdi * 0.92);
    double lciAnnualNet = p * (cdi * 0.92);

    // 3. Poupança tradicional (Quando Selic > 8,5% a.a.: 0,5% a.m. + TR ~ 0,06% = 0,563% a.m.)
    double poupancaMonthlyRate = (selic > 0.085) ? 0.00563 : (monthlyCdi * 0.70);
    double poupancaAnnualRate = std::pow(1.0 + poupancaMonthlyRate, 12.0) - 1.0;
    double poupancaMonthlyNet = p * poupancaMonthlyRate;
    double poupancaAnnualNet = p * poupancaAnnualRate;

    return FixedIncomeSimulation {
        round2(p),
        round2(selicMonthlyNet),
        round2(selicAnnualNet),
        round2(lciMonthlyNet),
        round2(lciAnnualNet),
        round2(poupancaMonthlyNet),
        round2(poupancaAnnualNet)
    };
}

GoalProjection YieldCalculator::calculateGoalProjection(
    double targetAmount,
    double currentAmount,
    int remainingMonths,
    double annualCdiPercent
) {
    double target = std::max(1.0, targetAmount);
    double current = std::max(0.0, currentAmount);
    int n = std::max(1, remainingMonths);

    double cdi = std::max(0.0, annualCdiPercent) / 100.0;
    double monthlyNetRate = (std::pow(1.0 + cdi, 1.0 / 12.0) - 1.0) * (1.0 - 0.175);

    double passiveMonthly = current * monthlyNetRate;
    double futureValueCurrent = current * std::pow(1.0 + monthlyNetRate, n);
    double gap = std::max(0.0, target - futureValueCurrent);

    double requiredMonthly = 0.0;
    if (gap > 0.0) {
        if (monthlyNetRate > 0.0001) {
            requiredMonthly = gap * monthlyNetRate / (std::pow(1.0 + monthlyNetRate, n) - 1.0);
        } else {
            requiredMonthly = gap / n;
        }
    }

    double pct = std::min(100.0, (current / target) * 100.0);

    return GoalProjection {
        round2(passiveMonthly),
        round2(requiredMonthly),
        round2(pct)
    };
}

} // namespace finpay
