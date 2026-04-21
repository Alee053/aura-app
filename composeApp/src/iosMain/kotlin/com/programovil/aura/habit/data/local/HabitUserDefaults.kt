package com.programovil.aura.habit.data.local

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class HabitUserDefaults {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val habitsKey = "habits_data"
    private val completionsKey = "completions_data"

    private val _habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val _completionsFlow = MutableStateFlow<List<HabitCompletion>>(emptyList())

    init {
        refreshFlows()
    }

    private fun refreshFlows() {
        _habitsFlow.value = getHabits()
        _completionsFlow.value = getCompletions()
    }

    fun getHabits(): List<Habit> {
        val data = defaults.dataForKey(habitsKey)
        if (data == null) return emptyList()
        return try {
            decodeHabits(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHabit(habit: Habit) {
        val habits = getHabits().toMutableList()
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) habits[index] = habit else habits.add(habit)
        defaults.setObject(encodeHabits(habits), forKey = habitsKey)
        refreshFlows()
    }

    fun deleteHabit(habitId: String) {
        val habits = getHabits().filter { it.id != habitId }
        defaults.setObject(encodeHabits(habits), forKey = habitsKey)
        val completions = getCompletions().filter { it.habitId != habitId }
        defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
        refreshFlows()
    }

    fun getCompletions(): List<HabitCompletion> {
        val data = defaults.dataForKey(completionsKey)
        if (data == null) return emptyList()
        return try {
            decodeCompletions(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCompletionsForHabit(habitId: String): List<HabitCompletion> {
        return getCompletions().filter { it.habitId == habitId }
    }

    fun toggleCompletion(habitId: String, date: String): Boolean {
        val completions = getCompletions().toMutableList()
        val existing = completions.find { it.habitId == habitId && it.completedDate == date }
        if (existing != null) {
            completions.removeAll { it.habitId == habitId && it.completedDate == date }
            defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
            refreshFlows()
            return false
        } else {
            val completion = HabitCompletion(
                id = java.util.UUID.randomUUID().toString(),
                habitId = habitId,
                completedDate = date,
                completedAt = System.currentTimeMillis()
            )
            completions.add(completion)
            defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
            refreshFlows()
            return true
        }
    }

    fun deleteOldCompletions(cutoffDate: String) {
        val completions = getCompletions().filter { it.completedDate >= cutoffDate }
        defaults.setObject(encodeCompletions(completions), forKey = completionsKey)
        refreshFlows()
    }

    fun habitsFlow(): Flow<List<Habit>> = _habitsFlow
    fun completionsFlow(): Flow<List<HabitCompletion>> = _completionsFlow

    private fun encodeHabits(habits: List<Habit>): NSData {
        val maps = habits.map { mapOf(
            "id" to it.id,
            "name" to it.name,
            "recurrenceType" to it.recurrenceType.name,
            "daysOfWeek" to it.daysOfWeek.joinToString(","),
            "color" to it.color,
            "createdAt" to it.createdAt
        ) }
        return NSJSONSerialization.dataWithJSONObject(maps, 0, null)
    }

    private fun decodeHabits(data: NSData): List<Habit> {
        val array = NSJSONSerialization.JSONObjectWithData(data, 0, null) as? NSArray ?: return emptyList()
        return (0 until array.count).mapNotNull { i ->
            val map = array.objectAtIndex(i) as? NSDictionary ?: return@mapNotNull null
            Habit(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = map["name"] as? String ?: return@mapNotNull null,
                recurrenceType = RecurrenceType.valueOf(map["recurrenceType"] as? String ?: return@mapNotNull null),
                daysOfWeek = (map["daysOfWeek"] as? String ?: "").split(",").filter { it.isNotBlank() }.map { it.toInt() },
                color = map["color"] as? String ?: return@mapNotNull null,
                createdAt = (map["createdAt"] as? NSNumber)?.longValue ?: 0L
            )
        }
    }

    private fun encodeCompletions(completions: List<HabitCompletion>): NSData {
        val maps = completions.map { mapOf(
            "id" to it.id,
            "habitId" to it.habitId,
            "completedDate" to it.completedDate,
            "completedAt" to it.completedAt
        ) }
        return NSJSONSerialization.dataWithJSONObject(maps, 0, null)
    }

    private fun decodeCompletions(data: NSData): List<HabitCompletion> {
        val array = NSJSONSerialization.JSONObjectWithData(data, 0, null) as? NSArray ?: return emptyList()
        return (0 until array.count).mapNotNull { i ->
            val map = array.objectAtIndex(i) as? NSDictionary ?: return@mapNotNull null
            HabitCompletion(
                id = map["id"] as? String ?: return@mapNotNull null,
                habitId = map["habitId"] as? String ?: return@mapNotNull null,
                completedDate = map["completedDate"] as? String ?: return@mapNotNull null,
                completedAt = (map["completedAt"] as? NSNumber)?.longValue ?: 0L
            )
        }
    }
}