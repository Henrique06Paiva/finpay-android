package dev.henrique.finpay

/**
 * Singleton de ponte (JNI Bridge) com o motor nativo em C++17 (libfinpay.so).
 * Centraliza todas as declarações `external` para manter as Activities e Views desacopladas.
 */
object FinPayNativeEngine {

    init {
        System.loadLibrary("finpay")
    }

    /**
     * Retorna informações de versão e compilador da biblioteca C++.
     */
    external fun getEngineInfoNative(): String

    /**
     * Calcula o holerite CLT com faixas progressivas de INSS e IRRF em C++.
     */
    external fun calculatePayrollNative(
        grossSalary: Double,
        dependents: Int,
        otherDeductions: Double
    ): PayrollResult

    /**
     * Calcula a data exata de pagamento no mês (Último dia útil ou 5º dia útil),
     * descontando fins de semana e feriados nacionais fixos e móveis (Meeus).
     */
    external fun getPaymentDateNative(
        year: Int,
        month: Int,
        payOnLastBusinessDay: Boolean
    ): String

    /**
     * Decompõe uma parcela de financiamento/empréstimo em Principal vs. Juros (Price ou SAC).
     */
    external fun calculateInstallmentBreakdownNative(
        currentBalance: Double,
        monthlyRatePercent: Double,
        remainingMonths: Int,
        isSAC: Boolean
    ): InstallmentBreakdownResult

    /**
     * Simula a quitação antecipada de múltiplos contratos comparando Avalanche vs. Bola de Neve.
     */
    external fun compareDebtStrategiesNative(
        balances: DoubleArray,
        rates: DoubleArray,
        minPayments: DoubleArray,
        extraMonthlyPayment: Double
    ): PayoffComparisonResult

    /**
     * Simula o rendimento líquido mensal e anual em Renda Fixa (Selic/CDB, LCI/LCA e Poupança).
     */
    external fun simulateFixedIncomeNative(
        investedAmount: Double,
        annualSelicPercent: Double,
        annualCdiPercent: Double
    ): FixedIncomeResult

    /**
     * Calcula o rendimento passivo mensal do saldo atual de uma meta e o aporte necessário.
     */
    external fun calculateGoalProjectionNative(
        targetAmount: Double,
        currentAmount: Double,
        remainingMonths: Int,
        annualCdiPercent: Double
    ): GoalProjectionResult
}
