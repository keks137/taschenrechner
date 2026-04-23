package bubble.keks.taschenrechner

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

data class ThemeColors(
	val col: Int,
	val text: Int,
)

data class Theme(
	val background: ThemeColors,
	val inDisplay: ThemeColors,
	val resDisplay: ThemeColors,
	val butNumber: ThemeColors,
	val butOperator: ThemeColors,
	val butAction: ThemeColors,
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
	LPAR,
	RPAR,
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

data class CalcViews(
	val root: View,
	val inp: TextView,
	val res: TextView,
	val pad: GridLayout,
)

data class ConvertViews(
	val root: View,
	val inp: TextView,
	val res: TextView,
	val pad: GridLayout,
	val fromUnit: Spinner,
	val toUnit: Spinner,
	val convertButton: Button,
)

enum class Mode { CALC, CONVERT }

val CWELT_THEME =
	Theme(
		background = ThemeColors(col = Color.parseColor("#000000"), text = Color.parseColor("#FEED01")),
		inDisplay =
			ThemeColors(
				col = Color.parseColor("#000000"),
				text = Color.parseColor("#FEED01"),
			),
		resDisplay =
			ThemeColors(
				col = Color.parseColor("#000000"),
				text = Color.parseColor("#FEED01"),
			),
		butNumber =
			ThemeColors(
				col = Color.parseColor("#FEED01"),
				text = Color.parseColor("#000000"),
			),
		butAction =
			ThemeColors(
				col = Color.parseColor("#5F5F5F"),
				text = Color.parseColor("#FEED01"),
			),
		butOperator =
			ThemeColors(
				col = Color.parseColor("#DBDBDB"),
				text = Color.parseColor("#000000"),
			),
	)

val DIGIT_TOKENS =
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

val BUTTON_LAYOUT =
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
		BK.LPAR,
		BK.RPAR,
		BK.CLEAR,
		BK.NUM_0,
		BK.DOT,
		BK.BS,
		BK.EQ,
	)

class MainActivity : Activity() {
	private val scale = 10

	private var state = CalcState()
	private val theme = CWELT_THEME

