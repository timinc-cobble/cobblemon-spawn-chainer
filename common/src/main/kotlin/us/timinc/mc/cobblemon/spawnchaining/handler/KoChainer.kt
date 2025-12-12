package us.timinc.mc.cobblemon.spawnchaining.handler

import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.util.getPlayer
import us.timinc.mc.cobblemon.spawnchaining.SpawnChaining
import us.timinc.mc.cobblemon.spawnchaining.api.SpawnOverrideRecorder
import us.timinc.mc.cobblemon.timcore.AbstractHandler
import us.timinc.mc.cobblemon.timcore.getIdentifier

object KoChainer : AbstractHandler<BattleFaintedEvent>(), SpawnOverrideRecorder {
    override fun handle(evt: BattleFaintedEvent) {
        if (!SpawnChaining.config.onKo) return

        val debugger = SpawnChaining.debugger.getCaseDebugger()

        val targetPokemon = evt.killed.effectedPokemon
        val koingPlayer = evt.battle.playerUUIDs.first().getPlayer() ?: return

        debugger.debug("${koingPlayer.name.string} knocked out a ${targetPokemon.getIdentifier()}")

        record(koingPlayer, targetPokemon, SpawnChaining.DataKeys.Triggers.KO, debugger)
    }
}