package dev.henrique.finpay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repositório Local-First do FinPay no Android.
 * Gerencia o estado das Contas, Transações, Holerite CLT, Dívidas e Metas
 * persistindo localmente via SharedPreferences em formato JSON estruturado.
 */
class FinPayRepository(context: Context) {

    private val prefs = context.getSharedPreferences("finpay_local_vault_v1", Context.MODE_PRIVATE)

    var privacyMode: Boolean
        get() = prefs.getBoolean("privacy_mode", false)
        set(value) = prefs.edit().putBoolean("privacy_mode", value).apply()

    fun loadAccounts(): MutableList<BankAccount> {
        val raw = prefs.getString("accounts_json", null)
        if (raw.isNullOrBlank()) {
            return mutableListOf(
                BankAccount("acc_1", "Nubank Corrente", "Conta corrente principal", 3060.00),
                BankAccount("acc_2", "Reserva BTG Selic", "Investimentos · Liquidez diária", 14552.80)
            )
        }
        val arr = JSONArray(raw)
        val list = mutableListOf<BankAccount>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                BankAccount(
                    o.getString("id"),
                    o.getString("name"),
                    o.getString("subtitle"),
                    o.getDouble("balance")
                )
            )
        }
        return list
    }

    fun saveAccounts(accounts: List<BankAccount>) {
        val arr = JSONArray()
        for (a in accounts) {
            val o = JSONObject()
            o.put("id", a.id)
            o.put("name", a.name)
            o.put("subtitle", a.subtitle)
            o.put("balance", a.balance)
            arr.put(o)
        }
        prefs.edit().putString("accounts_json", arr.toString()).apply()
    }

    fun loadPayrollConfig(): PayrollConfig {
        val raw = prefs.getString("payroll_json", null) ?: return PayrollConfig()
        val o = JSONObject(raw)
        return PayrollConfig(
            employerName = o.optString("employerName", "NovaEra Tecnologia S.A. (Demo)"),
            roleName = o.optString("roleName", "Engenheiro de Software"),
            grossSalary = o.optDouble("grossSalary", 5800.0),
            dependents = o.optInt("dependents", 0),
            fixedOtherDiscounts = o.optDouble("fixedOtherDiscounts", 110.0),
            monthExtraAdjustment = o.optDouble("monthExtraAdjustment", 0.0),
            payOnLastBusinessDay = o.optBoolean("payOnLastBusinessDay", true)
        )
    }

    fun savePayrollConfig(cfg: PayrollConfig) {
        val o = JSONObject()
        o.put("employerName", cfg.employerName)
        o.put("roleName", cfg.roleName)
        o.put("grossSalary", cfg.grossSalary)
        o.put("dependents", cfg.dependents)
        o.put("fixedOtherDiscounts", cfg.fixedOtherDiscounts)
        o.put("monthExtraAdjustment", cfg.monthExtraAdjustment)
        o.put("payOnLastBusinessDay", cfg.payOnLastBusinessDay)
        prefs.edit().putString("payroll_json", o.toString()).apply()
    }

    fun loadTransactions(): MutableList<TransactionItem> {
        val raw = prefs.getString("transactions_json", null)
        if (raw.isNullOrBlank()) {
            return mutableListOf(
                TransactionItem(UUID.randomUUID().toString(), "Supermercado Pão de Açúcar", "Alimentação", "22/10/2026", 540.00, isIncome = false, isFixed = false),
                TransactionItem(UUID.randomUUID().toString(), "Condomínio & Internet Fibra", "Moradia", "15/10/2026", 890.00, isIncome = false, isFixed = true),
                TransactionItem(UUID.randomUUID().toString(), "Rendimento Dividendos / FIIs", "Investimentos", "14/10/2026", 433.00, isIncome = true, isFixed = false),
                TransactionItem(UUID.randomUUID().toString(), "Consultoria Técnica Freelance", "Renda Extra", "10/10/2026", 1200.00, isIncome = true, isFixed = false),
                TransactionItem(UUID.randomUUID().toString(), "Combustível & Mobilidade", "Transporte", "08/10/2026", 330.00, isIncome = false, isFixed = false),
                TransactionItem(UUID.randomUUID().toString(), "Assinaturas & Streaming", "Lazer", "05/10/2026", 129.90, isIncome = false, isFixed = true)
            )
        }
        val arr = JSONArray(raw)
        val list = mutableListOf<TransactionItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                TransactionItem(
                    o.getString("id"),
                    o.getString("title"),
                    o.getString("category"),
                    o.getString("date"),
                    o.getDouble("amount"),
                    o.getBoolean("isIncome"),
                    o.getBoolean("isFixed")
                )
            )
        }
        return list
    }

    fun saveTransactions(list: List<TransactionItem>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("category", t.category)
            o.put("date", t.date)
            o.put("amount", t.amount)
            o.put("isIncome", t.isIncome)
            o.put("isFixed", t.isFixed)
            arr.put(o)
        }
        prefs.edit().putString("transactions_json", arr.toString()).apply()
    }

    fun loadDebts(): MutableList<DebtContract> {
        val raw = prefs.getString("debts_json", null)
        if (raw.isNullOrBlank()) {
            return mutableListOf(
                DebtContract(
                    id = "debt_1",
                    title = "Financiamento Notebook Pro",
                    institution = "Nubank · Tabela Price",
                    balance = 4200.00,
                    monthlyRatePercent = 1.95,
                    remainingMonths = 12,
                    isSAC = false
                ),
                DebtContract(
                    id = "debt_2",
                    title = "Crédito Pessoal Consignado",
                    institution = "Itaú · Sistema SAC",
                    balance = 8500.00,
                    monthlyRatePercent = 1.35,
                    remainingMonths = 24,
                    isSAC = true
                )
            )
        }
        val arr = JSONArray(raw)
        val list = mutableListOf<DebtContract>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                DebtContract(
                    o.getString("id"),
                    o.getString("title"),
                    o.getString("institution"),
                    o.getDouble("balance"),
                    o.getDouble("monthlyRatePercent"),
                    o.getInt("remainingMonths"),
                    o.getBoolean("isSAC")
                )
            )
        }
        return list
    }

    fun saveDebts(list: List<DebtContract>) {
        val arr = JSONArray()
        for (d in list) {
            val o = JSONObject()
            o.put("id", d.id)
            o.put("title", d.title)
            o.put("institution", d.institution)
            o.put("balance", d.balance)
            o.put("monthlyRatePercent", d.monthlyRatePercent)
            o.put("remainingMonths", d.remainingMonths)
            o.put("isSAC", d.isSAC)
            arr.put(o)
        }
        prefs.edit().putString("debts_json", arr.toString()).apply()
    }

    fun loadGoals(): MutableList<FinancialGoal> {
        val raw = prefs.getString("goals_json", null)
        if (raw.isNullOrBlank()) {
            return mutableListOf(
                FinancialGoal(
                    id = "goal_1",
                    title = "Reserva de Emergência (6 meses)",
                    subtitle = "Alocado em Tesouro Selic / 100% CDI",
                    targetAmount = 20000.00,
                    currentAmount = 14552.80,
                    remainingMonths = 10
                ),
                FinancialGoal(
                    id = "goal_2",
                    title = "Upgrade Setup & Viagem",
                    subtitle = "Alocado em LCI Isenta de IR",
                    targetAmount = 8000.00,
                    currentAmount = 3200.00,
                    remainingMonths = 8
                )
            )
        }
        val arr = JSONArray(raw)
        val list = mutableListOf<FinancialGoal>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                FinancialGoal(
                    o.getString("id"),
                    o.getString("title"),
                    o.getString("subtitle"),
                    o.getDouble("targetAmount"),
                    o.getDouble("currentAmount"),
                    o.getInt("remainingMonths")
                )
            )
        }
        return list
    }

    fun saveGoals(list: List<FinancialGoal>) {
        val arr = JSONArray()
        for (g in list) {
            val o = JSONObject()
            o.put("id", g.id)
            o.put("title", g.title)
            o.put("subtitle", g.subtitle)
            o.put("targetAmount", g.targetAmount)
            o.put("currentAmount", g.currentAmount)
            o.put("remainingMonths", g.remainingMonths)
            arr.put(o)
        }
        prefs.edit().putString("goals_json", arr.toString()).apply()
    }

    fun loadMarketRates(): MarketRates {
        val raw = prefs.getString("market_rates_json", null) ?: return MarketRates()
        val o = JSONObject(raw)
        return MarketRates(
            selic = o.optDouble("selic", 13.75),
            cdi = o.optDouble("cdi", 13.65),
            ipca = o.optDouble("ipca", 4.58),
            usdBid = o.optDouble("usdBid", 5.04),
            usdPctChange = o.optDouble("usdPctChange", 0.38),
            lastUpdated = o.optString("lastUpdated", "Valores de referência")
        )
    }

    fun saveMarketRates(rates: MarketRates) {
        val o = JSONObject().apply {
            put("selic", rates.selic)
            put("cdi", rates.cdi)
            put("ipca", rates.ipca)
            put("usdBid", rates.usdBid)
            put("usdPctChange", rates.usdPctChange)
            put("lastUpdated", rates.lastUpdated)
        }
        prefs.edit().putString("market_rates_json", o.toString()).apply()
    }
}
