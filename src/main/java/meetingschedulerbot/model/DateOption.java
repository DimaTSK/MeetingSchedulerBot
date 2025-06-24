package meetingschedulerbot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class DateOption {
    private String id;
    private LocalDateTime dateTime;
    private Set<String> votes = new HashSet<>();

    public DateOption() {
    }

    public DateOption(String id, LocalDateTime dateTime) {
        this.id = id;
        this.dateTime = dateTime;
    }

    public DateOption(String id, LocalDateTime dateTime, Set<String> votes) {
        this.id = id;
        this.dateTime = dateTime;
        this.votes = votes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Set<String> getVotes() {
        return votes;
    }

    public void setVotes(Set<String> votes) {
        this.votes = votes;
    }

    public String getFormattedDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return dateTime.format(formatter);
    }
}
