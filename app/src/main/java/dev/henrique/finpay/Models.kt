package dev.henrique.finpay

/**
 * Estruturas retornadas pela camada C++ via JNI (libfinpay.so)
 */
data class InstallmentBreakdownResult(
    val installmentAmount: Double,
    val principalPortion: Double,
    val interestPortion: Double,
    val remainingBalance: Double
)

data class PayoffComparisonResult(
    val baselineMonths: Int,
    val baselineTotalInterest: Double,
    val avalancheMonths: Int,
    val avalancheTotalInterest: Double,
    val avalancheInterestSaved: Double,
    val avalancheMonthsSaved: Int,
    val snowballMonths: Int,
    val snowballTotalInterest: Double,
    val snowballInterestSaved: Double,
    val snowballMonthsSaved: Int
)

data class FixedIncomeResult(
    val principal: Double,
    val selicMonthlyNet: Double,
    val selicAnnualNet: Double,
    val lciMonthlyNet: Double,
    val lciAnnualNet: Double,
    val poupancaMonthlyNet: Double,
    val poupancaAnnualNet: Double
)

data class GoalProjectionResult(
    val passiveMonthlyYield: Double,
    val requiredMonthlyContribution: Double,
    val progressPercent: Double
)

/**
 * Modelos de domínio da aplicação FinPay
 */
data class BankAccount(
    val id: String,
    val name: String,
    val subtitle: String,
    var balance: Double
)

data class TransactionItem(
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val amount: Double,
    val isIncome: Boolean,
    val isFixed: Boolean
)

data class BudgetCategory(
    val id: String,
    val name: String,
    val monthlyLimit: Double,
    val spent: Double
)

data class DebtContract(
    val id: String,
    val title: String,
    val institution: String,
    var balance: Double,
    val monthlyRatePercent: Double,
    val remainingMonths: Int,
    val isSAC: Boolean
)

data class FinancialGoal(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetAmount: Double,
    var currentAmount: Double,
    val remainingMonths: Int
)

data class PayrollConfig(
    var employerName: String = "NovaEra Tecnologia S.A. (Demo)",
    var roleName: String = "Engenheiro de Software",
    var grossSalary: Double = 5800.0,
    var dependents: Int = 0,
    var fixedOtherDiscounts: Double = 110.0,
    var monthExtraAdjustment: Double = 0.0,
    var payOnLastBusinessDay: Boolean = true
)
