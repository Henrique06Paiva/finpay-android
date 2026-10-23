#include <jni.h>
#include <string>
#include <vector>
#include "PayrollCalculator.hpp"
#include "DateEngine.hpp"
#include "DebtSimulator.hpp"
#include "YieldCalculator.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_getEngineInfoNative(
        JNIEnv* env,
        jobject /* this */) {
    std::string info = "Motor Nativo FinPay C++17 (Clang / Android NDK) · Ativo";
    return env->NewStringUTF(info.c_str());
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_calculatePayrollNative(
        JNIEnv* env,
        jobject /* this */,
        jdouble grossSalary,
        jint dependents,
        jdouble otherDeductions) {

    finpay::PayrollResult result = finpay::PayrollCalculator::calculate(
            static_cast<double>(grossSalary),
            static_cast<int>(dependents),
            static_cast<double>(otherDeductions)
    );

    jclass cls = env->FindClass("dev/henrique/finpay/PayrollResult");
    if (!cls) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(DDDDZ)V");
    if (!ctor) return nullptr;

    return env->NewObject(
            cls,
            ctor,
            static_cast<jdouble>(result.grossSalary),
            static_cast<jdouble>(result.inssDiscount),
            static_cast<jdouble>(result.irrfDiscount),
            static_cast<jdouble>(result.netSalary),
            static_cast<jboolean>(result.usedSimplifiedIrrf)
    );
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_getPaymentDateNative(
        JNIEnv* env,
        jobject /* this */,
        jint year,
        jint month,
        jboolean payOnLastBusinessDay) {

    std::string dateStr = payOnLastBusinessDay
            ? finpay::DateEngine::getLastBusinessDayFormatted(year, month)
            : finpay::DateEngine::getFifthBusinessDayFormatted(year, month);

    return env->NewStringUTF(dateStr.c_str());
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_calculateInstallmentBreakdownNative(
        JNIEnv* env,
        jobject /* this */,
        jdouble currentBalance,
        jdouble monthlyRatePercent,
        jint remainingMonths,
        jboolean isSAC) {

    finpay::InstallmentBreakdown res = finpay::DebtSimulator::calculateInstallmentBreakdown(
            static_cast<double>(currentBalance),
            static_cast<double>(monthlyRatePercent),
            static_cast<int>(remainingMonths),
            static_cast<bool>(isSAC)
    );

    jclass cls = env->FindClass("dev/henrique/finpay/InstallmentBreakdownResult");
    if (!cls) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(DDDD)V");
    if (!ctor) return nullptr;

    return env->NewObject(
            cls,
            ctor,
            static_cast<jdouble>(res.installmentAmount),
            static_cast<jdouble>(res.principalPortion),
            static_cast<jdouble>(res.interestPortion),
            static_cast<jdouble>(res.remainingBalance)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_compareDebtStrategiesNative(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray balances,
        jdoubleArray rates,
        jdoubleArray minPayments,
        jdouble extraMonthlyPayment) {

    jsize len = env->GetArrayLength(balances);
    jdouble* balPtr = env->GetDoubleArrayElements(balances, nullptr);
    jdouble* ratePtr = env->GetDoubleArrayElements(rates, nullptr);
    jdouble* minPtr = env->GetDoubleArrayElements(minPayments, nullptr);

    std::vector<finpay::DebtContractInput> contracts;
    contracts.reserve(len);
    for (jsize i = 0; i < len; ++i) {
        contracts.push_back({
            static_cast<double>(balPtr[i]),
            static_cast<double>(ratePtr[i]),
            static_cast<double>(minPtr[i])
        });
    }

    // Libera os ponteiros de array sem copiar de volta (JNI_ABORT = somente leitura)
    env->ReleaseDoubleArrayElements(balances, balPtr, JNI_ABORT);
    env->ReleaseDoubleArrayElements(rates, ratePtr, JNI_ABORT);
    env->ReleaseDoubleArrayElements(minPayments, minPtr, JNI_ABORT);

    finpay::PayoffSimulationComparison cmp = finpay::DebtSimulator::compareStrategies(
            contracts,
            static_cast<double>(extraMonthlyPayment)
    );

    jclass cls = env->FindClass("dev/henrique/finpay/PayoffComparisonResult");
    if (!cls) return nullptr;
    // Assinatura: (I D I D D I I D D I)V
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(IDIDDIIDDI)V");
    if (!ctor) return nullptr;

    return env->NewObject(
            cls,
            ctor,
            static_cast<jint>(cmp.baselineMonths),
            static_cast<jdouble>(cmp.baselineTotalInterest),
            static_cast<jint>(cmp.avalancheMonths),
            static_cast<jdouble>(cmp.avalancheTotalInterest),
            static_cast<jdouble>(cmp.avalancheInterestSaved),
            static_cast<jint>(cmp.avalancheMonthsSaved),
            static_cast<jint>(cmp.snowballMonths),
            static_cast<jdouble>(cmp.snowballTotalInterest),
            static_cast<jdouble>(cmp.snowballInterestSaved),
            static_cast<jint>(cmp.snowballMonthsSaved)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_simulateFixedIncomeNative(
        JNIEnv* env,
        jobject /* this */,
        jdouble investedAmount,
        jdouble annualSelicPercent,
        jdouble annualCdiPercent) {

    finpay::FixedIncomeSimulation sim = finpay::YieldCalculator::simulateFixedIncome(
            static_cast<double>(investedAmount),
            static_cast<double>(annualSelicPercent),
            static_cast<double>(annualCdiPercent)
    );

    jclass cls = env->FindClass("dev/henrique/finpay/FixedIncomeResult");
    if (!cls) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(DDDDDDD)V");
    if (!ctor) return nullptr;

    return env->NewObject(
            cls,
            ctor,
            static_cast<jdouble>(sim.principal),
            static_cast<jdouble>(sim.selicMonthlyNet),
            static_cast<jdouble>(sim.selicAnnualNet),
            static_cast<jdouble>(sim.lciMonthlyNet),
            static_cast<jdouble>(sim.lciAnnualNet),
            static_cast<jdouble>(sim.poupancaMonthlyNet),
            static_cast<jdouble>(sim.poupancaAnnualNet)
    );
}

extern "C" JNIEXPORT jobject JNICALL
Java_dev_henrique_finpay_FinPayNativeEngine_calculateGoalProjectionNative(
        JNIEnv* env,
        jobject /* this */,
        jdouble targetAmount,
        jdouble currentAmount,
        jint remainingMonths,
        jdouble annualCdiPercent) {

    finpay::GoalProjection proj = finpay::YieldCalculator::calculateGoalProjection(
            static_cast<double>(targetAmount),
            static_cast<double>(currentAmount),
            static_cast<int>(remainingMonths),
            static_cast<double>(annualCdiPercent)
    );

    jclass cls = env->FindClass("dev/henrique/finpay/GoalProjectionResult");
    if (!cls) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(DDD)V");
    if (!ctor) return nullptr;

    return env->NewObject(
            cls,
            ctor,
            static_cast<jdouble>(proj.passiveMonthlyYield),
            static_cast<jdouble>(proj.requiredMonthlyContribution),
            static_cast<jdouble>(proj.progressPercent)
    );
}
