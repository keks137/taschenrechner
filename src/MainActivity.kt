package bubble.keks.taschenrechner

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

data class Theme(
	val background: Int,
	val backgroundText: Int,
	val inDisplay: Int,
	val inDisplayText: Int,
	val resDisplay: Int,
	val resDisplayText: Int,
	val butNumber: Int,
	val butNumberText: Int,
	val butOperator: Int,
	val butOperatorText: Int,
	val butAction: Int,
	val butActionText: Int,
)

enum class ButtonKind { NUMBER, EQ, CLEAR }

enum class UIKind {
	BACKGROUND,
	RES_DISPLAY,
	IN_DISPLAY,
	BUT_NUM,
	BUT_OP,
	BUT_AC,
}

data class CalcState(
	val mode: Mode = Mode.CALC,
	val strIn: String = "",
	val strResult: String = "",
)

enum class Mode { CALC, CONVERT }

val CWELT_THEME =
	Theme(
		background = Color.parseColor("#000000"),
		backgroundText = Color.parseColor("#FEED01"),
		inDisplay = Color.parseColor("#000000"),
		inDisplayText = Color.parseColor("#FEED01"),
		resDisplay = Color.parseColor("#000000"),
		resDisplayText = Color.parseColor("#FEED01"),
		butNumber = Color.parseColor("#FEED01"),
		butNumberText = Color.parseColor("#000000"),
		butAction = Color.parseColor("#5F5F5F"),
		butActionText = Color.parseColor("#FEED01"),
		butOperator = Color.parseColor("#DBDBDB"),
		butOperatorText = Color.parseColor("#000000"),
	)

class MainActivity : Activity() {
	private var currentInput = StringBuilder()
	private var previousValue: BigDecimal? = null
	private var pendingOp: String? = null
	private val scale = 10

	private var state = CalcState()
	private val theme = CWELT_THEME

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		if (hasFocus) {
			window.decorView.systemUiVisibility = (
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
					View.SYSTEM_UI_FLAG_FULLSCREEN or
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			)
		}
	}

	private fun buildUi() {
		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setBackgroundColor(theme.background)
				layoutParams =
					LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.MATCH_PARENT,
					)
				setPadding(dp(16), dp(16), dp(16), dp(16))
			}

		val inDisplay =
			TextView(this).apply {
				text = ""
				textSize = 24f
				setTextColor(theme.inDisplayText)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(16), dp(16), dp(8))
			}

		val resDisplay =
			TextView(this).apply {
				text = "0"
				textSize = 48f
				setTextColor(theme.resDisplayText)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(8), dp(16), dp(32))
			}

		val grid =
			GridLayout(this).apply {
				rowCount = 4
				columnCount = 4
				layoutParams =
					LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT,
					)
			}

		val buttons =
			listOf(
				"7",
				"8",
				"9",
				"/",
				"4",
				"5",
				"6",
				"*",
				"1",
				"2",
				"3",
				"-",
				"C",
				"0",
				"+",
				"<->",
				"=",
			)

		buttons.forEach { label ->
			val btn =
				Button(this).apply {
					text = label
					textSize = 24f
					when (label)
					{
						"=", "C", "<->" -> {
							backgroundTintList =
								android.content.res.ColorStateList
									.valueOf(theme.butAction)

							setTextColor(theme.butActionText)
						}

						"/", "*", "-", "+" -> {
							backgroundTintList =android.content.res.ColorStateList.valueOf( theme.butOperator)

							setTextColor(theme.butOperatorText)
						}

						else -> {
							backgroundTintList =android.content.res.ColorStateList.valueOf( theme.butNumber)

							setTextColor(theme.butNumberText)
						}
					}
					layoutParams =
						GridLayout.LayoutParams().apply {
							width = 0
							height = dp(64)
							columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
							rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
							setMargins(dp(4), dp(4), dp(4), dp(4))
						}
					setOnClickListener {
						handleInput(label, resDisplay, inDisplay)
					}
				}
			grid.addView(btn)
		}

		root.addView(inDisplay)
		root.addView(resDisplay)
		root.addView(grid)
		setContentView(root)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		window.decorView.systemUiVisibility = (
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
				View.SYSTEM_UI_FLAG_FULLSCREEN or
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		)

		buildUi()
	}

	private fun handleInput(
		label: String,
		display: TextView,
		expressionDisplay: TextView,
	) {
		when (label) {
			"C" -> {
				currentInput.clear()
				previousValue = null
				pendingOp = null
				display.text = "0"
				expressionDisplay.text = ""
			}

			"=" -> {
				calculate(display, expressionDisplay)
			}

			"<->" -> {}

			"+", "-", "*", "/" -> {
				if (currentInput.isNotEmpty()) {
					previousValue = currentInput.toString().toBigDecimalOrNull()
					pendingOp = label
					currentInput.clear()
					updateExpression(expressionDisplay)
				}
			}

			else -> {
				currentInput.append(label)
				display.text = currentInput.toString()
				updateExpression(expressionDisplay)
			}
		}
	}

	private fun updateExpression(expressionDisplay: TextView) {
		val expr = StringBuilder()
		if (previousValue != null) {
			expr.append(previousValue!!.stripTrailingZeros().toPlainString())
			expr.append(" ")
			expr.append(pendingOp)
			expr.append(" ")
		}
		if (currentInput.isNotEmpty()) {
			expr.append(currentInput.toString())
		}
		expressionDisplay.text = expr.toString()
	}

	private fun calculate(
		resDisplay: TextView,
		inDisplay: TextView,
	) {
		val current = currentInput.toString().toBigDecimalOrNull() ?: return
		val prev = previousValue ?: return
		val op = pendingOp ?: return

		val result =
			when (op) {
				"+" -> {
					prev.add(current)
				}

				"-" -> {
					prev.subtract(current)
				}

				"*" -> {
					prev.multiply(current)
				}

				"/" -> {
					if (current.compareTo(BigDecimal.ZERO) != 0) {
						prev.divide(current, scale, RoundingMode.HALF_UP)
					} else {
						null
					}
				}

				else -> {
					return
				}
			}

		if (result == null) {
			resDisplay.text = "Error"
			currentInput.clear()
			previousValue = null
			pendingOp = null
			return
		}

		resDisplay.text = result.stripTrailingZeros().toPlainString()

		// currentInput.clear().append(result)
		previousValue = null
		pendingOp = null
	}

	private fun dp(px: Int): Int = (px * resources.displayMetrics.density).toInt()
}
