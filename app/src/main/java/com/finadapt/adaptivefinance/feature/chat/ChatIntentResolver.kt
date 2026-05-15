package com.finadapt.adaptivefinance.feature.chat

import android.content.SharedPreferences
import com.finadapt.adaptivefinance.data.local.ExpenseDao
import com.finadapt.adaptivefinance.data.remote.AiIntentResponse
import com.finadapt.adaptivefinance.data.remote.toCurrency
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.abs

class ChatIntentResolver(
    private val expenseDao: ExpenseDao,
    private val prefs: SharedPreferences
) {
    //Randomizer for natural phrasing
    private fun pickRandom(vararg phrases: String): String = phrases.random()

    //Contextual Emojis based on spend severity
    private fun getSpendEmoji(amount: Float, timeframe: String): String {
        val threshold = if (timeframe == "TODAY" || timeframe == "YESTERDAY") 100f else 500f
        return when {
            amount == 0f -> pickRandom("\uD83C\uDF89\uD83C\uDF43", "⚔\uFE0F", "\uD83D\uDE4C\uD83C\uDFFE")
            amount > threshold -> pickRandom("\uD83D\uDCB0", "\uFE0F\uFE0F\uFE0F", "\uD83D\uDCB9")
            else -> pickRandom("\uD83E\uDEE7\uD83D\uDC97", "\uFE0F", "⋆\uFE0E ˖")
        }
    }

    //Conversational Follow-ups
    private fun getFollowUp(intent: String): String {
        return when (intent) {
            "SPEND_SUMMARY" -> pickRandom("Want me to dig into a specific category?", "Shall we check how much budget you have left?")
            "SPEND_ANALYSIS" -> pickRandom("Surprised by any of those?", "We can set a limit on that top category if you want.")
            "BUDGET_STATUS" -> pickRandom("Want to see a list of what you bought recently?", "Any specific purchases you want me to search for?")
            else -> pickRandom("What else is on your mind?", "Anything else you want to check?")
        }
    }

    suspend fun execute(command: AiIntentResponse): String {
        //small delay makes the bot feel thoughtful
        delay(600)

        val intent = command.intent.uppercase()
        val category = command.category
        val timeframe = command.timeframe.uppercase()

        var endTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val startTime: Long = when (timeframe) {
            "TODAY" -> calendar.timeInMillis
            "YESTERDAY" -> { endTime = calendar.timeInMillis - 1L; calendar.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis }
            "WEEK" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            "MONTH" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            "ALL" -> 0L
            else -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        }

        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val timeLabel = when (timeframe) {
            "TODAY" -> "today"
            "YESTERDAY" -> "yesterday (${dayFormat.format(calendar.timeInMillis)})"
            "WEEK" -> "this past week"
            "MONTH" -> "this past month"
            "ALL" -> "over all time"
            else -> "recently"
        }

        val responseText = when (intent) {
            "SPEND_SUMMARY" -> {
                if (category == "ALL") {
                    val totalSpent = expenseDao.getTotalSpendTimeBounded(startTime, endTime) ?: 0f
                    val emoji = getSpendEmoji(totalSpent, timeframe)

                    if (totalSpent == 0f) {
                        pickRandom(
                            "Looks like you haven't spent a single ringgit $timeLabel. Impressive discipline! $emoji",
                            "Your ledger is completely blank for $timeLabel. Great job saving! $emoji"
                        )
                    } else {
                        //solid emoji bullets instead of spaces for perfect mobile alignment
                        val breakdownText = expenseDao.getCategoryBreakdown(startTime, endTime).joinToString("\n") {
                            "🔹 ${it.category}: ${it.total.toCurrency()}"
                        }

                        val intro = pickRandom(
                            "Let me check the books... $timeLabel, your total outflow was ${totalSpent.toCurrency()}. $emoji",
                            "Pulling those numbers now. You've spent ${totalSpent.toCurrency()} in total $timeLabel. $emoji"
                        )

                        //guarantee it won't break on different screen sizes
                        intro + "\n\n" +
                                "\uD83D\uDCD2 Category Breakdown:\n" +
                                breakdownText
                    }
                } else {
                    val totalSpent = expenseDao.getTotalSpendByCategoryAndTime(category, startTime, endTime) ?: 0f
                    val emoji = getSpendEmoji(totalSpent, timeframe)
                    if (totalSpent == 0f) {
                        "Your $category spending is at absolute zero $timeLabel. $emoji"
                    } else {
                        pickRandom(
                            "You dropped ${totalSpent.toCurrency()} on $category $timeLabel. $emoji",
                            "$timeLabel, your $category expenses totaled ${totalSpent.toCurrency()}. $emoji"
                        )
                    }
                }
            }
            "SPEND_ANALYSIS" -> {
                val breakdown = expenseDao.getCategoryBreakdown(startTime, endTime)
                val highest = expenseDao.getHighestExpense(startTime, endTime)

                if (breakdown.isEmpty() || highest == null) {
                    pickRandom(
                        "My scanners are empty. Track some expenses $timeLabel first!",
                        "I don't have enough data for $timeLabel to give you a solid analysis yet."
                    )
                } else {
                    val topCategory = breakdown.maxByOrNull { it.total }
                    val singleMerchant = highest.merchantName.ifEmpty { highest.category }

                    //Solid emoji bullets for alignment
                    val top3 = breakdown.sortedByDescending { it.total }.take(3).joinToString("\n") {
                        "🔹 ${it.category}: ${it.total.toCurrency()}"
                    }

                    val intro = pickRandom(
                        "Crunching the numbers for $timeLabel...",
                        "Here's what your spending habits look like $timeLabel:"
                    )

                    //Clean stacking with absolute line breaks
                    intro + "\n\n" +
                            "\uD83D\uDE2B Biggest Drain:\n" +
                            "   ${topCategory?.category} (${(topCategory?.total ?: 0f).toCurrency()})\n\n" +
                            "\uD83D\uDCB3 Heaviest Hit:\n" +
                            "   ${highest.amount.toCurrency()} at $singleMerchant\n\n" +
                            "\uD83D\uDD1D Top Spending Zones:\n" +
                            top3
                }
            }
            "BUDGET_STATUS" -> {
                val budget = prefs.getFloat("MONTHLY_BUDGET", 1000f)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
                val totalSpent = expenseDao.getTotalSpendTimeBounded(thirtyDaysAgo, System.currentTimeMillis()) ?: 0f
                val remaining = budget - totalSpent

                if (remaining < 0) {
                    pickRandom(
                        "Checking your vault status... Warning! You are currently ${abs(remaining).toCurrency()} OVER your monthly budget! 🛑",
                        "Time to dial back the spending! You're in the red by ${abs(remaining).toCurrency()}. ⚠️"
                    )
                } else {
                    pickRandom(
                        "Checking your vault status... You have ${remaining.toCurrency()} left in your budget for the next 30 days. You're doing great! 🟢",
                        "Looking good! You still have ${remaining.toCurrency()} of runway left. ✔"
                    )
                }
            }
            "LIST_TRANSACTIONS" -> {
                val recentItems = if (category == "ALL") expenseDao.getExpensesSince(startTime, endTime)
                else expenseDao.getExpensesByCategoryAndTimeList(category, startTime, endTime)
                val categoryText = if (category == "ALL") "" else " $category"

                if (recentItems.isEmpty()) {
                    "Your ledger is empty for$categoryText $timeLabel."
                } else {
                    val formattedList = recentItems.take(10).joinToString("\n") {
                        "🔹 ${it.amount.toCurrency()} at ${it.merchantName.ifEmpty { "Unknown" }}"
                    }
                    val intro = pickRandom(
                        "Here are your$categoryText purchases from $timeLabel:\n",
                        "I pulled your latest$categoryText transactions for $timeLabel:\n"
                    )
                    "$intro\n$formattedList"
                }
            }
            "SEARCH_TRANSACTIONS" -> {
                val searchTerm = command.searchTerm
                if (searchTerm.isBlank()) {
                    return "I'm ready to search the ledger! What specific merchant or item should I look for?"
                }

                val searchResults = expenseDao.searchExpenses(searchTerm, startTime, endTime)
                if (searchResults.isEmpty()) {
                    "Nothing came up for '$searchTerm' $timeLabel. Are you sure you logged it?"
                } else {
                    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val formattedList = searchResults.take(5).joinToString("\n") {
                        "🔹 ${it.amount.toCurrency()} at ${it.merchantName.ifEmpty { it.category }} (Logged at ${timeFormatter.format(java.util.Date(it.timestamp))})"
                    }
                    "Here is what I found for '$searchTerm' $timeLabel:\n\n$formattedList"
                }
            }
            "GENERAL_CHAT" -> {
                //If the AI actually provided a reply, use it
                command.directReply.ifBlank {
                    //Fallback: A natural conversation filler instead of an error
                    pickRandom(
                        "I hear you! What else about your budget is on your mind?",
                        "That makes sense. Want me to pull up any specific transactions?",
                        "Got it! Let me know if you want to dig into a specific category."
                    )
                }
            }
            else -> ""
        }
        return if (responseText.isNotBlank()) "$responseText\n\n💡 ${getFollowUp(intent)}" else ""
    }
}