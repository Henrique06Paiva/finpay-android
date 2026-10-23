package dev.henrique.finpay

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dev.henrique.finpay.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: FinPayRepository
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    private lateinit var accounts: MutableList<BankAccount>
    private lateinit var payrollConfig: PayrollConfig
    private lateinit var transactions: MutableList<TransactionItem>
    private lateinit var debts: MutableList<DebtContract>
    private lateinit var goals: MutableList<FinancialGoal>
    private lateinit var marketRates: MarketRates

    private var currentTab = 0 // 0=Home, 1=Transactions, 2=Budget, 3=Debts, 4=Goals
    private var txFilter = "ALL" // ALL, INCOME, EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FinPayRepository(this)
        accounts = repository.loadAccounts()
        payrollConfig = repository.loadPayrollConfig()
        transactions = repository.loadTransactions()
        debts = repository.loadDebts()
        goals = repository.loadGoals()
        marketRates = repository.loadMarketRates()

        binding.tvNativeStatusFooter.text = FinPayNativeEngine.getEngineInfoNative()

        setupNavigation()
        setupHeaderActions()
        setupHomeInteractions()
        setupTransactionsInteractions()
        setupDebtsInteractions()
        setupGoalsInteractions()

        refreshAllUI()
    }

    private fun formatMoney(value: Double): String {
        if (repository.privacyMode) {
            return "R$ •••••"
        }
        return currencyFormat.format(value)
    }

    private fun setupNavigation() {
        val navButtons = listOf(
            binding.navBtnHome,
            binding.navBtnTransactions,
            binding.navBtnBudget,
            binding.navBtnDebts,
            binding.navBtnGoals
        )

        fun selectTab(index: Int) {
            currentTab = index
            binding.viewTabHome.visibility = if (index == 0) View.VISIBLE else View.GONE
            binding.viewTabTransactions.visibility = if (index == 1) View.VISIBLE else View.GONE
            binding.viewTabBudget.visibility = if (index == 2) View.VISIBLE else View.GONE
            binding.viewTabDebts.visibility = if (index == 3) View.VISIBLE else View.GONE
            binding.viewTabGoals.visibility = if (index == 4) View.VISIBLE else View.GONE

            val titles = listOf(
                "Visão geral",
                "Extrato de lançamentos",
                "Orçamento mensal",
                "Gestão de dívidas (C++)",
                "Metas & Mercado (C++)"
            )
            binding.tvHeaderTitle.text = titles[index]

            navButtons.forEachIndexed { idx, btn ->
                if (idx == index) {
                    btn.setBackgroundColor(ContextCompat.getColor(this, R.color.text_primary))
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
            binding.mainScrollView.smoothScrollTo(0, 0)
        }

        navButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { selectTab(index) }
        }
        selectTab(0)
    }

    private fun setupHeaderActions() {
        binding.btnPrivacyToggle.setOnClickListener {
            repository.privacyMode = !repository.privacyMode
            binding.btnPrivacyToggle.text = if (repository.privacyMode) "Mostrar" else "Ocultar"
            refreshAllUI()
        }
        binding.btnPrivacyToggle.text = if (repository.privacyMode) "Mostrar" else "Ocultar"

        binding.btnAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun setupHomeInteractions() {
        binding.btnOpenPayrollModal.setOnClickListener {
            showPayrollModalDialog()
        }

        binding.cardAccount1.setOnClickListener {
            if (accounts.isNotEmpty()) showEditAccountDialog(accounts[0])
        }
        binding.cardAccount2.setOnClickListener {
            if (accounts.size > 1) showEditAccountDialog(accounts[1])
        }
    }

    private fun setupTransactionsInteractions() {
        binding.btnFilterAll.setOnClickListener {
            txFilter = "ALL"
            renderTransactionsList()
        }
        binding.btnFilterIncome.setOnClickListener {
            txFilter = "INCOME"
            renderTransactionsList()
        }
        binding.btnFilterExpense.setOnClickListener {
            txFilter = "EXPENSE"
            renderTransactionsList()
        }
    }

    private fun setupDebtsInteractions() {
        binding.btnSimulateDebts.setOnClickListener {
            renderDebtsAndSimulation()
        }
    }

    private fun setupGoalsInteractions() {
        binding.btnSimulateYield.setOnClickListener {
            renderGoalsAndMarket()
        }

        binding.btnRefreshMarketRates.setOnClickListener {
            binding.btnRefreshMarketRates.isEnabled = false
            binding.btnRefreshMarketRates.text = "Buscando..."

            lifecycleScope.launch {
                val result = MarketApiService.fetchLiveMarketRates(marketRates)
                result.onSuccess { newRates ->
                    marketRates = newRates
                    repository.saveMarketRates(marketRates)
                    Toast.makeText(
                        this@MainActivity,
                        "Cotações atualizadas com sucesso via BrasilAPI & AwesomeAPI!",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    Toast.makeText(
                        this@MainActivity,
                        "Sem conexão com internet: mantendo cotações salvas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                binding.btnRefreshMarketRates.isEnabled = true
                binding.btnRefreshMarketRates.text = "⟳ Atualizar"
                renderGoalsAndMarket()
            }
        }
    }

    private fun refreshAllUI() {
        renderHomeTab()
        renderTransactionsList()
        renderBudgetTab()
        renderDebtsAndSimulation()
        renderGoalsAndMarket()
    }

    // =========================================================================
    // ABA 1: RENDERIZAÇÃO DA VISÃO GERAL + FOLHA CLT (C++ ENGINE)
    // =========================================================================
    private fun renderHomeTab() {
        val totalBalance = accounts.sumOf { it.balance }
        binding.tvTotalBalance.text = formatMoney(totalBalance)

        if (accounts.isNotEmpty()) {
            binding.tvAcc1Name.text = "● ${accounts[0].name}"
            binding.tvAcc1Balance.text = formatMoney(accounts[0].balance)
        }
        if (accounts.size > 1) {
            binding.tvAcc2Name.text = "● ${accounts[1].name}"
            binding.tvAcc2Balance.text = formatMoney(accounts[1].balance)
        }

        val monthIncomes = transactions.filter { it.isIncome }.sumOf { it.amount }
        val monthExpenses = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val monthNet = monthIncomes - monthExpenses

        binding.tvMonthIncome.text = formatMoney(monthIncomes)
        binding.tvMonthExpense.text = formatMoney(monthExpenses)
        binding.tvMonthNet.text = (if (monthNet >= 0) "+" else "") + formatMoney(monthNet)
        binding.tvMonthNet.setTextColor(
            ContextCompat.getColor(
                this,
                if (monthNet >= 0) R.color.success_green else R.color.danger_red
            )
        )

        // Cálculo do Salário Líquido e Data Real de Pagamento via C++ (PayrollCalculator + DateEngine)
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        val payrollRes = FinPayNativeEngine.calculatePayrollNative(
            payrollConfig.grossSalary,
            payrollConfig.dependents,
            payrollConfig.fixedOtherDiscounts
        )
        val payDate = FinPayNativeEngine.getPaymentDateNative(
            year,
            month,
            payrollConfig.payOnLastBusinessDay
        )

        val finalNetSalary = (payrollRes.netSalary + payrollConfig.monthExtraAdjustment).coerceAtLeast(0.0)
        val totalDiscounts = payrollRes.inssDiscount + payrollRes.irrfDiscount + payrollConfig.fixedOtherDiscounts

        binding.tvHomePayrollNet.text = "Líquido: ${formatMoney(finalNetSalary)}"
        binding.tvHomePayrollSummary.text =
            "Bruto ${formatMoney(payrollConfig.grossSalary)} · Desc. -${formatMoney(totalDiscounts)} · Crédito $payDate"

        val fixedExpenses = transactions.filter { !it.isIncome && it.isFixed }.sumOf { it.amount }
        binding.tvFixedBills.text = formatMoney(fixedExpenses)
        binding.tvExpectedReceivables.text = formatMoney(finalNetSalary)
        binding.tvReceivablesDate.text = if (payrollConfig.payOnLastBusinessDay) {
            "Crédito $payDate (Último dia útil)"
        } else {
            "Crédito $payDate (5º dia útil)"
        }

        val foodSpent = transactions.filter { !it.isIncome && it.category.equals("Alimentação", true) }
            .sumOf { it.amount }
        val foodPct = ((foodSpent / 900.0) * 100.0).toInt().coerceAtMost(100)
        binding.tvHomeBudgetAlert.text =
            "Alimentação: $foodPct% consumido (${formatMoney(foodSpent)} / ${formatMoney(900.0)})"
    }

    // =========================================================================
    // ABA 2: RENDERIZAÇÃO DO EXTRATO DE TRANSAÇÕES
    // =========================================================================
    private fun renderTransactionsList() {
        binding.llTransactionsList.removeAllViews()

        val filtered = transactions.filter {
            when (txFilter) {
                "INCOME" -> it.isIncome
                "EXPENSE" -> !it.isIncome
                else -> true
            }
        }

        for (tx in filtered) {
            val card = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(this@MainActivity, R.color.card_border)
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(this).apply {
                text = tx.title
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            }

            val tagFixed = if (tx.isFixed) " · Fixo" else ""
            val tvSub = TextView(this).apply {
                text = "${tx.category} · ${tx.date}$tagFixed"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
            }

            leftCol.addView(tvTitle)
            leftCol.addView(tvSub)

            val tvAmount = TextView(this).apply {
                val sign = if (tx.isIncome) "+" else "-"
                text = "$sign ${formatMoney(tx.amount)}"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (tx.isIncome) R.color.success_green else R.color.text_primary
                    )
                )
            }

            row.addView(leftCol)
            row.addView(tvAmount)
            card.addView(row)

            // Toque longo ou clique para excluir lançamento
            card.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle(tx.title)
                    .setMessage("Deseja remover este lançamento de ${formatMoney(tx.amount)}?")
                    .setPositiveButton("Excluir") { _, _ ->
                        transactions.removeAll { it.id == tx.id }
                        repository.saveTransactions(transactions)
                        refreshAllUI()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            binding.llTransactionsList.addView(card)
        }
    }

    // =========================================================================
    // ABA 3: RENDERIZAÇÃO DO ORÇAMENTO MENSAL
    // =========================================================================
    private fun renderBudgetTab() {
        binding.llBudgetCategoriesList.removeAllViews()

        val limits = listOf(
            "Moradia" to 1200.0,
            "Alimentação" to 900.0,
            "Transporte" to 500.0,
            "Lazer" to 400.0
        )

        val totalLimit = limits.sumOf { it.second }
        val totalSpent = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val totalPct = ((totalSpent / totalLimit) * 100.0).toInt().coerceIn(0, 100)

        binding.tvBudgetOverview.text = "${formatMoney(totalSpent)} de ${formatMoney(totalLimit)}"
        binding.pbTotalBudget.progress = totalPct

        for ((catName, limitVal) in limits) {
            val spentVal = transactions
                .filter { !it.isIncome && it.category.equals(catName, ignoreCase = true) }
                .sumOf { it.amount }
            val pct = ((spentVal / limitVal) * 100.0).toInt().coerceIn(0, 100)

            val card = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(this@MainActivity, R.color.card_border)
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val tvCat = TextView(this).apply {
                text = catName
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvRatio = TextView(this).apply {
                text = "${formatMoney(spentVal)} / ${formatMoney(limitVal)} ($pct%)"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            }

            headerRow.addView(tvCat)
            headerRow.addView(tvRatio)

            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = pct
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(8)
                ).apply {
                    topMargin = dp(8)
                }
            }

            box.addView(headerRow)
            box.addView(pb)
            card.addView(box)
            binding.llBudgetCategoriesList.addView(card)
        }
    }

    // =========================================================================
    // ABA 4: DÍVIDAS & SIMULADOR AVALANCHE VS. BOLA DE NEVE (C++ ENGINE)
    // =========================================================================
    private fun renderDebtsAndSimulation() {
        binding.llDebtsList.removeAllViews()

        val extraPayment = binding.etExtraDebtPayment.text.toString().toDoubleOrNull() ?: 400.0

        val balances = DoubleArray(debts.size)
        val rates = DoubleArray(debts.size)
        val minPayments = DoubleArray(debts.size)

        debts.forEachIndexed { idx, contract ->
            // Chama o motor C++ (DebtSimulator.cpp) para decompor a parcela (Price ou SAC)
            val breakdown = FinPayNativeEngine.calculateInstallmentBreakdownNative(
                contract.balance,
                contract.monthlyRatePercent,
                contract.remainingMonths,
                contract.isSAC
            )

            balances[idx] = contract.balance
            rates[idx] = contract.monthlyRatePercent
            minPayments[idx] = breakdown.installmentAmount

            val card = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(this@MainActivity, R.color.card_border)
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
            }

            val tvTitle = TextView(this).apply {
                text = contract.title
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            }

            val tvSub = TextView(this).apply {
                text = "${contract.institution} · Taxa ${contract.monthlyRatePercent}% a.m. · ${contract.remainingMonths}x restantes"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
            }

            val tvBalance = TextView(this).apply {
                text = "Saldo Devedor: ${formatMoney(contract.balance)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.danger_red))
                setPadding(0, dp(6), 0, dp(4))
            }

            val tvBreakdown = TextView(this).apply {
                text = "Parcela C++: ${formatMoney(breakdown.installmentAmount)} " +
                    "(Amortiza ${formatMoney(breakdown.principalPortion)} · Juros ${formatMoney(breakdown.interestPortion)})"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            }

            val btnAmortize = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "Abater Parcela no Saldo"
                textSize = 11f
                isAllCaps = false
                setOnClickListener {
                    contract.balance = breakdown.remainingBalance
                    repository.saveDebts(debts)
                    Toast.makeText(
                        this@MainActivity,
                        "Parcela abatida! Novo saldo: ${formatMoney(contract.balance)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshAllUI()
                }
            }

            box.addView(tvTitle)
            box.addView(tvSub)
            box.addView(tvBalance)
            box.addView(tvBreakdown)
            box.addView(btnAmortize)
            card.addView(box)
            binding.llDebtsList.addView(card)
        }

        if (debts.isNotEmpty()) {
            val sim = FinPayNativeEngine.compareDebtStrategiesNative(
                balances,
                rates,
                minPayments,
                extraPayment
            )
            binding.tvAvalancheResult.text = "Poupa ${formatMoney(sim.avalancheInterestSaved)} em juros"
            binding.tvAvalancheMonths.text =
                "Quita em ${sim.avalancheMonths} meses (-${sim.avalancheMonthsSaved}m)"

            binding.tvSnowballResult.text = "Poupa ${formatMoney(sim.snowballInterestSaved)} em juros"
            binding.tvSnowballMonths.text =
                "Quita em ${sim.snowballMonths} meses (-${sim.snowballMonthsSaved}m)"
        }
    }

    // =========================================================================
    // ABA 5: METAS & SIMULADOR DE RENDA FIXA (C++ ENGINE)
    // =========================================================================
    private fun renderGoalsAndMarket() {
        // Atualiza os cards das cotações públicas ao vivo
        binding.tvMarketLastUpdated.text = marketRates.lastUpdated
        binding.tvSelicRate.text = "${marketRates.selic}%"
        binding.tvCdiRate.text = "${marketRates.cdi}%"
        binding.tvIpcaRate.text = "${marketRates.ipca}%"
        binding.tvUsdRate.text = "R$ ${String.format(Locale.US, "%.2f", marketRates.usdBid)}"
        binding.tvUsdChange.text = "${if (marketRates.usdPctChange >= 0) "+" else ""}${String.format(Locale.US, "%.2f", marketRates.usdPctChange)}% hoje"
        binding.tvUsdChange.setTextColor(
            ContextCompat.getColor(
                this,
                if (marketRates.usdPctChange >= 0) R.color.success_green else R.color.danger_red
            )
        )

        val invested = binding.etInvestedAmount.text.toString().toDoubleOrNull() ?: 10000.0

        // Executa a simulação em C++ alimentando com as taxas ao vivo da BrasilAPI
        val sim = FinPayNativeEngine.simulateFixedIncomeNative(
            invested,
            marketRates.selic,
            marketRates.cdi
        )

        binding.tvSimSelic.text =
            "Tesouro Selic / CDB 100% CDI: +${formatMoney(sim.selicMonthlyNet)}/mês (+${formatMoney(sim.selicAnnualNet)}/ano)"
        binding.tvSimLci.text =
            "LCI / LCA Isenta de IR (92% CDI): +${formatMoney(sim.lciMonthlyNet)}/mês (+${formatMoney(sim.lciAnnualNet)}/ano)"
        binding.tvSimPoupanca.text =
            "Poupança Tradicional: +${formatMoney(sim.poupancaMonthlyNet)}/mês (+${formatMoney(sim.poupancaAnnualNet)}/ano)"

        binding.llGoalsList.removeAllViews()
        for (goal in goals) {
            val proj = FinPayNativeEngine.calculateGoalProjectionNative(
                goal.targetAmount,
                goal.currentAmount,
                goal.remainingMonths,
                marketRates.cdi
            )

            val card = MaterialCardView(this).apply {
                radius = dp(20).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(this@MainActivity, R.color.card_border)
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
            }

            val tvTitle = TextView(this).apply {
                text = goal.title
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            }

            val tvSub = TextView(this).apply {
                text = "${goal.subtitle} · ${proj.progressPercent.toInt()}% concluído"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
            }

            val tvAmounts = TextView(this).apply {
                text = "${formatMoney(goal.currentAmount)} de ${formatMoney(goal.targetAmount)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.brand_green_primary))
                setPadding(0, dp(6), 0, dp(4))
            }

            val tvYieldInfo = TextView(this).apply {
                text = "Rendimento passivo C++: +${formatMoney(proj.passiveMonthlyYield)}/mês · " +
                    "Aporte sugerido: ${formatMoney(proj.requiredMonthlyContribution)}/mês"
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success_green))
            }

            val btnAddGoalMoney = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "+ Guardar R$ 500,00 nesta meta"
                textSize = 11f
                isAllCaps = false
                setOnClickListener {
                    goal.currentAmount += 500.0
                    repository.saveGoals(goals)
                    refreshAllUI()
                }
            }

            box.addView(tvTitle)
            box.addView(tvSub)
            box.addView(tvAmounts)
            box.addView(tvYieldInfo)
            box.addView(btnAddGoalMoney)
            card.addView(box)
            binding.llGoalsList.addView(card)
        }
    }

    // =========================================================================
    // MODAIS INTERATIVOS (HOLERITE CLT C++, NOVO LANÇAMENTO E SALDO DE CONTA)
    // =========================================================================
    private fun showPayrollModalDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        val etGross = EditText(this).apply {
            hint = "Salário Bruto Mensal (R$)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(payrollConfig.grossSalary.toString())
        }

        val etDependents = EditText(this).apply {
            hint = "Número de Dependentes (IRRF)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(payrollConfig.dependents.toString())
        }

        val etFixedDisc = EditText(this).apply {
            hint = "Descontos Fixos (VR/VT/Plano de Saúde)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(payrollConfig.fixedOtherDiscounts.toString())
        }

        val etMonthExtra = EditText(this).apply {
            hint = "Ajuste Exclusivo Deste Mês (+ Hora Extra / - Desconto)"
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(payrollConfig.monthExtraAdjustment.toString())
        }

        val cbLastBusinessDay = CheckBox(this).apply {
            text = "Receber no Último Dia Útil (desmarque para 5º dia útil)"
            isChecked = payrollConfig.payOnLastBusinessDay
        }

        val tvPreview = TextView(this).apply {
            textSize = 12f
            setPadding(0, dp(8), 0, dp(4))
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.brand_green_primary))
        }

        fun updateModalPreview() {
            val g = etGross.text.toString().toDoubleOrNull() ?: 0.0
            val d = etDependents.text.toString().toIntOrNull() ?: 0
            val o = etFixedDisc.text.toString().toDoubleOrNull() ?: 0.0
            val res = FinPayNativeEngine.calculatePayrollNative(g, d, o)
            tvPreview.text = "Cálculo C++ Oficial:\n" +
                "• INSS Progressivo: -${currencyFormat.format(res.inssDiscount)}\n" +
                "• IRRF Oficial: -${currencyFormat.format(res.irrfDiscount)}\n" +
                "• Líquido Base: ${currencyFormat.format(res.netSalary)}"
        }
        updateModalPreview()

        container.addView(TextView(this).apply { text = "Salário Bruto (R$):"; textSize = 11f })
        container.addView(etGross)
        container.addView(TextView(this).apply { text = "Dependentes:"; textSize = 11f })
        container.addView(etDependents)
        container.addView(TextView(this).apply { text = "Descontos Fixos CLT (VR/VT):"; textSize = 11f })
        container.addView(etFixedDisc)
        container.addView(TextView(this).apply { text = "Hora Extra / Ajuste do Mês (R$):"; textSize = 11f })
        container.addView(etMonthExtra)
        container.addView(cbLastBusinessDay)
        container.addView(tvPreview)

        AlertDialog.Builder(this)
            .setTitle("Holerite CLT Recorrente (C++)")
            .setView(container)
            .setPositiveButton("Salvar & Recalcular") { _, _ ->
                payrollConfig.grossSalary = etGross.text.toString().toDoubleOrNull() ?: 5800.0
                payrollConfig.dependents = etDependents.text.toString().toIntOrNull() ?: 0
                payrollConfig.fixedOtherDiscounts = etFixedDisc.text.toString().toDoubleOrNull() ?: 0.0
                payrollConfig.monthExtraAdjustment = etMonthExtra.text.toString().toDoubleOrNull() ?: 0.0
                payrollConfig.payOnLastBusinessDay = cbLastBusinessDay.isChecked
                repository.savePayrollConfig(payrollConfig)
                refreshAllUI()
                Toast.makeText(this, "Holerite recalculado pelo motor C++!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showAddTransactionDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        val etTitle = EditText(this).apply {
            hint = "Descrição (ex: Mercado, Uber, Pix)"
        }
        val etCategory = EditText(this).apply {
            hint = "Categoria (Alimentação, Moradia, Transporte, Lazer)"
            setText("Alimentação")
        }
        val etAmount = EditText(this).apply {
            hint = "Valor (R$)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val cbIsIncome = CheckBox(this).apply {
            text = "É uma Entrada / Receita"
            isChecked = false
        }
        val cbIsFixed = CheckBox(this).apply {
            text = "Custo ou receita fixa mensal"
            isChecked = false
        }

        container.addView(etTitle)
        container.addView(etCategory)
        container.addView(etAmount)
        container.addView(cbIsIncome)
        container.addView(cbIsFixed)

        AlertDialog.Builder(this)
            .setTitle("Novo Lançamento")
            .setView(container)
            .setPositiveButton("Adicionar") { _, _ ->
                val title = etTitle.text.toString().ifBlank { "Lançamento" }
                val category = etCategory.text.toString().ifBlank { "Geral" }
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0.0) {
                    val newItem = TransactionItem(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        category = category,
                        date = "23/10/2026",
                        amount = amount,
                        isIncome = cbIsIncome.isChecked,
                        isFixed = cbIsFixed.isChecked
                    )
                    transactions.add(0, newItem)
                    repository.saveTransactions(transactions)

                    // Atualiza o saldo da conta principal automaticamente
                    if (accounts.isNotEmpty()) {
                        accounts[0].balance += if (cbIsIncome.isChecked) amount else -amount
                        repository.saveAccounts(accounts)
                    }
                    refreshAllUI()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditAccountDialog(account: BankAccount) {
        val etBal = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(account.balance.toString())
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(etBal)
        }

        AlertDialog.Builder(this)
            .setTitle("Ajustar Saldo · ${account.name}")
            .setView(box)
            .setPositiveButton("Salvar") { _, _ ->
                account.balance = etBal.text.toString().toDoubleOrNull() ?: account.balance
                repository.saveAccounts(accounts)
                refreshAllUI()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
