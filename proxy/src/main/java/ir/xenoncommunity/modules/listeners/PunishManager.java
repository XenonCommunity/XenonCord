package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;

@ModuleListener
public class PunishManager implements Listener {
    private SQLManager sqlManager;
    public PunishManager(){
        sqlManager = new SQLManager(XenonCore.instance.getConfiguration().getSqlPunishments(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "reason TEXT," +
                        "banduration BIGINT," +
                        "muteduration BIGINT," +
                        "lastpunish BIGINT" +
                        ");");
        new Reflections("ir.xenoncommunity.punishmanager").getSubTypesOf(Command.class).forEach(command ->{
            try {
                Constructor<?> constructor = command.getConstructor(SQLManager.class);
                XenonCore.instance.logdebuginfo(String.format("CMD %s loaded.", command.getSimpleName()));
                XenonCore.instance.getBungeeInstance().pluginManager.registerCommand(null, (Command) constructor.newInstance(sqlManager));
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }
    @EventHandler
    public void onJoin(final LoginEvent e) {
        final String username = e.getConnection().getName();
        XenonCore.instance.getTaskManager().add(() -> {
            try {
                final Integer banduration = (Integer) sqlManager.getData(username, "banduration");
                final Integer lastpunish = (Integer) sqlManager.getData(username, "lastpunish");
                final Integer currentTime = (int) System.currentTimeMillis();

                if(banduration == null || lastpunish == null || currentTime == null) return;

                if(banduration > 0){
                    if(currentTime - lastpunish < banduration) {
                        e.setReason(new TextComponent(ChatColor.translateAlternateColorCodes(
                                '&', XenonCore.instance.getConfigData().getPunishmanager().getBandisconnectmessage()
                                        .replace("PLAYER", username)
                                        .replace("REASON", (String) sqlManager.getData(username, "reason"))
                                        .replace("DURATION", String.valueOf(banduration / 60000)))));
                        e.setCancelled(true);
                        return;
                    }
                    @Cleanup PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                            "DELETE from Players where username = ?;");
                    preparedStatement.setString(1, username);
                    preparedStatement.executeUpdate();
                    sqlManager.updateDB(preparedStatement);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    @EventHandler
    public void onChat(final ChatEvent e){
        // add commands that should not bypass mute
        if(e.getMessage().startsWith("/")) return;
        final String username = ((CommandSender)e.getSender()).getName();
        try {
            final Integer muteduration = (Integer) sqlManager.getData(username, "muteduration");
            final Integer lastpunish = (Integer) sqlManager.getData(username, "lastpunish");
            final Integer currentTime = (int) System.currentTimeMillis();

            if(muteduration == null || lastpunish == null || currentTime == null) return;

            if(muteduration > 0){
                if(currentTime - lastpunish < muteduration) {
                    Message.send((CommandSender) e.getSender(), ChatColor.translateAlternateColorCodes(
                                    '&', XenonCore.instance.getConfigData().getPunishmanager().getMuteblockmessage()
                                            .replace("PLAYER", username)
                                            .replace("REASON", (String) sqlManager.getData(username, "reason"))
                                            .replace("DURATION", String.valueOf(muteduration / 60000)))
                            , false);
                    e.setCancelled(true);
                    return;
                }
                @Cleanup PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                        "DELETE from Players where username = ?;");
                preparedStatement.setString(1, username);
                preparedStatement.executeUpdate();
                sqlManager.updateDB(preparedStatement);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
/*

 */
}
