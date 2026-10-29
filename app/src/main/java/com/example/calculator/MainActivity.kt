package com.example.calculator

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var tvHistory: TextView

    private var expression = ""          // what the user has typed, e.g. "12+3×2"
    private var justEvaluated = false    // true right after pressing "="
    private val operators = "+−×÷"

    // Hunter x Hunter flavoured messages shown after "="
    private val messages = listOf(
        "Hunter Exam passed!",
        "Nen aura stable. Answer confirmed.",
        "Killua-speed calculation!",
        "Gon would be proud of this result.",
        "Hunter License upgraded!",
        "Target located. Calculation complete."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the saved light/dark choice BEFORE the screen is built
        val prefs = getSharedPreferences("hxh_calc", MODE_PRIVATE)
        if (prefs.contains("night")) {
            AppCompatDelegate.setDefaultNightMode(
                if (prefs.getBoolean("night", false)) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Keeps content clear of the status and navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvDisplay = findViewById(R.id.tvDisplay)
        tvHistory = findViewById(R.id.tvHistory)

        // Digit keys
        val digitKeys = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9"
        )
        for ((id, digit) in digitKeys) {
            findViewById<View>(id).setOnClickListener { appendDigit(digit) }
        }

        // Operator keys
        val operatorKeys = mapOf(
            R.id.btnAdd to '+', R.id.btnSubtract to '−',
            R.id.btnMultiply to '×', R.id.btnDivide to '÷'
        )
        for ((id, op) in operatorKeys) {
            findViewById<View>(id).setOnClickListener { appendOperator(op) }
        }

        // Other keys
        findViewById<View>(R.id.btnDot).setOnClickListener { appendDot() }
        findViewById<View>(R.id.btnPercent).setOnClickListener { appendPercent() }
        findViewById<View>(R.id.btnClear).setOnClickListener { clearAll() }
        findViewById<View>(R.id.btnDel).setOnClickListener { deleteLast() }
        findViewById<View>(R.id.btnEquals).setOnClickListener { calculate() }

        // Light / Dark toggle: save the choice, then switch the whole theme
        findViewById<View>(R.id.btnTheme).setOnClickListener {
            val isNight = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            prefs.edit().putBoolean("night", !isNight).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isNight) AppCompatDelegate.MODE_NIGHT_NO
                else AppCompatDelegate.MODE_NIGHT_YES
            )
        }

        // After a theme switch the screen is rebuilt, so restore what was typed
        if (savedInstanceState != null) {
            expression = savedInstanceState.getString("expr") ?: ""
            tvHistory.text = savedInstanceState.getString("hist") ?: ""
            justEvaluated = savedInstanceState.getBoolean("done", false)
        }
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("expr", expression)
        outState.putString("hist", tvHistory.text.toString())
        outState.putBoolean("done", justEvaluated)
    }

    // ---------- Typing ----------

    private fun currentNumber(): String =
        expression.takeLastWhile { it.isDigit() || it == '.' }

    private fun startFreshIfNeeded() {
        if (justEvaluated) {
            expression = ""
            tvHistory.text = ""
            justEvaluated = false
        }
    }

    private fun appendDigit(digit: String) {
        startFreshIfNeeded()
        if (expression.endsWith("%")) return
        if (currentNumber() == "0") expression = expression.dropLast(1) // no "007"
        expression += digit
        refresh()
    }

    private fun appendDot() {
        startFreshIfNeeded()
        if (expression.endsWith("%")) return
        val number = currentNumber()
        if (number.contains('.')) return
        expression += if (number.isEmpty()) "0." else "."
        refresh()
    }

    private fun appendOperator(op: Char) {
        justEvaluated = false // keep the result on screen and continue from it
        if (expression.isEmpty()) {
            if (op == '−') expression = "−" // allows starting with a negative number
        } else if (expression != "−") {
            expression = if (expression.last() in operators) {
                expression.dropLast(1) + op // replace the previous operator
            } else {
                expression + op
            }
        }
        refresh()
    }

    private fun appendPercent() {
        if (expression.isNotEmpty() && expression.last().isDigit()) {
            justEvaluated = false
            expression += "%"
            refresh()
        }
    }

    private fun deleteLast() {
        if (justEvaluated) { clearAll(); return }
        expression = expression.dropLast(1)
        refresh()
    }

    private fun clearAll() {
        expression = ""
        justEvaluated = false
        tvHistory.text = ""
        refresh()
    }

    private fun refresh() {
        tvDisplay.text = expression.ifEmpty { "0" }
    }

    // ---------- Calculating ----------

    private fun calculate() {
        var expr = expression
        while (expr.isNotEmpty() && expr.last() in operators) expr = expr.dropLast(1)
        if (expr.isEmpty() || expr == "−") return

        try {
            val result = format(Evaluator(expr).evaluate())
            tvHistory.text = "$expr ="
            expression = result.replace('-', '−')
            justEvaluated = true
            refresh()
            pulseDisplay()
            Toast.makeText(this, messages.random(), Toast.LENGTH_SHORT).show()
        } catch (e: ArithmeticException) {
            showError("Can't divide by 0")
        } catch (e: NumberFormatException) {
            showError("Error")
        }
    }

    private fun showError(message: String) {
        expression = ""
        justEvaluated = true
        tvHistory.text = ""
        tvDisplay.text = message
    }

    // Shows 5.0 as "5" and 0.30000000000000004 as "0.3"
    private fun format(value: BigDecimal): String {
        val cleaned = value.stripTrailingZeros()
        return if (cleaned.signum() == 0) "0" else cleaned.toPlainString()
    }

    // A small "aura" pulse on the display when "=" is pressed
    private fun pulseDisplay() {
        tvDisplay.pivotX = tvDisplay.width.toFloat()
        tvDisplay.pivotY = tvDisplay.height / 2f
        tvDisplay.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).withEndAction {
            tvDisplay.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }
}

// Reads text like "12+3×2" and respects the order of operations
private class Evaluator(private val s: String) {
    private var pos = 0

    fun evaluate(): BigDecimal = parseExpression()

    private fun peek(): Char? = if (pos < s.length) s[pos] else null

    // + and − (lowest priority)
    private fun parseExpression(): BigDecimal {
        var value = parseTerm()
        while (true) {
            when (peek()) {
                '+' -> { pos++; value = value.add(parseTerm()) }
                '−' -> { pos++; value = value.subtract(parseTerm()) }
                else -> return value
            }
        }
    }

    // × and ÷ (higher priority)
    private fun parseTerm(): BigDecimal {
        var value = parseFactor()
        while (true) {
            when (peek()) {
                '×' -> { pos++; value = value.multiply(parseFactor()) }
                '÷' -> { pos++; value = value.divide(parseFactor(), MathContext.DECIMAL64) }
                else -> return value
            }
        }
    }

    // A number, optionally negative, optionally followed by %
    private fun parseFactor(): BigDecimal {
        if (peek() == '−') { pos++; return parseFactor().negate() }
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        var value = BigDecimal(s.substring(start, pos))
        while (peek() == '%') { pos++; value = value.divide(BigDecimal(100)) }
        return value
    }
}