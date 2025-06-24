package meetingschedulerbot.service;

import meetingschedulerbot.model.DateOption;
import meetingschedulerbot.model.Meeting;
import meetingschedulerbot.model.UserState;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingService {
    Meeting createMeeting(String title, String creatorId);
    Meeting getMeeting(String meetingId);
    void addDateOption(String meetingId, LocalDateTime dateTime);
    void addVote(String meetingId, String optionId, String userId, String username);
    DateOption getBestOption(String meetingId);
    List<Meeting> getMeetingsByCreator(String creatorId);
    UserState getUserState(String userId);
    void setUserState(String userId, UserState.State state, String meetingId);
}