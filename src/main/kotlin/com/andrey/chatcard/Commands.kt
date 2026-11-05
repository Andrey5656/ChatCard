package com.andrey.chatcard

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StaffCommand(private val plugin: ChatCard) : BasicCommand {
    private val subcommands = linkedMapOf(
        "mute" to ChatCard.MUTE,
        "clear" to ChatCard.CLEAR,
        "slow" to ChatCard.SLOW,
        "reload" to ChatCard.RELOAD
    )

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val sender = source.sender
        val settings = plugin.settings
        val sub = args.firstOrNull()?.lowercase()
        val permission = subcommands[sub] ?: return Text.send(sender, settings.message("usage"))
        if (!sender.hasPermission(permission)) return Text.send(sender, settings.message("no-permission"))
        when (sub) {
            "mute" -> {
                plugin.muted = !plugin.muted
                Text.send(plugin.server, settings.message(if (plugin.muted) "mute-on" else "mute-off"), "player" to sender.name)
            }
            "clear" -> {
                val blank = Component.text("\n".repeat(settings.clearLines))
                plugin.server.onlinePlayers.filter { !it.hasPermission(ChatCard.BYPASS_CLEAR) }.forEach { it.sendMessage(blank) }
                Text.send(plugin.server, settings.message("cleared"), "player" to sender.name)
            }
            "slow" -> {
                val seconds = args.getOrNull(1)?.toDoubleOrNull()?.takeIf { it >= 0 } ?: return Text.send(sender, settings.message("slow-usage"))
                plugin.slow = seconds
                Text.send(plugin.server, settings.message(if (seconds > 0) "slow-on" else "slow-off"), "seconds" to Text.seconds(seconds))
            }
            "reload" -> {
                plugin.load()
                Text.send(sender, plugin.settings.message("reloaded"))
            }
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        if (args.size > 1) return emptyList()
        val typed = args.firstOrNull() ?: ""
        return subcommands.filter { (name, permission) -> source.sender.hasPermission(permission) && name.startsWith(typed, ignoreCase = true) }.keys
    }

    override fun canUse(sender: CommandSender): Boolean = subcommands.values.any { sender.hasPermission(it) }
}

class ToggleCommand(private val plugin: ChatCard, private val mentions: Boolean) : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val settings = plugin.settings
        val player = source.executor as? Player ?: source.sender as? Player ?: return Text.send(source.sender, settings.message("players-only"))
        val key = if (mentions) {
            if (plugin.toggles.toggleMentions(player)) "mentions-off" else "mentions-on"
        } else {
            if (plugin.toggles.toggleChat(player)) "chat-hidden" else "chat-shown"
        }
        Text.send(player, settings.message(key))
    }

    override fun permission(): String = if (mentions) ChatCard.TOGGLE_MENTIONS else ChatCard.TOGGLE_CHAT
}
