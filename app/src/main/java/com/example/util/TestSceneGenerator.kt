package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.random.Random

enum class TestSceneType(val label: String, val icon: String) {
    TRADING("Trading BTC/USD", "📈"),
    GAMING("Table Casino / Jeu", "🎲"),
    SPORTS("Paris Sportifs Live", "⚽"),
    CAMERA_REAL("Caméra Physiques (En direct)", "📷")
}

object TestSceneGenerator {
    fun generateBitmap(type: TestSceneType, width: Int = 1280, height: Int = 720): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (type) {
            TestSceneType.TRADING -> drawTradingChart(canvas, width, height)
            TestSceneType.GAMING -> drawCasinoScene(canvas, width, height)
            TestSceneType.SPORTS -> drawSportsScene(canvas, width, height)
            TestSceneType.CAMERA_REAL -> drawCameraPlaceholder(canvas, width, height)
        }

        return bitmap
    }

    private fun drawTradingChart(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Dark background
        canvas.drawColor(Color.parseColor("#0F172A"))

        // Grid lines
        paint.color = Color.parseColor("#1E293B")
        paint.strokeWidth = 2f
        for (x in 0..w step 100) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), h.toFloat(), paint)
        }
        for (y in 0..h step 80) {
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), paint)
        }

        // Header Title & Prices
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("PROXIYA TRADE • BTC/USDT (15M)", 40f, 60f, paint)

        paint.color = Color.parseColor("#22C55E")
        paint.textSize = 48f
        canvas.drawText("$67,480.50 (+4.25%)", 40f, 120f, paint)

        // Draw Candlesticks
        val rand = Random(42)
        var currentY = h * 0.5f
        val candleWidth = 24f
        val gap = 16f
        var startX = 60f

        for (i in 0..25) {
            val change = (rand.nextFloat() - 0.48f) * 60f
            val open = currentY
            val close = open - change
            val isGreen = close < open

            val high = minOf(open, close) - rand.nextFloat() * 30f
            val low = maxOf(open, close) + rand.nextFloat() * 30f

            // Wick
            paint.color = if (isGreen) Color.parseColor("#22C55E") else Color.parseColor("#EF4444")
            paint.strokeWidth = 3f
            val cx = startX + candleWidth / 2
            canvas.drawLine(cx, high, cx, low, paint)

            // Body
            paint.style = Paint.Style.FILL
            val top = minOf(open, close)
            val bottom = maxOf(open, close)
            canvas.drawRect(startX, top, startX + candleWidth, bottom, paint)

            currentY = close
            startX += candleWidth + gap
        }

        // EMA Moving Average Line
        val path = Path()
        paint.color = Color.parseColor("#F59E0B")
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE

        path.moveTo(60f, h * 0.55f)
        path.cubicTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.4f, w - 80f, h * 0.35f)
        canvas.drawPath(path, paint)

        // Overlay RSI Box
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E293B")
        val rsiRect = RectF(w - 350f, h - 180f, w - 40f, h - 40f)
        canvas.drawRoundRect(rsiRect, 16f, 16f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 28f
        canvas.drawText("RSI (14): 68.4 [Achat Fort]", w - 330f, h - 120f, paint)
        paint.color = Color.parseColor("#22C55E")
        canvas.drawText("Signal: Cassure Bullish ↗", w - 330f, h - 70f, paint)
    }

    private fun drawCasinoScene(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Green Felt background
        canvas.drawColor(Color.parseColor("#064E3B"))

        // Table border
        paint.color = Color.parseColor("#047857")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 20f
        canvas.drawRect(30f, 30f, w - 30f, h - 30f, paint)

        // Title
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FDE047")
        paint.textSize = 42f
        paint.isFakeBoldText = true
        canvas.drawText("TABLE CASINO LIVE • TEXAS HOLD'EM & ROULETTE", 50f, 80f, paint)

        // Cards on Table
        val cardWidth = 110f
        val cardHeight = 160f

        val cards = listOf("A♠", "K♠", "Q♥", "10♦", "J♠")
        var cardX = 120f
        val cardY = 220f

        for (card in cards) {
            // Card background
            paint.color = Color.WHITE
            val cardRect = RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight)
            canvas.drawRoundRect(cardRect, 12f, 12f, paint)

            // Card text
            paint.color = if (card.contains("♥") || card.contains("♦")) Color.RED else Color.BLACK
            paint.textSize = 34f
            canvas.drawText(card, cardX + 18f, cardY + 50f, paint)

            cardX += cardWidth + 30f
        }

        // Pot & Odds Info
        paint.color = Color.parseColor("#10B981")
        val potRect = RectF(50f, h - 220f, w - 50f, h - 50f)
        canvas.drawRoundRect(potRect, 20f, 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 34f
        canvas.drawText("POT ACTUEL: 2,450€ • PROBABILITÉ DE QUINTE FLUSH: 88%", 80f, h - 150f, paint)
        paint.textSize = 28f
        paint.color = Color.parseColor("#FDE047")
        canvas.drawText("Recommandation IA: Relancer (Raise 3x Big Blind)", 80f, h - 90f, paint)
    }

    private fun drawSportsScene(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Pitch background
        canvas.drawColor(Color.parseColor("#1E1B4B"))

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.isFakeBoldText = true
        canvas.drawText("MATCH EN DIRECT • LIGUE DES CHAMPIONS (78')", 50f, 80f, paint)

        // Scoreboard Box
        paint.color = Color.parseColor("#312E81")
        val scoreBox = RectF(50f, 130f, w - 50f, 320f)
        canvas.drawRoundRect(scoreBox, 24f, 24f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 64f
        canvas.drawText("REAL MADRID  2 - 1  MAN CITY", 120f, 240f, paint)

        // Odds grid
        paint.textSize = 30f
        paint.color = Color.WHITE
        val oddsBox1 = RectF(50f, 360f, w / 3f - 20f, 520f)
        val oddsBox2 = RectF(w / 3f + 10f, 360f, 2f * w / 3f - 10f, 520f)
        val oddsBox3 = RectF(2f * w / 3f + 20f, 360f, w - 50f, 520f)

        paint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(oddsBox1, 16f, 16f, paint)
        canvas.drawRoundRect(oddsBox2, 16f, 16f, paint)
        canvas.drawRoundRect(oddsBox3, 16f, 16f, paint)

        paint.color = Color.parseColor("#22C55E")
        canvas.drawText("Victoire Real: 1.45", 70f, 440f, paint)
        paint.color = Color.parseColor("#EF4444")
        canvas.drawText("Nul: 3.80", w / 3f + 40f, 440f, paint)
        paint.color = Color.parseColor("#EF4444")
        canvas.drawText("Victoire City: 6.50", 2f * w / 3f + 40f, 440f, paint)

        // AI Live Alert Banner
        paint.color = Color.parseColor("#4338CA")
        val alertBox = RectF(50f, h - 140f, w - 50f, h - 40f)
        canvas.drawRoundRect(alertBox, 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText("⚡ IA ALERT: Pression offensive forte de City (8 tir cadrés). Fortes chances de +0.5 but !", 70f, h - 80f, paint)
    }

    private fun drawCameraPlaceholder(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.parseColor("#0F172A"))

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Flux Caméra En Direct Active", (w / 2).toFloat(), (h / 2 - 40).toFloat(), paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 26f
        canvas.drawText("Pointez l'objectif de votre smartphone vers votre écran de jeu ou trading", (w / 2).toFloat(), (h / 2 + 20).toFloat(), paint)
    }
}
