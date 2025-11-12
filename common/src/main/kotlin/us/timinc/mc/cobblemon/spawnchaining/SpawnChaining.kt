package us.timinc.mc.cobblemon.spawnchaining

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.spawning.BestSpawner.fishingSpawner
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.platform.events.PlatformEvents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import us.timinc.mc.cobblemon.spawnchaining.handler.CaptureChainer
import us.timinc.mc.cobblemon.spawnchaining.handler.KoChainer
import us.timinc.mc.cobblemon.spawnchaining.influence.ReplacementInfluence
import us.timinc.mc.cobblemon.timcore.*

const val MOD_ID: String = "spawn_chaining"

object SpawnChaining : AbstractMod<SpawnChaining.SpawnChainingConfig>(MOD_ID, SpawnChainingConfig::class.java) {
    class SpawnChainingConfig : AbstractConfig() {
        val onCapture: Boolean = true
        val onKo: Boolean = true
        val initialChance: Float = 1.0F
        val points: Map<String, Map<String, Float>> = mapOf()
        val notifyPlayer: Boolean = true
        val breakOnSuccess: Boolean = true
        val chainingBlacklist = mutableSetOf(
            "labels=legendary,mythical,ultra_beast,paradox any_label"
        )
        val chainingWhitelist = mutableSetOf<String>()
        val overrideBlacklist = mutableSetOf(
            "labels=legendary,mythical,ultra_beast,paradox,pseudo_legendary any_label"
        )
        val overrideWhitelist = mutableSetOf<String>()
        val assumePlayerSpawnered: Boolean = true
        val levelModRange: IntRange = -5..5
        val levelModMaxRange: IntRange = -15..15
    }

    object TranslationComponents {
        fun chained(): MutableComponent = Component.translatable("spawn_chaining.feedback.chained")
    }

    object DataKeys {
        const val LEVEL_MOD = "spawn_chaining:level_mod"
    }

    object CustomPokemonProperties {
        val LEVEL_MOD = CustomFloatProperty(DataKeys.LEVEL_MOD)
    }

    init {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.LOWEST, CaptureChainer::handle)
        TimCoreEvents.BATTLE_FAINTED_PVW_WILD.subscribe(Priority.LOWEST, KoChainer::handle)
        PlayerSpawnerFactory.influenceBuilders.add { ReplacementInfluence(TimCore.DataKeys.SpawnCauses.PLAYER_SPAWNER) }
        PlatformEvents.SERVER_STARTED.subscribe(Priority.LOWEST) {
            fishingSpawner.influences.add(ReplacementInfluence(TimCore.DataKeys.SpawnCauses.FISHING))
        }
    }
}