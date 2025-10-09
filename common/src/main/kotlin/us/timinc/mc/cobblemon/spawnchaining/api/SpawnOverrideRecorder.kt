package us.timinc.mc.cobblemon.spawnchaining.api

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import us.timinc.mc.cobblemon.spawnchaining.SpawnChaining
import us.timinc.mc.cobblemon.spawnchaining.extension.constrain
import us.timinc.mc.cobblemon.spawnchaining.store.SpawnOverride
import us.timinc.mc.cobblemon.timcore.*

interface SpawnOverrideRecorder {
    fun record(
        player: ServerPlayer,
        pokemon: Pokemon,
        debugger: Debugger.Case<SpawnChaining.SpawnChainingConfig>,
    ): Boolean {
        if (!LimitedList.PokemonMatcherList.matchesList(
                pokemon,
                SpawnChaining.config.chainingWhitelist,
                SpawnChaining.config.chainingBlacklist
            )
        ) {
            debugger.debug("${pokemon.getIdentifier()} is prohibited by the blacklist/whitelist; not recording.")
            return false
        }

        var spawnCause = pokemon.getSpawnCause()?.let { ResourceLocation.parse(it) }
        if (spawnCause == null) {
            debugger.debug("No spawn cause recorded.")
            if (SpawnChaining.config.assumePlayerSpawnered) {
                debugger.debug("Assuming player spawnered.")
                spawnCause = TimCore.DataKeys.SpawnCauses.PLAYER_SPAWNER
            } else {
                debugger.debug("It hurt itself in its confusion (returning).")
                return false
            }
        }

        val props = PokemonProperties()
        val species = pokemon.species.resourceIdentifier.path
        props.species = species
        debugger.debug("Set species to $species.")
        val form = pokemon.form.name
        props.form = form
        debugger.debug("Set form to $form.")

        val previousLevelMod = (SpawnChaining.CustomPokemonProperties.LEVEL_MOD.getValue(pokemon)?.toInt() ?: 0)
        debugger.debug("Previous level mod was $previousLevelMod.")
        val levelModRoll = SpawnChaining.config.levelModRange.random()
        debugger.debug("Rolled a new level mod of $levelModRoll.")
        val levelMod = SpawnChaining.config.levelModMaxRange.constrain(levelModRoll + previousLevelMod)
        props.level = pokemon.level + levelMod - previousLevelMod
        debugger.debug("Updated props level to ${props.level}.")

        SpawnOverride.record(player, props, spawnCause, levelMod)
        debugger.debug("Recorded props against player.")

        return true
    }
}