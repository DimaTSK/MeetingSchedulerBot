package meetingschedulerbot.model;

public class UserState {
    private String userId;
    private State state;
    private String meetingId;

    public enum State {
        NONE,
        AWAITING_MEETING_TITLE,
        AWAITING_DATE_OPTION,
        AWAITING_VOTE
    }

    public UserState() {
    }

    public UserState(String userId, State state, String meetingId) {
        this.userId = userId;
        this.state = state;
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }
}
