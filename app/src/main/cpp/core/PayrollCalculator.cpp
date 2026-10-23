#include "PayrollCalculator.hpp"
#include <algorithm>
#include <cmath>
#include <vector>

namespace finpay {

// Função auxiliar para arredondamento bancário em 2 casas decimais
static double round2(double value) {
    return std::round(value * 100.0) / 100.0;
}

struct Bracket {
    double limit;
    double rate;
    double deduction;
};

double PayrollCalculator::calculateProgressiveINSS(double grossSalary) {
    double gross = std::max(0.0, grossSalary);

    // Tabela progressiva oficial de INSS
    static const std::vector<Bracket> inssBrackets = {
        { 1621.33, 0.075, 0.0 },
        { 2902.84, 0.090, 24.32 },
        { 4354.27, 0.120, 111.41 },
        { 8475.55, 0.140, 198.49 }
    };

    double capped = std::min(gross, inssBrackets.back().limit);

    for (const auto& b : inssBrackets) {
        if (capped <= b.limit) {
            double discount = (capped * b.rate) - b.deduction;
            return round2(std::max(0.0, discount));
        }
    }

    // Se ultrapassar o teto
    double maxDiscount = (inssBrackets.back().limit * inssBrackets.back().rate) - inssBrackets.back().deduction;
    return round2(maxDiscount);
}

double PayrollCalculator::calculateProgressiveIRRF(
    double grossSalary,
    double inssDiscount,
    int dependents,
    double otherDeductions,
    bool* outUsedSimplified
) {
    double gross = std::max(0.0, grossSalary);
    if (gross <= 0.0) {
        if (outUsedSimplified) *outUsedSimplified = false;
        return 0.0;
    }

    // Dedução por dependente oficial (R$ 189,59)
    double dependentsDeduction = std::max(0, dependents) * 189.59;
    double legalDeductions = inssDiscount + dependentsDeduction + std::max(0.0, otherDeductions);

    // Opção 1: Base de cálculo com deduções legais
    double baseLegal = std::max(0.0, gross - legalDeductions);

    // Opção 2: Desconto simplificado padrão (R$ 607,44)
    double baseSimplified = std::max(0.0, gross - 607.44);

    // Tabela progressiva mensal de IRRF
    static const std::vector<Bracket> irrfBrackets = {
        { 2428.80, 0.000, 0.0 },
        { 3036.00, 0.075, 182.16 },
        { 4048.00, 0.150, 409.86 },
        { 5060.00, 0.225, 713.46 },
        { 1e12,    0.275, 966.46 }
    };

    auto calcTax = [](double base) -> double {
        for (const auto& b : irrfBrackets) {
            if (base <= b.limit) {
                double tax = (base * b.rate) - b.deduction;
                return round2(std::max(0.0, tax));
            }
        }
        return 0.0;
    };

    double taxLegal = calcTax(baseLegal);
    double taxSimplified = calcTax(baseSimplified);

    if (taxSimplified < taxLegal) {
        if (outUsedSimplified) *outUsedSimplified = true;
        return taxSimplified;
    } else {
        if (outUsedSimplified) *outUsedSimplified = false;
        return taxLegal;
    }
}

PayrollResult PayrollCalculator::calculate(
    double grossSalary,
    int dependents,
    double otherDeductions
) {
    double inss = calculateProgressiveINSS(grossSalary);
    bool usedSimplified = false;
    double irrf = calculateProgressiveIRRF(grossSalary, inss, dependents, otherDeductions, &usedSimplified);

    double totalDiscounts = inss + irrf + std::max(0.0, otherDeductions);
    double net = std::max(0.0, grossSalary - totalDiscounts);

    return PayrollResult {
        round2(grossSalary),
        inss,
        irrf,
        round2(net),
        usedSimplified
    };
}

} // namespace finpay
