package dev.henrique.finpay

/**
 * Representa o resultado do cálculo de folha gerado pelo motor C++ (libfinpay.so).
 * Os campos e a ordem do construtor correspondem exatamente à assinatura JNI (DDDDZ)V.
 */
data class PayrollResult(
    val grossSalary: Double,
    val inssDiscount: Double,
    val irrfDiscount: Double,
    val netSalary: Double,
    val usedSimplifiedIrrf: Boolean
)
