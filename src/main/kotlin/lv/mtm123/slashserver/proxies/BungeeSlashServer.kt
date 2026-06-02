package lv.mtm123.slashserver.proxies

import lv.mtm123.slashserver.util.Config
import net.md_5.bungee.api.plugin.Plugin
import org.bstats.bungeecord.Metrics
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.ChatColor

class BungeeSlashServer : Plugin(), Listener {

    private lateinit var config: Config

    override fun onEnable() {
        config = Config.loadConfig(dataFolder)
        proxy.pluginManager.registerListener(this, this)
        
        proxy.pluginManager.registerCommand(this, object : Command("slashserver", "slashserver.admin") {
            override fun execute(sender: CommandSender, args: Array<out String>) {
                if (args.size == 1 && args[0].equals("reload", ignoreCase = true)) {
                    Config.reloadConfig(dataFolder, config)
                    sender.sendMessage(*ComponentBuilder("SlashServer config reloaded!").color(ChatColor.GREEN).create())
                } else {
                    sender.sendMessage(*ComponentBuilder("Usage: /slashserver reload").color(ChatColor.RED).create())
                }
            }
        })

        Metrics(this, 12510)
    }

    override fun onDisable() {
    }

    @EventHandler
    fun onChat(event: ChatEvent) {
        if (!event.isCommand) return
        val player = event.sender as? ProxiedPlayer ?: return

        val cmd = event.message.split(" ")[0].substring(1).lowercase()
        
        config.servers.forEach { s ->
            val aliases = s.commands.map { it.lowercase() } + s.server.lowercase()
            if (aliases.contains(cmd)) {
                if (s.disabledServers.contains(player.server.info.name)) {
                    // Do nothing, let it go to backend
                    return
                } else {
                    event.isCancelled = true
                    if (player.hasPermission(s.permission)) {
                        val server = proxy.getServerInfo(s.server)
                        if (server != null) {
                            player.connect(server)
                        }
                    }
                    return
                }
            }
        }
    }
}
