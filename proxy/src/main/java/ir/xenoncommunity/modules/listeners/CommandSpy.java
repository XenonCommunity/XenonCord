package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener
public class CommandSpy implements Listener {
    @EventHandler
    public void onCommand(ChatEvent e) {
        if (!e.getMessage().startsWith("/") || !(e.getSender() instanceof ProxiedPlayer)) return;

        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (player.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpybypass())) return;

        final String rawCommand = e.getMessage();

        XenonCore.instance.getTaskManager().add(() -> {
            if (Arrays.stream(XenonCore.instance.getConfigData().getCommandspy().getSpyexceptions())
                    .map(String::toLowerCase)
                    .anyMatch(rawCommand.substring(1).toLowerCase()::contains)) return;

            XenonCore.instance.getBungeeInstance().getPlayers().stream()
                    .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getCommandspy().getSpyperm()))
                    .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                            XenonCore.instance.getConfigData().getCommandspy().getSpymessage()
                                    .replace("PLAYER", player.getDisplayName())
                                    .replace("COMMAND", rawCommand), true));
        });
    }
}
