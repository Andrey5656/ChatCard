package com.andrey.chatcard

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.max

class ChatListener(private val plugin: ChatCard) : Listener {
    private class Sent(val time: Long, val text: String)

    private val word = Regex("[A-Za-z0-9_]+")
    private val last = ConcurrentHashMap<UUID, Sent>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val settings = plugin.settings
        var raw = Text.plain(event.message())
        val previous = last[player.uniqueId]
        val now = System.currentTimeMillis()

        if (plugin.muted && !player.hasPermission(ChatCard.BYPASS_MUTE)) return deny(event, player, settings.message("muted"))
        val wait = max(settings.cooldown, plugin.slow)
        if (previous != null && wait > 0 && !player.hasPermission(ChatCard.BYPASS_COOLDOWN)) {
            val left = wait - (now - previous.time) / 1000.0
            if (left > 0) return deny(event, player, settings.message("cooldown"), "seconds" to Text.seconds(left))
        }
        val links = player.hasPermission(ChatCard.LINKS)
        if (!links && Filter.link.containsMatchIn(raw)) return deny(event, player, settings.message("links"))

        val filter = !player.hasPermission(ChatCard.BYPASS_FILTER)
        if (filter) {
            if (Filter.tooManyCaps(raw, settings.capsMinLetters, settings.capsMaxPercent)) raw = raw.lowercase()
            settings.words?.let { words -> raw = words.replace(raw) { "*".repeat(it.value.length) } }
        }
        var message = Text.chat(player, raw)
        if (links) {
            message = message.replaceText {
                it.match(Filter.linkPattern).replacement { match, _ ->
                    Component.textOfChildren(Component.text(match.group()).clickEvent(ClickEvent.openUrl(Filter.url(match.group()))))
                }
            }
        }
        val emojis = settings.emojiPattern
        if (emojis != null && player.hasPermission(ChatCard.EMOJIS)) {
            message = message.replaceText { it.match(emojis).replacement { match, _ -> Component.text(settings.emojis.getValue(match.group())) } }
        }
        if (filter && previous != null && settings.duplicateSeconds > 0 && now - previous.time < settings.duplicateSeconds * 1000 &&
            previous.text == Filter.normalize(Text.plain(message))
        ) {
            return deny(event, player, settings.message("duplicate"))
        }
        event.message(message)
        event.viewers().removeIf { it is Player && it != player && plugin.toggles.chatHidden(it) }

        val format = settings.formats.firstOrNull { it.permission.isEmpty() || player.hasPermission(it.permission) } ?: return
        val template = plugin.cards.onPlayerThread(player) { plugin.cards.line(player, format.format) }
        if (template == null) {
            event.isCancelled = true
            return
        }

        val mentioned = if (settings.highlight.isEmpty()) emptySet() else mentioned(event, raw)
        val signed = event.signedMessage()
        val delete = if (signed.canDelete() && settings.deleteButton.isNotEmpty()) deleteButton(signed) else null
        event.renderer { _, _, body, viewer ->
            var text = body
            if (viewer is Player && viewer.uniqueId in mentioned) text = highlight(text, viewer.name, settings.highlight)
            val line = template.replaceText { it.matchLiteral("{message}").once().replacement(Component.textOfChildren(text)) }
            if (delete != null && viewer is Player && viewer.hasPermission(ChatCard.DELETE)) Component.textOfChildren(delete, line) else line
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onSent(event: AsyncChatEvent) {
        val player = event.player
        val raw = Text.plain(event.message())
        last[player.uniqueId] = Sent(System.currentTimeMillis(), Filter.normalize(raw))
        val settings = plugin.settings
        if (settings.sound.isEmpty() && settings.actionbar.isEmpty()) return
        val actionbar = if (settings.actionbar.isEmpty()) null else Text.parse(settings.actionbar.replace("{player}", player.name))
        val mentioned = mentioned(event, raw)
        for (target in event.viewers()) {
            if (target !is Player || target.uniqueId !in mentioned) continue
            target.scheduler.run(plugin, { _ ->
                if (settings.sound.isNotEmpty()) target.playSound(target, settings.sound, settings.volume, settings.pitch)
                if (actionbar != null) target.sendActionBar(actionbar)
            }, null)
        }
    }

    fun forget(player: Player) {
        last.remove(player.uniqueId)
    }

    private fun mentioned(event: AsyncChatEvent, raw: String): Set<UUID> {
        val words = word.findAll(raw).map { it.value.lowercase() }.toSet()
        return event.viewers().asSequence()
            .filterIsInstance<Player>()
            .filter { it != event.player && it.name.lowercase() in words && !plugin.toggles.mentionsOff(it) }
            .map { it.uniqueId }
            .toSet()
    }

    private fun highlight(message: Component, name: String, format: String): Component {
        val pattern = Pattern.compile("(?<![A-Za-z0-9_])@?" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE)
        val replacement = Component.textOfChildren(Text.parse(format.replace("{name}", name)))
        return message.replaceText { it.match(pattern).replacement(replacement) }
    }

    private fun deleteButton(signed: SignedMessage): Component {
        val settings = plugin.settings
        val callback = ClickCallback<Audience> { clicker ->
            if (clicker is Player && clicker.hasPermission(ChatCard.DELETE)) {
                plugin.server.deleteMessage(signed)
                Text.send(clicker, plugin.settings.message("deleted"))
            }
        }
        var button = Text.parse(settings.deleteButton).clickEvent(ClickEvent.callback(callback) { it.lifetime(Duration.ofHours(1)) })
        if (settings.deleteHover.isNotEmpty()) button = button.hoverEvent(HoverEvent.showText(Text.parse(settings.deleteHover)))
        return button
    }

    private fun deny(event: AsyncChatEvent, player: Player, raw: String, vararg tokens: Pair<String, String>) {
        event.isCancelled = true
        Text.send(player, raw, *tokens)
    }
}
