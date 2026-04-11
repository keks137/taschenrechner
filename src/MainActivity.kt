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

data class ThemeComponent(
	val col: Int,
	val text: Int,
)

data class Theme(
	val background: ThemeComponent,
	val inDisplay: ThemeComponent,
	val resDisplay: ThemeComponent,
	val butNumber: ThemeComponent,
	val butOperator: ThemeComponent,
	val butAction: ThemeComponent,
)

enum class BK {
	NONE,
	NUM_0,
	NUM_1,
	NUM_2,
	NUM_3,
	NUM_4,
	NUM_5,
	NUM_6,
	NUM_7,
	NUM_8,
	NUM_9,
	ADD,
	SUB,
	MUL,
	BS,
	DIV,
	DOT,
	EQ,
	CLEAR,
}

enum class UIKind {
	BACKGROUND,
	RES_DISPLAY,
	IN_DISPLAY,
	BUT_NUM,
	BUT_OP,
	BUT_AC,
}

data class CalcState(
	var mode: Mode = Mode.CALC,
	var strResult: String = "",
	var tokens: MutableList<BK> = mutableListOf(),
	var tpos: Int = 0,
	var allowFallback: Boolean = false,
)

enum class Mode { CALC, CONVERT }

val CWELT_THEME =
	Theme(
		background = ThemeComponent(col = Color.parseColor("#000000"), text = Color.parseColor("#FEED01")),
		inDisplay =
			ThemeComponent(
				col = Color.parseColor("#000000"),
				text = Color.parseColor("#FEED01"),
			),
		resDisplay =
			ThemeComponent(
				col = Color.parseColor("#000000"),
				text = Color.parseColor("#FEED01"),
			),
		butNumber =
			ThemeComponent(
				col = Color.parseColor("#FEED01"),
				text = Color.parseColor("#000000"),
			),
		butAction =
			ThemeComponent(
				col = Color.parseColor("#5F5F5F"),
				text = Color.parseColor("#FEED01"),
			),
		butOperator =
			ThemeComponent(
				col = Color.parseColor("#DBDBDB"),
				text = Color.parseColor("#000000"),
			),
	)

class MainActivity : Activity() {
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

	private fun tokensToString(tokens: List<BK>): String = tokens.joinToString("") { buttonStr(it) }

	private fun buttonStr(buttonKind: BK): String =
		when (buttonKind) {
			BK.EQ -> "="
			BK.BS -> "<-"
			BK.ADD -> "+"
			BK.SUB -> "-"
			BK.MUL -> "*"
			BK.DIV -> "/"
			BK.DOT -> "."
			BK.CLEAR -> "C"
			BK.NUM_0 -> "0"
			BK.NUM_1 -> "1"
			BK.NUM_2 -> "2"
			BK.NUM_3 -> "3"
			BK.NUM_4 -> "4"
			BK.NUM_5 -> "5"
			BK.NUM_6 -> "6"
			BK.NUM_7 -> "7"
			BK.NUM_8 -> "8"
			BK.NUM_9 -> "9"
			else -> "unknown"
		}

	private lateinit var inDisplay: TextView
	private lateinit var resDisplay: TextView

	private fun updateDisplays() {
		inDisplay.text = tokensToString(state.tokens)
		resDisplay.text = state.strResult.ifEmpty { "0" }
	}

