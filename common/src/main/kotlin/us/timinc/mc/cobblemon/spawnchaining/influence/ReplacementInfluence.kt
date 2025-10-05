package us.timinc.mc.cobblemon.spawnchaining.influence

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import us.timinc.mc.cobblemon.counter.CounterMod
import us.timinc.mc.cobblemon.counter.api.CounterTypeRegistry
import us.timinc.mc.cobblemon.counter.extension.getCounterManager
import us.timinc.mc.cobblemon.spawnchaining.SpawnChaining
import us.timinc.mc.cobblemon.spawnchaining.store.SpawnOverride
import us.timinc.mc.cobblemon.timcore.Debugger
import us.timinc.mc.cobblemon.timcore.LimitedList
import us.timinc.mc.cobblemon.timcore.PokemonRepresentation
import kotlin.random.Random.Default.nextFloat

class ReplacementInfluence(val context: ResourceLocation, val player: ServerPlayer? = null) : SpawningInfluence {
    override fun affectAction(action: SpawnAction<*>) {
        if (action !is PokemonSpawnAction) return

        val debugger = SpawnChaining.debugger.getCaseDebugger()

        val pokemonRep = PokemonRepresentation.FromProperties(action.props)
        if (!LimitedList.PokemonMatcherList.matchesList(
                pokemonRep.getPokemon(),
                SpawnChaining.config.overrideWhitelist,
                SpawnChaining.config.overrideBlacklist
            )
        ) {
            debugger.debug("Invalid Pokemon to override, per the whitelist/blacklist.")
            return
        }

        val player = player ?: action.ctx.cause.entity as? ServerPlayer ?: return
        debugger.debug("Attempting a replacement influence for ${player.name.string} on the $context context.")

        val override = SpawnOverride.find(player, context)

        if (override == null) {
            debugger.debug("No override found.")
            return
        }
        debugger.debug("Found an override of ${override.properties.originalString} with a level mod of ${override.levelMod}.")

        val overrideChance = getOverrideChance(player, debugger, pokemonRep)
        debugger.debug("Has a chance of $overrideChance.")
        val overrideRoll = nextFloat()
        debugger.debug("Rolled a $overrideRoll.")
        if (overrideRoll > overrideChance) {
            debugger.debug("Failed the roll.")
            return
        }

        action.props = override.properties
        debugger.debug("Switched action's props to override's.")

        if (SpawnChaining.config.notifyPlayer) {
            player.sendSystemMessage(SpawnChaining.TranslationComponents.chained())
        }

        if (SpawnChaining.config.breakOnSuccess) {
            SpawnOverride.remove(player)
            debugger.debug("Broke the spawn override.")
        }

        action.entity.subscribe {
            SpawnChaining.CustomPokemonProperties.LEVEL_MOD.entityApplicator(
                it, override.levelMod.toFloat()
            )
            debugger.debug("Saved the level mod onto the Pokemon.")
        }
    }

    private fun getOverrideChance(
        player: ServerPlayer,
        debugger: Debugger.Case<SpawnChaining.SpawnChainingConfig>?,
        pokemonRep: PokemonRepresentation<PokemonProperties>,
    ): Float {
        val manager = player.getCounterManager()

        var boost = 0F
        for ((counterTypeName, scoreTypeList) in SpawnChaining.config.points) {
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

        return SpawnChaining.config.initialChance + boost
    }
}