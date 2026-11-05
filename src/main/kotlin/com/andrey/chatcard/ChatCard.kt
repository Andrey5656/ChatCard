package com.andrey.chatcard

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

class ChatCard : JavaPlugin() {
    @Volatile
    lateinit var settings: Settings
    lateinit var cards: Cards
    lateinit var toggles: Toggles
    lateinit var chat: ChatListener

    @Volatile
    var muted = false

    @Volatile
    var slow = 0.0

    override fun onEnable() {
        saveDefaultConfig()
        if (!File(dataFolder, "messages.yml").exists()) saveResource("messages.yml", false)
        load()
        if (!server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            logger.warning("PlaceholderAPI is not installed, so placeholders like %vault_prefix% show as plain text. Install PlaceholderAPI and the Player, Vault and Statistic expansions.")
        }
        cards = Cards(this)
        toggles = Toggles(this)
        chat = ChatListener(this)
        server.pluginManager.registerEvents(chat, this)
        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.onlinePlayers.forEach { player -> player.scheduler.run(this, { _ -> toggles.load(player) }, null) }

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register("chatcard", "ChatCard staff commands.", listOf("chat"), StaffCommand(this))
            commands.register("togglechat", "Hide or show chat.", emptyList(), ToggleCommand(this, mentions = false))
            commands.register("togglementions", "Turn mentions on or off.", emptyList(), ToggleCommand(this, mentions = true))
        }
    }

    fun load() {
        reloadConfig()
        val messages = YamlConfiguration.loadConfiguration(File(dataFolder, "messages.yml"))
        getResource("messages.yml")?.use { messages.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8))) }
        settings = Settings(config, messages)
        settings.unknownSound?.let { logger.warning("Unknown sound in mentions.sound: $it") }
        settings.formats
            .map { it.permission }
            .filter { it.isNotEmpty() && server.pluginManager.getPermission(it) == null }
            .forEach { server.pluginManager.addPermission(Permission(it, PermissionDefault.FALSE)) }
    }

    companion object {
        const val RELOAD = "chatcard.reload"
        const val MUTE = "chatcard.mute"
        const val CLEAR = "chatcard.clear"
        const val SLOW = "chatcard.slow"
        const val DELETE = "chatcard.delete"
        const val BYPASS_MUTE = "chatcard.bypass.mute"
        const val BYPASS_COOLDOWN = "chatcard.bypass.cooldown"
        const val BYPASS_FILTER = "chatcard.bypass.filter"
        const val BYPASS_CLEAR = "chatcard.bypass.clear"
        const val LINKS = "chatcard.links"
        const val CHAT_COLOR = "chatcard.chat.color"
        const val CHAT_HEX = "chatcard.chat.hex"
        const val CHAT_GRADIENT = "chatcard.chat.gradient"
        const val CHAT_FORMAT = "chatcard.chat.format"
        const val EMOJIS = "chatcard.emojis"
        const val TOGGLE_CHAT = "chatcard.togglechat"
        const val TOGGLE_MENTIONS = "chatcard.togglementions"
    }
}
