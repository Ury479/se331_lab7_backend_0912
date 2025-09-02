package entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Event {
    Long id;
    String category;
    String title;
    String description;
    String location;
    String date;
    String time;
    Boolean petAllowed;
    String organizer;
}