	private fun buildUi() {
		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setBackgroundColor(theme.background.col)
				layoutParams =
					LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.MATCH_PARENT,
					)
				setPadding(dp(16), dp(16), dp(16), dp(16))
			}

		inDisplay =
			TextView(this).apply {
				text = ""
				textSize = 24f
				setTextColor(theme.inDisplay.text)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(16), dp(16), dp(8))
			}

		resDisplay =
			TextView(this).apply {
				text = "0"
				textSize = 48f
				setTextColor(theme.resDisplay.text)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(8), dp(16), dp(32))
			}

		val grid =
			GridLayout(this).apply {
				rowCount = 4
				columnCount = 5
				layoutParams =
					LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT,
					)
			}

		val buttons =
			arrayOf(
				BK.NUM_7,
				BK.NUM_8,
				BK.NUM_9,
				BK.ADD,
				BK.SUB,
				BK.NUM_4,
				BK.NUM_5,
				BK.NUM_6,
				BK.MUL,
				BK.DIV,
				BK.NUM_1,
				BK.NUM_2,
				BK.NUM_3,
				BK.NONE,
				BK.NONE,
				BK.CLEAR,
				BK.NUM_0,
				BK.DOT,
				BK.BS,
				BK.EQ,
			)

		buttons.forEach { bkind ->
			val btn =
				Button(this).apply {
					text = buttonStr(bkind)
					textSize = 24f
					when (bkind)
					{
						BK.EQ, BK.CLEAR, BK.BS -> {
							backgroundTintList =
								android.content.res.ColorStateList
									.valueOf(theme.butAction.col)

							setTextColor(theme.butAction.text)
						}

						BK.NUM_0,
						BK.NUM_1,
						BK.NUM_2,
						BK.NUM_3,
						BK.NUM_4,
						BK.NUM_5,
						BK.NUM_6,
						BK.NUM_7,
						BK.NUM_8,
						BK.NUM_9,
						-> {
							backgroundTintList =
								android.content.res.ColorStateList
									.valueOf(theme.butNumber.col)

							setTextColor(theme.butNumber.text)
						}

						// 	BK.ADD, BK.SUB, BK.MUL, BK.DIV , BK.DOT
						else -> {
							backgroundTintList =
								android.content.res.ColorStateList
									.valueOf(theme.butOperator.col)

							setTextColor(theme.butOperator.text)
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
						handleInput(bkind)
					}
				}
			grid.addView(btn)
		}

		root.addView(inDisplay)
		root.addView(resDisplay)
		root.addView(grid)
		setContentView(root)
		updateDisplays()
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

	private fun handleInput(bkind: BK) {
		state.allowFallback = true
		when (bkind) {
			BK.CLEAR -> {
				state.tokens.clear()
			}

			BK.EQ -> {
				state.allowFallback = false
			}

			BK.BS -> {
				state.tokens.removeLastOrNull()
			}

			// "<->" -> {}

			// BK.ADD, BK.SUB, BK.MUL, BK.DIV -> {
			// 	// if (inDisplay.isNotEmpty()) {
			// 		updateExpression(expressionDisplay)
			// 	// }
			// }

			else -> {
				state.tokens.add(bkind)
			}
		}
		calculate()
		updateDisplays()
	}

	private fun calculate() {
		if (state.tokens.isEmpty()) {
			state.strResult = ""
			return
		}

		val result = evaluate()
		if (result != null) {
			state.strResult = result.stripTrailingZeros().toPlainString()
		} else if (!state.allowFallback) {
			// Strict mode: show error when = was pressed
			state.strResult = "?"
		}
		state.tpos = 0
	}

	private fun peek() = state.tokens.getOrNull(state.tpos)

	private fun consume() = state.tokens.getOrNull(state.tpos++)

	private fun addSub(): BigDecimal? {
		var left = mulDiv() ?: return null
		while (true) {
			when (peek()) {
				BK.ADD -> {
					consume()
					val right = mulDiv() ?: return null
					left = left.add(right)
				}

				BK.SUB -> {
					consume()
					val right = mulDiv() ?: return null
					left = left.subtract(right)
				}

				else -> {
					break
				}
			}
		}
		return left
	}

	private fun mulDiv(): BigDecimal? {
		var left = unaryOrNumber() ?: return null

		while (true) {
			when (peek()) {
				BK.MUL -> {
					consume()
					val right = unaryOrNumber() ?: return null
					left = left.multiply(right)
				}

				BK.DIV -> {
					consume()
					val right = unaryOrNumber() ?: return null
					if (right == BigDecimal.ZERO) return null // division by zero
					left = left.divide(right, 10, RoundingMode.HALF_UP)
				}

				else -> {
					break
				}
			}
		}
		return left
	}

	private fun unaryOrNumber(): BigDecimal? {
		if (peek() == BK.SUB) {
			consume()
			val inner = unaryOrNumber() ?: return null
			return inner.negate()
		}
		return number()
	}

	private val DIGIT_TOKENS =
		setOf(
			BK.NUM_0,
			BK.NUM_1,
			BK.NUM_2,
			BK.NUM_3,
			BK.NUM_4,
			BK.NUM_5,
			BK.NUM_6,
			BK.NUM_7,
			BK.NUM_8,
			BK.NUM_9,
		)

	private fun BK.toDigit(): Char? =
		when (this) {
			BK.NUM_0 -> '0'
			BK.NUM_1 -> '1'
			BK.NUM_2 -> '2'
			BK.NUM_3 -> '3'
			BK.NUM_4 -> '4'
			BK.NUM_5 -> '5'
			BK.NUM_6 -> '6'
			BK.NUM_7 -> '7'
			BK.NUM_8 -> '8'
			BK.NUM_9 -> '9'
			else -> null
		}

	private fun number(): BigDecimal? {
		val sb = StringBuilder()
		var hasDot = false

		while (true) {
			when (peek()) {
				in DIGIT_TOKENS -> {
					peek()?.toDigit()?.let { sb.append(it) }
					consume()
				}

				BK.DOT -> {
					if (hasDot) break
					hasDot = true
					sb.append('.')
					consume()
				}

				else -> {
					break
				}
			}
		}

		if (sb.isEmpty() || sb.toString() == ".") return null
		return sb.toString().toBigDecimalOrNull()
	}

	private fun evaluate(): BigDecimal? {
		var tokens = state.tokens

		val result = addSub()
		// Ensure we consumed all tokens
		if (state.tpos != tokens.size) return null
		return result
	}

	private fun dp(px: Int): Int = (px * resources.displayMetrics.density).toInt()
}
