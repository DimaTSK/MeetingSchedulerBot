package meetingschedulerbot.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Meeting {
    private String id;
    private String title;
    private String creatorId;
    private List<DateOption> dateOptions = new ArrayList<>();
    private Map<String, User> participants = new HashMap<>();

    public Meeting() {
    }

    public Meeting(String id, String title, String creatorId) {
        this.id = id;
        this.title = title;
        this.creatorId = creatorId;
    }

    public Meeting(String id, String title, String creatorId, List<DateOption> dateOptions, Map<String, User> participants) {
        this.id = id;
        this.title = title;
        this.creatorId = creatorId;
        this.dateOptions = dateOptions;
        this.participants = participants;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public List<DateOption> getDateOptions() {
        return dateOptions;
    }

    public void setDateOptions(List<DateOption> dateOptions) {
        this.dateOptions = dateOptions;
    }

    public Map<String, User> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, User> participants) {
        this.participants = participants;
    }
}
