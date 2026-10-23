#include "DebtSimulator.hpp"
#include <algorithm>
#include <cmath>

namespace finpay {

static double round2(double v) {
    return std::round(v * 100.0) / 100.0;
}

InstallmentBreakdown DebtSimulator::calculateInstallmentBreakdown(
    double currentBalance,
    double monthlyRatePercent,
    int remainingMonths,
    bool isSAC
) {
    double bal = std::max(0.0, currentBalance);
    int n = std::max(1, remainingMonths);
    double r = std::max(0.0, monthlyRatePercent) / 100.0;

    if (bal <= 0.0) {
        return { 0.0, 0.0, 0.0, 0.0 };
    }

    double interest = bal * r;
    double principal = 0.0;
    double installment = 0.0;

    if (isSAC) {
        // Sistema SAC: Amortização constante
        principal = bal / n;
        installment = principal + interest;
    } else {
        // Tabela Price: Prestação constante = P * [r(1+r)^n] / [(1+r)^n - 1]
        if (r > 0.000001) {
            double factor = std::pow(1.0 + r, n);
            installment = bal * (r * factor) / (factor - 1.0);
            principal = std::max(0.0, installment - interest);
        } else {
            principal = bal / n;
            installment = principal;
        }
    }

    double nextBal = std::max(0.0, bal - principal);
    return {
        round2(installment),
        round2(principal),
        round2(interest),
        round2(nextBal)
    };
}

struct SimResult {
    int months;
    double totalInterest;
};

static SimResult runSimulation(
    std::vector<DebtContractInput> list,
    double extraPayment,
    bool prioritizeHighestRate
) {
    int months = 0;
    double totalInterest = 0.0;
    const int MAX_MONTHS = 360;

    while (months < MAX_MONTHS) {
        bool anyActive = false;
        for (const auto& d : list) {
            if (d.balance > 0.01) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive) break;

        ++months;
        double availableExtra = std::max(0.0, extraPayment);

        // 1. Aplica juros mensais e paga parcela mínima de cada contrato
        for (auto& d : list) {
            if (d.balance <= 0.01) continue;
            double interest = d.balance * (std::max(0.0, d.monthlyRatePercent) / 100.0);
            totalInterest += interest;
            d.balance += interest;

            double minPay = std::min(d.balance, std::max(interest + 10.0, d.minimumPayment));
            d.balance -= minPay;
        }

        // 2. Ordena os contratos ativos segundo a estratégia (Avalanche vs Bola de Neve)
        std::vector<size_t> order;
        for (size_t i = 0; i < list.size(); ++i) {
            if (list[i].balance > 0.01) order.push_back(i);
        }

        std::sort(order.begin(), order.end(), [&](size_t a, size_t b) {
            if (prioritizeHighestRate) {
                return list[a].monthlyRatePercent > list[b].monthlyRatePercent;
            } else {
                return list[a].balance < list[b].balance;
            }
        });

        // 3. Direciona o aporte extra para a dívida prioritária
        for (size_t idx : order) {
            if (availableExtra <= 0.01) break;
            double pay = std::min(list[idx].balance, availableExtra);
            list[idx].balance -= pay;
            availableExtra -= pay;
        }
    }

    return { months, round2(totalInterest) };
}

PayoffSimulationComparison DebtSimulator::compareStrategies(
    const std::vector<DebtContractInput>& debts,
    double extraMonthlyPayment
) {
    SimResult base = runSimulation(debts, 0.0, true);
    SimResult aval = runSimulation(debts, extraMonthlyPayment, true);
    SimResult snow = runSimulation(debts, extraMonthlyPayment, false);

    return PayoffSimulationComparison {
        base.months,
        base.totalInterest,
        aval.months,
        aval.totalInterest,
        round2(std::max(0.0, base.totalInterest - aval.totalInterest)),
        std::max(0, base.months - aval.months),
        snow.months,
        snow.totalInterest,
        round2(std::max(0.0, base.totalInterest - snow.totalInterest)),
        std::max(0, base.months - snow.months)
    };
}

} // namespace finpay
