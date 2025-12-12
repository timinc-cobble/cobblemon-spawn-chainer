package us.timinc.mc.cobblemon.spawnchaining.influence

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import us.timinc.mc.cobblemon.counter.CounterMod
import us.timinc.mc.cobblemon.counter.api.CounterTypeRegistry
import us.timinc.mc.cobblemon.counter.extension.getCounterManager
import us.timinc.mc.cobblemon.spawnchaining.SpawnChaining
import us.timinc.mc.cobblemon.spawnchaining.store.SpawnOverride
import us.timinc.mc.cobblemon.timcore.Debugger
import us.timinc.mc.cobblemon.timcore.LimitedList
import us.timinc.mc.cobblemon.timcore.PokemonMatcher
import us.timinc.mc.cobblemon.timcore.PokemonRepresentation
import kotlin.random.Random.Default.nextFloat

class ReplacementInfluence(val context: ResourceLocation, val player: ServerPlayer? = null) : SpawningInfluence {
    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        if (action !is PokemonSpawnAction || entity !is PokemonEntity) return

        val debugger = SpawnChaining.debugger.getCaseDebugger()

        val overriddenPokemonRep = PokemonRepresentation.FromProperties(action.props)
        if (!LimitedList.PokemonMatcherList.matchesList(
                overriddenPokemonRep.getPokemon(),
                SpawnChaining.config.overrideWhitelist.map(PokemonMatcher::parse).toSet(),
                SpawnChaining.config.overrideBlacklist.map(PokemonMatcher::parse).toSet()
            )
        ) {
            debugger.debug("Invalid Pokemon to override, per the whitelist/blacklist.")
            return
        }

        val player = player ?: action.spawnablePosition.cause.entity as? ServerPlayer ?: return
        debugger.debug("Attempting a replacement influence for ${player.name.string} on the $context context.")

        val override = SpawnOverride.find(player, context)

        if (override == null) {
            debugger.debug("No override found.")
            return
        }
        debugger.debug("Found an override of ${override.properties.asString()} with a level mod of ${override.levelMod}.")

        val overrideChance =
            getOverrideChance(
                player,
                PokemonRepresentation.FromProperties(override.properties),
                context,
                override.trigger,
                debugger
            )
        debugger.debug("Has a chance of $overrideChance.")
        val overrideRoll = nextFloat()
        debugger.debug("Rolled a $overrideRoll.")
        if (overrideRoll > overrideChance) {
            debugger.debug("Failed the roll.")
            return
        }

        entity.pokemon = override.properties.create()
        debugger.debug("Switched entity's Pokemon to override's.")

        if (SpawnChaining.config.notifyPlayer) {
            player.sendSystemMessage(SpawnChaining.TranslationComponents.chained())
        }

        val breakOnSuccess = SpawnChaining.getContextConfig(override.context.path, override.trigger)?.breakOnSuccess
            ?: SpawnChaining.config.breakOnSuccess
        if (breakOnSuccess) {
            SpawnOverride.remove(player)
            debugger.debug("Broke the spawn override.")
        }

        SpawnChaining.CustomPokemonProperties.LEVEL_MOD.entityApplicator(entity, override.levelMod.toFloat())
        debugger.debug("Saved the level mod onto the Pokemon.")
    }

    private fun getOverrideChance(
        player: ServerPlayer,
        pokemonRep: PokemonRepresentation<PokemonProperties>,
        context: ResourceLocation,
        trigger: String,
        debugger: Debugger.Case<SpawnChaining.SpawnChainingConfig>?,
    ): Float {
        val manager = player.getCounterManager()

        var boost = 0F
        val points = SpawnChaining.getContextConfig(context.path, trigger)?.points ?: SpawnChaining.config.points
        for ((counterTypeName, scoreTypeList) in points) {
            for ((scoreTypeName, value) in scoreTypeList) {
                try {
                    val counterType = CounterTypeRegistry.findByType(counterTypeName)
                    val species = pokemonRep.species!!
                    val form = pokemonRep.form!!
                    val statScore = if (scoreTypeName == CounterMod.ScoreTypes.STREAK.type) manager.getStreakScore(
                        counterType,
                        species.resourceIdentifier,
                        form.name
                    ) else manager.getCountScore(counterType, species.resourceIdentifier, form.name)
                    boost += statScore * value
                } catch (e: Exception) {
                    e.message?.let { debugger?.debug(it) }
                }
            }
        }

        val initialChance =
            SpawnChaining.getContextConfig(context.path, trigger)?.initialChance ?: SpawnChaining.config.initialChance
        return initialChance + boost
    }
}