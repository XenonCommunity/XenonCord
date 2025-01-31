package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.reflections.Reflections;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.util.Arrays;

@ModuleListener
public class PunishManager implements Listener {
    private SQLManager sqlManager;
    public PunishManager(){
        XenonCore.instance.getTaskManager().async(this::initBackend);
        if(XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) return;

        sqlManager = new SQLManager(XenonCore.instance.getConfiguration().getSqlPunishments(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "reason TEXT," +
                        "banduration BIGINT," +
                        "muteduration BIGINT," +
                        "lastpunish BIGINT," +
                        "punishadmin TEXT" +
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
        if(XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) return;
        final String username = e.getConnection().getName();
        XenonCore.instance.getTaskManager().add(() -> {
            try {
                final Integer banduration = (Integer) sqlManager.getData(username, "banduration");
                final Integer lastpunish = (Integer) sqlManager.getData(username, "lastpunish");
                final Integer currentTime = (int) System.currentTimeMillis();
                final String punishAdmin = (String) sqlManager.getData(e.getConnection().getName(), "punishadmin");

                if(banduration == null || lastpunish == null || currentTime == null || punishAdmin == null) return;

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
                    Message.send(XenonCore.instance.getConfigData().getPunishmanager().getUnbanconsolelogmessage()
                            .replace("PLAYER1",
                            e.getConnection().getName()
                                   ) .replace("PLAYER2", punishAdmin));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    @EventHandler
    public void onChat(final ChatEvent e){
        if(XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) return;
        if(e.getMessage().startsWith("/") &&
                Arrays.stream(XenonCore.instance.getConfigData().getPunishmanager().getMutecommands())
                        .noneMatch(element -> e.getMessage().split(" ")[0].equals(element))) {
            return;
        }
        final String username = ((CommandSender)e.getSender()).getName();
        try {
            final Integer muteduration = (Integer) sqlManager.getData(username, "muteduration");
            final Integer lastpunish = (Integer) sqlManager.getData(username, "lastpunish");
            final Integer currentTime = (int) System.currentTimeMillis();
            final String punishAdmin = (String) sqlManager.getData(((CommandSender) e.getSender()).getName(), "punishadmin");

            if(muteduration == null || lastpunish == null || currentTime == null || punishAdmin == null) return;

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
                Message.send(XenonCore.instance.getConfigData().getPunishmanager().getUnmuteconsolelogmessage()
                        .replace("PLAYER1",
                                ((CommandSender) e.getSender()).getName())
                        .replace("PLAYER2", punishAdmin));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SneakyThrows private void initBackend(){
        @Cleanup final ServerSocket serverSocket = new ServerSocket( 20019, 50, InetAddress.getByName("127.0.0.1"));

        while(true){
            @Cleanup final Socket socket = serverSocket.accept();
            @Cleanup final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String req;
            while((req = br.readLine()) != null){

                XenonCore.instance.getBungeeInstance().getPluginManager().dispatchCommand(
                        XenonCore.instance.getBungeeInstance().getConsole(), req
                );
            }
        }
    }
}
