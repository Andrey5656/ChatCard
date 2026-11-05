package com.andrey.chatcard

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Toggles(plugin: ChatCard) {
    private val chatKey = NamespacedKey(plugin, "chat_hidden")
    private val mentionsKey = NamespacedKey(plugin, "mentions_off")
    private val chatHidden = ConcurrentHashMap.newKeySet<UUID>()
    private val mentionsOff = ConcurrentHashMap.newKeySet<UUID>()

    fun load(player: Player) {
        val data = player.persistentDataContainer
        if (data.has(chatKey, PersistentDataType.BYTE)) chatHidden.add(player.uniqueId)
        if (data.has(mentionsKey, PersistentDataType.BYTE)) mentionsOff.add(player.uniqueId)
    }

    fun unload(player: Player) {
        chatHidden.remove(player.uniqueId)
        mentionsOff.remove(player.uniqueId)
    }

    fun chatHidden(player: Player): Boolean = player.uniqueId in chatHidden

    fun mentionsOff(player: Player): Boolean = player.uniqueId in mentionsOff

    fun toggleChat(player: Player): Boolean = toggle(player, chatKey, chatHidden)

    fun toggleMentions(player: Player): Boolean = toggle(player, mentionsKey, mentionsOff)

    private fun toggle(player: Player, key: NamespacedKey, set: MutableSet<UUID>): Boolean {
        val on = set.add(player.uniqueId)
        if (on) {
            player.persistentDataContainer.set(key, PersistentDataType.BYTE, 1)
        } else {
            set.remove(player.uniqueId)
            player.persistentDataContainer.remove(key)
        }
        return on
    }
}
