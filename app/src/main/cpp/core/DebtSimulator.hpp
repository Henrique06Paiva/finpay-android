#ifndef FINPAY_DEBT_SIMULATOR_HPP
#define FINPAY_DEBT_SIMULATOR_HPP

#include <vector>

namespace finpay {

struct DebtContractInput {
    double balance;
    double monthlyRatePercent; // Ex: 1.85 para 1,85% a.m.
    double minimumPayment;
};

struct InstallmentBreakdown {
    double installmentAmount;
    double principalPortion;
    double interestPortion;
    double remainingBalance;
};

struct PayoffSimulationComparison {
    int baselineMonths;
    double baselineTotalInterest;

    int avalancheMonths;
    double avalancheTotalInterest;
    double avalancheInterestSaved;
    int avalancheMonthsSaved;

    int snowballMonths;
    double snowballTotalInterest;
    double snowballInterestSaved;
    int snowballMonthsSaved;
};

class DebtSimulator {
public:
    /**
     * Calcula a decomposição da parcela atual (Principal vs Juros) no sistema Price ou SAC.
     */
    static InstallmentBreakdown calculateInstallmentBreakdown(
        double currentBalance,
        double monthlyRatePercent,
        int remainingMonths,
        bool isSAC
    );

    /**
     * Simula a quitação de múltiplos contratos comparando o pagamento mínimo (Baseline),
     * Estratégia Avalanche (maior juro primeiro) e Bola de Neve (menor saldo primeiro).
     */
    static PayoffSimulationComparison compareStrategies(
        const std::vector<DebtContractInput>& debts,
        double extraMonthlyPayment
    );
};

} // namespace finpay

#endif // FINPAY_DEBT_SIMULATOR_HPP
