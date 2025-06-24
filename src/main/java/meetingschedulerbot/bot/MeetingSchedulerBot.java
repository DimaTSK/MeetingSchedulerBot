package meetingschedulerbot.bot;

import meetingschedulerbot.config.BotConfig;
import meetingschedulerbot.model.DateOption;
import meetingschedulerbot.model.Meeting;
import meetingschedulerbot.model.UserState;
import meetingschedulerbot.service.MeetingService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MeetingSchedulerBot extends TelegramLongPollingBot {
    private final MeetingService meetingService;
    private final BotConfig config;

    private static final String HELP_TEXT = """
        Я бот для организации встреч! Вот мои команды:
        
        /newmeeting - создать новую встречу
        /mymeetings - показать ваши встречи
        /help - показать это сообщение
        
        Для создания встречи:
        1. Отправьте /newmeeting
        2. Введите название встречи
        3. Добавьте варианты дат и времени в формате дд.мм.гггг чч:мм
        4. Отправьте /done когда закончите добавлять даты
        5. Поделитесь ID встречи с участниками
        
        Для голосования:
        1. Отправьте /vote ID_ВСТРЕЧИ
        2. Выберите удобные варианты дат
        """;

    // Обновленный конструктор с передачей токена в родительский класс
    public MeetingSchedulerBot(BotConfig config, MeetingService meetingService) {
        super(config.getBotToken());
        this.config = config;
        this.meetingService = meetingService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Обработка сообщений
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userId = update.getMessage().getFrom().getId().toString();
            String username = update.getMessage().getFrom().getUserName() != null
                    ? update.getMessage().getFrom().getUserName()
                    : update.getMessage().getFrom().getFirstName();

            UserState userState = meetingService.getUserState(userId);

            // Обработка команд
            if (messageText.startsWith("/")) {
                handleCommand(chatId, userId, username, messageText, userState);
            } else {
                // Обработка обычных сообщений в зависимости от состояния пользователя
                handleMessageByState(chatId, userId, username, messageText, userState);
            }
        }

        // Обработка колбэков от инлайн-кнопок
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String userId = update.getCallbackQuery().getFrom().getId().toString();
            String username = update.getCallbackQuery().getFrom().getUserName() != null
                    ? update.getCallbackQuery().getFrom().getUserName()
                    : update.getCallbackQuery().getFrom().getFirstName();

            handleCallback(chatId, userId, username, callbackData);
        }
    }

    private void handleCommand(long chatId, String userId, String username, String command, UserState userState) {
        if (command.equals("/start") || command.equals("/help")) {
            sendMessage(chatId, HELP_TEXT);
            meetingService.setUserState(userId, UserState.State.NONE, null);
        } else if (command.equals("/newmeeting")) {
            sendMessage(chatId, "Введите название встречи:");
            meetingService.setUserState(userId, UserState.State.AWAITING_MEETING_TITLE, null);
        } else if (command.equals("/mymeetings")) {
            showUserMeetings(chatId, userId);
        } else if (command.equals("/done") && userState.getState() == UserState.State.AWAITING_DATE_OPTION) {
            Meeting meeting = meetingService.getMeeting(userState.getMeetingId());
            if (meeting != null && !meeting.getDateOptions().isEmpty()) {
                String response = "Встреча \"" + meeting.getTitle() + "\" создана!\n" +
                        "ID встречи: " + meeting.getId() + "\n\n" +
                        "Варианты дат:\n" + formatDateOptions(meeting) + "\n\n" +
                        "Участники могут проголосовать, отправив команду:\n" +
                        "/vote " + meeting.getId();
                sendMessage(chatId, response);
                meetingService.setUserState(userId, UserState.State.NONE, null);
            } else {
                sendMessage(chatId, "Нужно добавить хотя бы один вариант даты. Введите дату и время в формате дд.мм.гггг чч:мм");
            }
        } else if (command.startsWith("/vote ")) {
            String meetingId = command.substring(6).trim();
            Meeting meeting = meetingService.getMeeting(meetingId);
            if (meeting != null) {
                showVotingOptions(chatId, userId, meeting);
                meetingService.setUserState(userId, UserState.State.AWAITING_VOTE, meetingId);
            } else {
                sendMessage(chatId, "Встреча с ID " + meetingId + " не найдена.");
            }
        } else if (command.startsWith("/results ")) {
            String meetingId = command.substring(9).trim();
            showMeetingResults(chatId, meetingId);
        } else {
            sendMessage(chatId, "Неизвестная команда. Отправьте /help для получения списка доступных команд.");
        }
    }

    private void handleMessageByState(long chatId, String userId, String username, String messageText, UserState userState) {
        switch (userState.getState()) {
            case AWAITING_MEETING_TITLE:
                Meeting meeting = meetingService.createMeeting(messageText, userId);
                sendMessage(chatId, "Встреча \"" + messageText + "\" создается.\n\n" +
                        "Теперь добавьте варианты дат и времени в формате дд.мм.гггг чч:мм\n" +
                        "Например: 25.12.2023 18:30\n\n" +
                        "Когда закончите добавлять даты, отправьте /done");
                meetingService.setUserState(userId, UserState.State.AWAITING_DATE_OPTION, meeting.getId());
                break;

            case AWAITING_DATE_OPTION:
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    LocalDateTime dateTime = LocalDateTime.parse(messageText, formatter);

                    meetingService.addDateOption(userState.getMeetingId(), dateTime);
                    sendMessage(chatId, "Вариант даты добавлен: " + messageText + "\n" +
                            "Добавьте еще вариант или отправьте /done для завершения.");
                } catch (DateTimeParseException e) {
                    sendMessage(chatId, "Неверный формат даты. Используйте формат дд.мм.гггг чч:мм\n" +
                            "Например: 25.12.2023 18:30");
                }
                break;

            default:
                sendMessage(chatId, "Отправьте /help для получения списка доступных команд.");
                break;
        }
    }

    private void handleCallback(long chatId, String userId, String username, String callbackData) {
        if (callbackData.startsWith("vote_")) {
            String[] parts = callbackData.split("_");
            if (parts.length == 3) {
                String meetingId = parts[1];
                String optionId = parts[2];

                meetingService.addVote(meetingId, optionId, userId, username);

                Meeting meeting = meetingService.getMeeting(meetingId);
                if (meeting != null) {
                    sendMessage(chatId, "Ваш голос учтен!\n\nТекущие результаты:\n" + formatVotingResults(meeting));
                }
            }
        }
    }

    private void showUserMeetings(long chatId, String userId) {
        List<Meeting> meetings = meetingService.getMeetingsByCreator(userId);

        if (meetings.isEmpty()) {
            sendMessage(chatId, "У вас пока нет созданных встреч.");
            return;
        }

        StringBuilder sb = new StringBuilder("Ваши встречи:\n\n");
        for (Meeting meeting : meetings) {
            sb.append("📅 \"").append(meeting.getTitle()).append("\"\n");
            sb.append("ID: ").append(meeting.getId()).append("\n");
            sb.append("Варианты дат: ").append(meeting.getDateOptions().size()).append("\n");
            sb.append("Участников: ").append(meeting.getParticipants().size()).append("\n");
            sb.append("Результаты: /results ").append(meeting.getId()).append("\n\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void showVotingOptions(long chatId, String userId, Meeting meeting) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Встреча: \"" + meeting.getTitle() + "\"\n" +
                "Выберите удобные для вас варианты:");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (DateOption option : meeting.getDateOptions()) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            button.setText(option.getFormattedDateTime());
            button.setCallbackData("vote_" + meeting.getId() + "_" + option.getId());

            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showMeetingResults(long chatId, String meetingId) {
        Meeting meeting = meetingService.getMeeting(meetingId);
        if (meeting == null) {
            sendMessage(chatId, "Встреча с ID " + meetingId + " не найдена.");
            return;
        }

        DateOption bestOption = meetingService.getBestOption(meetingId);

        StringBuilder sb = new StringBuilder();
        sb.append("Результаты встречи \"").append(meeting.getTitle()).append("\":\n\n");
        sb.append(formatVotingResults(meeting)).append("\n\n");

        if (bestOption != null) {
            sb.append("🏆 Оптимальная дата: ").append(bestOption.getFormattedDateTime());
            sb.append(" (").append(bestOption.getVotes().size()).append(" голосов)");
        } else {
            sb.append("Пока нет голосов.");
        }

        sendMessage(chatId, sb.toString());
    }

    private String formatDateOptions(Meeting meeting) {
        return meeting.getDateOptions().stream()
                .map(DateOption::getFormattedDateTime)
                .collect(Collectors.joining("\n"));
    }

    private String formatVotingResults(Meeting meeting) {
        StringBuilder sb = new StringBuilder();

        for (DateOption option : meeting.getDateOptions()) {
            sb.append(option.getFormattedDateTime());
            sb.append(" - ").append(option.getVotes().size()).append(" голосов");

            if (!option.getVotes().isEmpty()) {
                sb.append(" (");
                List<String> voterNames = option.getVotes().stream()
                        .map(voterId -> meeting.getParticipants().get(voterId))
                        .filter(user -> user != null)
                        .map(user -> user.getUsername() != null ? user.getUsername() : "User")
                        .collect(Collectors.toList());
                sb.append(String.join(", ", voterNames));
                sb.append(")");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
