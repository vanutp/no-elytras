package dev.vanutp.no_elytras

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin


class NoElytras : JavaPlugin(), Listener {
    companion object {
        val FORBIDDEN_TEXT = Component.text("Flying is not allowed here")
            .color(NamedTextColor.BLUE)
    }
    private var blacklistWorlds = listOf<String>()

    private fun reload() {
        saveDefaultConfig()
        reloadConfig()
        blacklistWorlds = config.getStringList("blacklist_worlds")
    }

    @Suppress("UnstableApiUsage")
    private fun registerCommand() {
        val commandTree = Commands.literal("no-elytras")
            .requires { sender -> sender.sender.isOp }
            .then(
                Commands.literal("reload")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        reload()
                        ctx.source.sender.sendPlainMessage("Config reloaded")
                        Command.SINGLE_SUCCESS
                    })
            .build()
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(commandTree, "NoElytras command")
        }
    }

    override fun onEnable() {
        reload()
        server.pluginManager.registerEvents(this, this)
        registerCommand()
    }

    @EventHandler
    fun onElytra(e: EntityToggleGlideEvent) {
        val player = e.entity as? Player ?: return
        if (blacklistWorlds.contains(player.world.name)) {
            e.isCancelled = true
            player.sendActionBar(FORBIDDEN_TEXT)
        }
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (e.player.isGliding && blacklistWorlds.contains(e.player.world.name)) {
            e.player.isGliding = false
            e.player.sendActionBar(FORBIDDEN_TEXT)
        }
    }

    @EventHandler
    fun onBoost(e: PlayerElytraBoostEvent) {
        if (blacklistWorlds.contains(e.player.world.name)) {
            e.isCancelled = true
            e.player.sendActionBar(FORBIDDEN_TEXT)
        }
    }
}
