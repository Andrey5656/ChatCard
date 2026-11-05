package com.andrey.chatcard

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import java.util.regex.Pattern

class Format(val permission: String, val format: String)

class Settings(config: FileConfiguration, private val messages: ConfigurationSection) {
    val formats: List<Format> = config.getConfigurationSection("chat.formats")?.let { section ->
        section.getKeys(false).mapNotNull { key ->
            section.getConfigurationSection(key)?.let { Format(it.getString("permission", "")!!, it.getString("format", "")!!) }
        }
    } ?: emptyList()
    val deleteButton = config.getString("chat.delete-button", "")!!
    val deleteHover = config.getString("chat.delete-hover", "")!!

    val hoverLines: List<String> = config.getStringList("hover.lines")
    val click = config.getString("hover.click", "")!!

    val join = config.getString("join.message", "")!!
    val firstJoin = config.getString("join.first-join", "")!!
    val quit = config.getString("quit.message", "")!!
    val death = config.getString("death.message", "")!!

    val highlight = config.getString("mentions.highlight", "")!!
    private val soundName = config.getString("mentions.sound", "")!!
    val sound: String = when {
        soundName.isEmpty() -> ""
        NamespacedKey.fromString(soundName) != null -> soundName
        else -> Registry.SOUND_EVENT.keyStream()
            .filter { it.key.replace('.', '_').equals(soundName, ignoreCase = true) }
            .findFirst()
            .map { it.asString() }
            .orElse("")
    }
    val unknownSound: String? = soundName.takeIf { it.isNotEmpty() && sound.isEmpty() }
    val volume = config.getDouble("mentions.volume", 1.0).toFloat()
    val pitch = config.getDouble("mentions.pitch", 1.0).toFloat()
    val actionbar = config.getString("mentions.actionbar", "")!!

    val cooldown = config.getDouble("filter.cooldown")
    val duplicateSeconds = config.getDouble("filter.duplicate-seconds")
    val capsMinLetters = config.getInt("filter.caps.min-letters")
    val capsMaxPercent = config.getInt("filter.caps.max-percent")
    val words: Regex? = config.getStringList("filter.words").map { it.trim() }.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let { list ->
        val alternatives = list.joinToString("|") { word -> word.map { Regex.escape(it.toString()) + "+" }.joinToString("") }
        Regex("(?<![\\p{L}\\p{N}])(?:$alternatives)(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    }

    val emojis: Map<String, String> = config.getConfigurationSection("emojis")?.let { section ->
        section.getKeys(false).associateWith { section.getString(it, "")!! }
    } ?: emptyMap()
    val emojiPattern: Pattern? = emojis.keys.takeIf { it.isNotEmpty() }
        ?.sortedByDescending { it.length }
        ?.joinToString("|") { Pattern.quote(it) }
        ?.let { Pattern.compile(it) }

    val clearLines = config.getInt("clear-lines", 100)

    fun message(key: String): String = messages.getString(key, "")!!
}
