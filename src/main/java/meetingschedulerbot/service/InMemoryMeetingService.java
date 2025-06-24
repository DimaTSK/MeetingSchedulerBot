package meetingschedulerbot.service;
import meetingschedulerbot.model.DateOption;
import meetingschedulerbot.model.Meeting;
import meetingschedulerbot.model.User;
import meetingschedulerbot.model.UserState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class InMemoryMeetingService implements MeetingService {
    private final Map<String, Meeting> meetings = new ConcurrentHashMap<>();
    private final Map<String, UserState> userStates = new ConcurrentHashMap<>();

    @Override
    public Meeting createMeeting(String title, String creatorId) {
        String meetingId = generateUniqueId();
        Meeting meeting = new Meeting(meetingId, title, creatorId);
        meetings.put(meetingId, meeting);
        return meeting;
    }

    @Override
    public Meeting getMeeting(String meetingId) {
        return meetings.get(meetingId);
    }

    @Override
    public void addDateOption(String meetingId, LocalDateTime dateTime) {
        Meeting meeting = meetings.get(meetingId);
        if (meeting != null) {
            DateOption option = new DateOption(generateUniqueId(), dateTime);
            meeting.getDateOptions().add(option);
        }
    }

    @Override
    public void addVote(String meetingId, String optionId, String userId, String username) {
        Meeting meeting = meetings.get(meetingId);
        if (meeting != null) {
            meeting.getDateOptions().stream()
                    .filter(option -> option.getId().equals(optionId))
                    .findFirst()
                    .ifPresent(option -> option.getVotes().add(userId));

            meeting.getParticipants().put(userId, new User(userId, username));
        }
    }

    @Override
    public DateOption getBestOption(String meetingId) {
        Meeting meeting = meetings.get(meetingId);
        if (meeting != null && !meeting.getDateOptions().isEmpty()) {
            return meeting.getDateOptions().stream()
                    .max(Comparator.comparingInt(option -> option.getVotes().size()))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public List<Meeting> getMeetingsByCreator(String creatorId) {
        return meetings.values().stream()
                .filter(meeting -> meeting.getCreatorId().equals(creatorId))
                .collect(Collectors.toList());
    }

    @Override
    public UserState getUserState(String userId) {
        return userStates.getOrDefault(userId, new UserState(userId, UserState.State.NONE, null));
    }

    @Override
    public void setUserState(String userId, UserState.State state, String meetingId) {
        userStates.put(userId, new UserState(userId, state, meetingId));
    }

    private String generateUniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}