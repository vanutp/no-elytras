package dev.vanutp.no_elytras

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level


class NoElytras : JavaPlugin(), Listener {
    private val mm = MiniMessage.miniMessage()
    private var forbiddenText: Component = Component.text("")
    private var blacklistWorlds = listOf<String>()
    private var logUsages = false

    private fun reload() {
        saveDefaultConfig()
        reloadConfig()
        blacklistWorlds = config.getStringList("blacklist_worlds")
        forbiddenText = mm.deserialize(config.getString("forbidden_message") ?: "")
        logUsages = config.getBoolean("log_usages")
    }

    @Suppress("UnstableApiUsage")
    private fun registerCommand() {
        val commandTree = Commands.literal("no-elytras")
            .requires { sender -> sender.sender.isOp }
            .then(
                Commands.literal("reload")
                    .executes { ctx: CommandContext<CommandSourceStack> ->
                        try {
                            reload()
                            ctx.source.sender.sendPlainMessage("Config reloaded")
                        } catch (e: Exception) {
                            ctx.source.sender.sendPlainMessage("Config reload failed, see server console")
                            logger.log(Level.SEVERE, e.message.toString(), e)
                        }
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

    private fun onUsageAttempt(player: Player) {
        player.sendActionBar(forbiddenText)
        if (logUsages) {
            logger.info("Player `${player.name}` attempted to use elytra in `${player.world.name}`")
        }
    }

    @EventHandler
    fun onElytra(e: EntityToggleGlideEvent) {
        val player = e.entity as? Player ?: return
        if (e.isGliding && blacklistWorlds.contains(player.world.name)) {
            e.isCancelled = true
            onUsageAttempt(player)
        }
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (e.player.isGliding && blacklistWorlds.contains(e.player.world.name)) {
            e.player.isGliding = false
            onUsageAttempt(e.player)
        }
    }

    @EventHandler
    fun onBoost(e: PlayerElytraBoostEvent) {
        if (blacklistWorlds.contains(e.player.world.name)) {
            e.isCancelled = true
            onUsageAttempt(e.player)
        }
    }
}
