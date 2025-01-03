package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.command.ConsoleCommandSender;

public class Ping extends Command implements Listener {
    public Ping(){
        super("Ping", XenonCore.instance.getConfigData().getPing().getPingperm());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(XenonCore.instance.getConfigData().getPing().getPingperm())) return;

        if (sender instanceof ConsoleCommandSender) {
            Message.send(sender, XenonCore.instance.getConfigData().getCannotexecasconsoleerrormessage(), false);
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length == 0) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getPing().getPingmessage()
                            .replace("PING", String.valueOf(player.getPing())), false);
            return;
        }

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getPing().getPingothersperm())) return;

        Message.send(sender,
                XenonCore.instance.getConfigData().getPing().getPingothersmessage()
                        .replace("PING", String.valueOf(XenonCore.instance.getBungeeInstance().getPlayer(args[0]).getPing()))
                        .replace("USERNAME", args[0]), false);
    }
}
