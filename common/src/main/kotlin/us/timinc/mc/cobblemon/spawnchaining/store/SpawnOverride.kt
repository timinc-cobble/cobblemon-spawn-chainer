package us.timinc.mc.cobblemon.spawnchaining.store

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import java.util.*

object SpawnOverride {
    private val spawnOverrides: MutableMap<UUID, Entry> = mutableMapOf()

    data class Entry(
        val properties: PokemonProperties,
        val context: ResourceLocation,
        val levelMod: Int,
    )

    @Suppress("MemberVisibilityCanBePrivate")
    fun record(player: ServerPlayer, properties: PokemonProperties, context: ResourceLocation, levelMod: Int) {
        spawnOverrides[player.uuid] = Entry(properties, context, levelMod)
    }

    fun find(player: ServerPlayer, context: ResourceLocation): Entry? {
        val uuid = player.uuid
        val found = spawnOverrides[uuid] ?: return null
        if (found.context != context) return null

        return found
    }

    fun remove(player: ServerPlayer) {
        spawnOverrides.remove(player.uuid)
    }
}