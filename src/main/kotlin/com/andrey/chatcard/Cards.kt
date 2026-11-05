package com.andrey.chatcard

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Cards(private val plugin: ChatCard) {
    fun line(player: Player, raw: String): Component {
        val line = Text.parse(Text.papi(player, raw))
        if (!raw.contains("{player}")) return line
        val name = Component.textOfChildren(name(player))
        return line.replaceText { it.matchLiteral("{player}").replacement(name) }
    }

    private fun name(player: Player): Component {
        val settings = plugin.settings
        var name: Component = Component.text(player.name)
        if (settings.hoverLines.isNotEmpty()) {
            val card = Component.join(JoinConfiguration.newlines(), settings.hoverLines.map { Text.parse(Text.papi(player, it)) })
            name = name.hoverEvent(HoverEvent.showText(card))
        }
        if (settings.click.isNotEmpty()) {
            name = name.clickEvent(ClickEvent.runCommand(Text.papi(player, settings.click)))
        }
        return name
    }

    fun <T : Any> onPlayerThread(player: Player, task: () -> T): T? {
        if (Bukkit.isOwnedByCurrentRegion(player)) return task()
        val future = CompletableFuture<T>()
        player.scheduler.run(plugin, { _ ->
            try {
                future.complete(task())
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }, { future.cancel(false) }) ?: return null
        return try {
            future.get(5, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } catch (e: TimeoutException) {
            null
        } catch (e: CancellationException) {
            null
        }
    }
}
