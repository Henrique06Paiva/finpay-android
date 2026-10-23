#ifndef FINPAY_YIELD_CALCULATOR_HPP
#define FINPAY_YIELD_CALCULATOR_HPP

namespace finpay {

struct FixedIncomeSimulation {
    double principal;
    double selicMonthlyNet;
    double selicAnnualNet;
    double lciMonthlyNet;
    double lciAnnualNet;
    double poupancaMonthlyNet;
    double poupancaAnnualNet;
};

struct GoalProjection {
    double passiveMonthlyYield;
    double requiredMonthlyContribution;
    double progressPercent;
};

class YieldCalculator {
public:
    /**
     * Simula o retorno líquido mensal e anual para Tesouro Selic/CDB 100% CDI (com desconto de IR),
     * LCI/LCA Isenta (ou CDB 110% CDI) e Poupança.
     */
    static FixedIncomeSimulation simulateFixedIncome(
        double investedAmount,
        double annualSelicPercent,
        double annualCdiPercent
    );

    /**
     * Calcula quanto o saldo atual de uma meta já rende sozinho por mês e o aporte necessário.
     */
    static GoalProjection calculateGoalProjection(
        double targetAmount,
        double currentAmount,
        int remainingMonths,
        double annualCdiPercent
    );
};

} // namespace finpay

#endif // FINPAY_YIELD_CALCULATOR_HPP
