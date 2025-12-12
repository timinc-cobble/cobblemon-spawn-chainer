package us.timinc.mc.cobblemon.spawnchaining.handler

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import us.timinc.mc.cobblemon.spawnchaining.SpawnChaining
import us.timinc.mc.cobblemon.spawnchaining.api.SpawnOverrideRecorder
import us.timinc.mc.cobblemon.timcore.AbstractHandler
import us.timinc.mc.cobblemon.timcore.getIdentifier

object CaptureChainer : AbstractHandler<PokemonCapturedEvent>(), SpawnOverrideRecorder {
    override fun handle(evt: PokemonCapturedEvent) {
        if (!SpawnChaining.config.onCapture) return

        val debugger = SpawnChaining.debugger.getCaseDebugger()

        val caughtPokemon = evt.pokemon
        val capturingPlayer = evt.player

        debugger.debug("${capturingPlayer.name.string} captured a ${caughtPokemon.getIdentifier()}")

        record(capturingPlayer, caughtPokemon, SpawnChaining.DataKeys.Triggers.CAPTURE, debugger)
    }
}