#ifndef FINPAY_PAYROLL_CALCULATOR_HPP
#define FINPAY_PAYROLL_CALCULATOR_HPP

namespace finpay {

/**
 * Estrutura de dados com o resultado detalhado do cálculo de folha.
 * Mantida em C++ puro sem qualquer dependência do Android ou JNI.
 */
struct PayrollResult {
    double grossSalary;
    double inssDiscount;
    double irrfDiscount;
    double netSalary;
    bool usedSimplifiedIrrf;
};

class PayrollCalculator {
public:
    /**
     * Calcula o INSS com a tabela progressiva oficial.
     */
    static double calculateProgressiveINSS(double grossSalary);

    /**
     * Calcula o IRRF comparando as deduções legais contra o desconto simplificado.
     */
    static double calculateProgressiveIRRF(
        double grossSalary,
        double inssDiscount,
        int dependents = 0,
        double otherDeductions = 0.0,
        bool* outUsedSimplified = nullptr
    );

    /**
     * Executa o cálculo completo do holerite.
     */
    static PayrollResult calculate(
        double grossSalary,
        int dependents = 0,
        double otherDeductions = 0.0
    );
};

} // namespace finpay

#endif // FINPAY_PAYROLL_CALCULATOR_HPP
