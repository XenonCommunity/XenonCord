package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.modules.commands.Maintenance;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener public class MotdChanger implements Listener {
    @EventHandler
    public void proxyPingEvent(final ProxyPingEvent e) {
        final ServerPing serverPing = e.getResponse();
        serverPing.setDescriptionComponent(new TextComponent(ChatColor.translateAlternateColorCodes('&',
                (Maintenance.downServers != null && Maintenance.downServers.contains("proxy")) ?
                        XenonCore.instance.getConfigData().getModules().getMaintenancemotd() :
                        XenonCore.instance.getConfigData().getModules().getMotd().replace("ONLINE", String.valueOf(XenonCore.instance.getBungeeInstance().getOnlineCount())))));
        e.setResponse(serverPing);
    }
}