	private lateinit var calc: CalcViews
	private lateinit var convert: ConvertViews


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)


		state = CalcState()

		calc = buildCalcScreen()
		convert = buildConvertScreen()

		val root =
			FrameLayout(this).apply {
				addView(calc.root)
				addView(convert.root.apply { visibility = View.GONE })
			}
		setContentView(root)
	}

	private fun buildCalcScreen(): CalcViews {
		val input =
			TextView(this).apply {
				text = ""
				textSize = 24f
				setTextColor(theme.inDisplay.text)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(16), dp(16), dp(8))
			}

		val result =
			TextView(this).apply {
				text = "0"
				textSize = 48f
				setTextColor(theme.resDisplay.text)
				gravity = Gravity.END or Gravity.CENTER_VERTICAL
				setPadding(dp(16), dp(8), dp(16), dp(32))
			}

		val pad = buildButtonGrid { handleCalcInput(it) }

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setBackgroundColor(theme.background.col)
				setPadding(dp(16), dp(16), dp(16), dp(16))
				addView(modeToggle { switchMode(Mode.CONVERT) })
				addView(input)
				addView(result)
				addView(
					View(context).apply {
						layoutParams =
							LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								0,
								1f,
							)
					},
				)

				addView(pad)
			}

		return CalcViews(root, input, result, pad)
	}

	private fun buildConvertScreen(): ConvertViews {
		val input =
			TextView(this).apply {
				text = "0"
				textSize = 32f
				setTextColor(theme.inDisplay.text)
				gravity = Gravity.END
				setPadding(dp(16), dp(24), dp(16), dp(8))
			}

		val result =
			TextView(this).apply {
				text = "0"
				textSize = 48f
				setTextColor(theme.resDisplay.text)
				gravity = Gravity.END
				setPadding(dp(16), dp(32), dp(16), dp(16))
			}

		val fromUnit = Spinner(this)
		val toUnit = Spinner(this)

		val unitRow =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				weightSum = 2f
				addView(
					fromUnit.apply {
						layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
					},
				)
				addView(
					toUnit.apply {
						layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
					},
				)
			}

		val convertButton =
			Button(this).apply {
				text = "Convert"
				textSize = 24f
				backgroundTintList =
					android.content.res.ColorStateList
						.valueOf(theme.butOperator.col)
				setTextColor(theme.butOperator.text)
				setOnClickListener { doConversion() }
			}

		val pad = buildButtonGrid { handleConvertInput(it) }

		val root =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setBackgroundColor(theme.background.col)
				setPadding(dp(16), dp(16), dp(16), dp(16))
				addView(modeToggle { switchMode(Mode.CALC) })
				addView(input)
				addView(unitRow)
				addView(convertButton)
				addView(result)
				addView(
					View(context).apply {
						layoutParams =
							LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								0,
								1f,
							)
					},
				)

				addView(pad)
			}

		return ConvertViews(root, input, result, pad, fromUnit, toUnit, convertButton)
	}

	private fun modeToggle(onClick: () -> Unit): Button =
		Button(this).apply {
			text = "⇄"
			textSize = 20f
			backgroundTintList =
				android.content.res.ColorStateList
					.valueOf(theme.butAction.col)
			setTextColor(theme.butAction.text)
			setOnClickListener { onClick() }
		}

	private fun buildButtonGrid(onClick: (BK) -> Unit): GridLayout =
		GridLayout(this).apply {
			rowCount = 4
			columnCount = 5

			BUTTON_LAYOUT.forEach { bkind ->
				addView(makeButton(bkind, onClick))
			}
		}

	private fun makeButton(
		bkind: BK,
		onClick: (BK) -> Unit,
	): Button {
		val (bgColor, textColor) =
			when (bkind) {
				BK.EQ, BK.CLEAR, BK.BS -> theme.butAction.col to theme.butAction.text
				in DIGIT_TOKENS -> theme.butNumber.col to theme.butNumber.text
				else -> theme.butOperator.col to theme.butOperator.text
			}

		return Button(this).apply {
			text = buttonStr(bkind)
			textSize = 24f
			backgroundTintList =
				android.content.res.ColorStateList
					.valueOf(bgColor)
			setTextColor(textColor)
			layoutParams =
				GridLayout.LayoutParams().apply {
					width = 0
					height = dp(64)
					columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
					rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
					setMargins(dp(4), dp(4), dp(4), dp(4))
				}
			setOnClickListener { onClick(bkind) }
		}
	}

	private fun switchMode(newMode: Mode) {
		state.mode = newMode
		calc.root.visibility = if (newMode == Mode.CALC) View.VISIBLE else View.GONE
		convert.root.visibility = if (newMode == Mode.CONVERT) View.VISIBLE else View.GONE

		if (newMode == Mode.CONVERT) {
			convert.inp.text = tokensToString(state.tokens).ifEmpty { "0" }
		}
	}

	private fun handleCalcInput(bkind: BK) {
		state.allowFallback = true
		when (bkind) {
			BK.CLEAR -> state.tokens.clear()
			BK.EQ -> state.allowFallback = false
			BK.BS -> state.tokens.removeLastOrNull()
			else -> state.tokens.add(bkind)
		}
		calculate()
		calc.inp.text = tokensToString(state.tokens)
		calc.res.text = state.strResult.ifEmpty { "0" }
	}

	private fun handleConvertInput(bkind: BK) {
		val current = convert.inp.text.toString()
		when (bkind) {
			BK.CLEAR -> {
				convert.inp.text = "0"
			}

			BK.BS -> {
				convert.inp.text = current.dropLast(1).ifEmpty { "0" }
			}

			BK.DOT -> {
				if (!current.contains(".")) convert.inp.text = "$current."
			}

			in DIGIT_TOKENS -> {
				val digit = buttonStr(bkind)
				convert.inp.text = if (current == "0") digit else "$current$digit"
			}

			else -> {}
		}
	}

	private fun doConversion() {
		// TODO: implement conversion logic
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
			BK.LPAR -> "("
			BK.RPAR -> ")"
			BK.NONE -> "unknown"
		}

	private fun updateCalcDisplays() {
		calc.inp.text = tokensToString(state.tokens)
		calc.res.text = state.strResult.ifEmpty { "0" }
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
		updateCalcDisplays()
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
		var left = unaryOrParen() ?: return null

		while (true) {
			when (peek()) {
				BK.MUL -> {
					consume()
					val right = unaryOrParen() ?: return null
					left = left.multiply(right)
				}

				BK.DIV -> {
					consume()
					val right = unaryOrParen() ?: return null
					if (right == BigDecimal.ZERO) return null
					left = left.divide(right, 10, RoundingMode.HALF_UP)
				}

				BK.LPAR, in DIGIT_TOKENS -> {
					val right = unaryOrParen() ?: return null
					left = left.multiply(right)
				}

				else -> {
					break
				}
			}
		}
		return left
	}

	private fun unaryOrParen(): BigDecimal? {
		if (peek() == BK.SUB) {
			consume()
			val inner = unaryOrParen() ?: return null
			return inner.negate()
		}
		return parenOrNumber()
	}

	private fun parenOrNumber(): BigDecimal? {
		return when (peek()) {
			BK.LPAR -> {
				consume()
				val expr = addSub() ?: return null
				when (peek()) {
					BK.RPAR -> {
						consume()
					}

					null -> { /* auto-close at end of input */ }

					else -> {
						return null
					}
				}
				expr
			}

			else -> {
				number()
			}
		}
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
