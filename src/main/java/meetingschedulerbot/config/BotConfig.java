package meetingschedulerbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {
    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }
}
