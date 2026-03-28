package lv.mtm123.slashserver.proxies

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.event.Subscribe
import com.google.inject.Inject
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import lv.mtm123.slashserver.cmd.VelocityNavigationCommand
import lv.mtm123.slashserver.util.Config
import org.bstats.velocity.Metrics
import java.io.File

@Plugin(
    id = "slashserver",
    name = "SlashServer",
    version = "1.2.0-SNAPSHOT",
    description = "Allows to use /<servername>",
    authors = ["MTM123"]
)
class VelocitySlashServer @Inject constructor(
    private val proxy: ProxyServer,
    private val metricsFactory: Metrics.Factory
) {

    private lateinit var config: Config
    private lateinit var mainDir: File

    @Subscribe
    fun onInit(event: ProxyInitializeEvent) {

        mainDir = File("./plugins/SlashServer/")
        if (!mainDir.exists()) {
            mainDir.mkdirs()
        }

        config = Config.loadConfig(mainDir)
        metricsFactory.make(this, 12509)

        registerCommands()

        val adminMeta = proxy.commandManager.metaBuilder("slashserver").plugin(this).build()
        proxy.commandManager.register(adminMeta, SimpleCommand { inv ->
            if (inv.source().hasPermission("slashserver.admin") && inv.arguments().size == 1 && inv.arguments()[0].lowercase() == "reload") {
                Config.reloadConfig(mainDir, config)
                registerCommands()
                inv.source().sendMessage(net.kyori.adventure.text.Component.text("SlashServer config reloaded!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN))
            } else if (inv.source().hasPermission("slashserver.admin")) {
                inv.source().sendMessage(net.kyori.adventure.text.Component.text("Usage: /slashserver reload").color(net.kyori.adventure.text.format.NamedTextColor.RED))
            }
        })
    }

    private fun registerCommands() {
        config.servers.forEach { s ->
            val meta = proxy.commandManager.metaBuilder(s.server)
                .aliases(*s.commands.toTypedArray())
                .plugin(this)
                .build()
            proxy.commandManager.register(
                meta,
                VelocityNavigationCommand(proxy, s.server, s.permission)
            )
        }
    }

    @Subscribe
    fun onCommandExecute(event: com.velocitypowered.api.event.command.CommandExecuteEvent) {
        val player = event.commandSource as? com.velocitypowered.api.proxy.Player ?: return
        val currentServer = player.currentServer.orElse(null)?.serverInfo?.name ?: return
        
        val cmd = event.command.split(" ")[0].lowercase()
        config.servers.forEach { s ->
            val aliases = s.commands + s.server
            if (aliases.contains(cmd) && s.disabledServers.contains(currentServer)) {
                event.result = com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult.forwardToServer()
                return
            }
        }
    }

}
