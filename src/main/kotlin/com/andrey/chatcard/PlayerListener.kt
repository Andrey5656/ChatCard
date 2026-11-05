package com.andrey.chatcard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(private val plugin: ChatCard) : Listener {
    private val blocked = setOf("me", "teammsg", "tm")

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val label = event.message.drop(1).substringBefore(' ').lowercase().removePrefix("minecraft:")
        if (label !in blocked) return
        event.isCancelled = true
        event.player.sendMessage(Component.translatable("command.unknown.command", NamedTextColor.RED))
    }

    @EventHandler
    fun onCommandList(event: PlayerCommandSendEvent) {
        event.commands.removeIf { it.lowercase().removePrefix("minecraft:") in blocked }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.toggles.load(player)
        val raw = if (player.hasPlayedBefore()) plugin.settings.join else plugin.settings.firstJoin
        event.joinMessage(if (raw.isEmpty()) null else plugin.cards.line(player, raw))
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val raw = plugin.settings.quit
        event.quitMessage(if (raw.isEmpty()) null else plugin.cards.line(player, raw))
        plugin.toggles.unload(player)
        plugin.chat.forget(player)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        val vanilla = event.deathMessage() ?: return
        val raw = plugin.settings.death
        event.deathMessage(
            if (raw.isEmpty()) null
            else plugin.cards.line(event.entity, raw).replaceText { it.matchLiteral("{message}").once().replacement(Component.textOfChildren(vanilla)) }
        )
    }
}
