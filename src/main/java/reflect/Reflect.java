package reflect;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.List;

public class Reflect extends JavaPlugin {
    private JDA jda;
    private int onlinePlayers;
    private String channelName;

    private class MinecraftEventListener implements Listener {
        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Reflect.this.minecraftToDiscord(event);
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Reflect.this.handlePlayerJoin();
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Reflect.this.handlePlayerQuit();
        }
    }

    private class DiscordEventListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
            Reflect.this.discordToMinecraft(event);
        }
    }

    @Override
    public void onEnable() {
        String token = getConfig().getString("bot-token");
        this.channelName = getConfig().getString("channel-name");

        if (token == null) {
            getLogger().warning("\"bot-token\" not specified, exiting.");
            getPluginLoader().disablePlugin(this);
        }

        if (this.channelName == null) {
            getLogger().warning("\"channel-name\" not specified, defaulting to \"reflect\".");
            this.channelName = "reflect";
        }

        try {
            this.jda = JDABuilder.createDefault(token)
                    .addEventListeners(new DiscordEventListener())
                    .build();
        } catch (LoginException err) {
            getLogger().warning("Failed to authenticate with discord api, exiting.");
            getPluginLoader().disablePlugin(this);
        }

        this.onlinePlayers = 0;
        this.updateStatus();

        getServer().getPluginManager().registerEvents(new MinecraftEventListener(), this);
    }

    private void minecraftToDiscord(AsyncPlayerChatEvent event) {
        List<TextChannel> channels = this.jda.getTextChannelsByName(this.channelName, true);

        for (TextChannel channel : channels) {
            channel.sendMessage("**<" + event.getPlayer().getDisplayName() + ">** " + event.getMessage()).queue();
        }
    }

    private void discordToMinecraft(MessageReceivedEvent event) {
        List<TextChannel> channels = this.jda.getTextChannelsByName(this.channelName, true);

        for (TextChannel channel : channels) {
            if (channel.getId().equals(event.getTextChannel().getId())) {
                if (event.getAuthor().isBot()) {
                    return;
                }

                Bukkit.broadcastMessage("[reflect] <" + event.getAuthor().getName() + "> " + event.getMessage().getContentDisplay());

                break;
            }
        }
    }

    private void handlePlayerJoin() {
        this.onlinePlayers++;
        this.updateStatus();
    }

    private void handlePlayerQuit() {
        this.onlinePlayers--;
        this.updateStatus();
    }

    private void updateStatus() {
        this.jda.getPresence().setActivity(Activity.playing(this.onlinePlayers + (this.onlinePlayers == 1 ? " player " : " players ") + "online."));
    }
}
