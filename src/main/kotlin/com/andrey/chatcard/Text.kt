package com.andrey.chatcard

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

object Text {
    private val legacy = Regex("[&§](#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])")
    private val names = mapOf(
        '0' to "black", '1' to "dark_blue", '2' to "dark_green", '3' to "dark_aqua",
        '4' to "dark_red", '5' to "dark_purple", '6' to "gold", '7' to "gray",
        '8' to "dark_gray", '9' to "blue", 'a' to "green", 'b' to "aqua",
        'c' to "red", 'd' to "light_purple", 'e' to "yellow", 'f' to "white",
        'k' to "obfuscated", 'l' to "bold", 'm' to "strikethrough", 'n' to "underlined", 'o' to "italic"
    )

    private val hexTag = object : TagResolver {
        override fun has(name: String): Boolean =
            name.length == 7 && name[0] == '#' && name.drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

        override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context): Tag? =
            if (has(name)) Tag.styling(TextColor.fromHexString(name)!!) else null
    }
    private val colorTags = TagResolver.resolver(NamedTextColor.NAMES.keys().map { TagResolver.resolver(it, Tag.styling(NamedTextColor.NAMES.value(it)!!)) })
    private val restricted = ConcurrentHashMap<Int, MiniMessage>()

    fun parse(raw: String): Component = MiniMessage.miniMessage().deserialize(convert(raw, color = true, hex = true, format = true))

    fun chat(player: Player, raw: String): Component {
        val color = player.hasPermission(ChatCard.CHAT_COLOR)
        val hex = player.hasPermission(ChatCard.CHAT_HEX)
        val gradient = player.hasPermission(ChatCard.CHAT_GRADIENT)
        val format = player.hasPermission(ChatCard.CHAT_FORMAT)
        val key = (if (color) 1 else 0) or (if (hex) 2 else 0) or (if (gradient) 4 else 0) or (if (format) 8 else 0)
        if (key == 0) return Component.text(raw)
        val mini = restricted.computeIfAbsent(key) {
            val tags = buildList {
                add(StandardTags.reset())
                if (color) add(colorTags)
                if (hex) add(hexTag)
                if (gradient) {
                    add(StandardTags.gradient())
                    add(StandardTags.rainbow())
                }
                if (format) add(StandardTags.decorations())
            }
            MiniMessage.builder().tags(TagResolver.resolver(tags)).build()
        }
        return mini.deserialize(convert(raw, color, hex, format))
    }

    private fun convert(raw: String, color: Boolean, hex: Boolean, format: Boolean): String = legacy.replace(raw) { match ->
        val code = match.groupValues[1].lowercase()
        when {
            code.startsWith("#") -> if (hex) "<reset><$code>" else match.value
            code == "r" -> if (color || hex || format) "<reset>" else match.value
            code[0] in 'k'..'o' -> if (format) "<${names[code[0]]}>" else match.value
            else -> if (color) "<reset><${names[code[0]]}>" else match.value
        }
    }

    fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)

    fun papi(player: Player, raw: String): String {
        if (raw.indexOf('%') < 0 || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return raw
        return PlaceholderAPI.setPlaceholders(player, raw)
    }

    fun seconds(value: Double): String {
        val rounded = ceil(value * 10) / 10
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(Locale.ROOT, "%.1f", rounded)
    }

    fun send(target: Audience, raw: String, vararg tokens: Pair<String, String>) {
        if (raw.isEmpty()) return
        target.sendMessage(parse(tokens.fold(raw) { text, (key, value) -> text.replace("{$key}", value) }))
    }
}
