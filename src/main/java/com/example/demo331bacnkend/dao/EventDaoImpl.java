package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class EventDaoImpl implements EventDao {

    private List<Event> eventList;

    @PostConstruct
    public void init() {
        // Seed 6 demo events (same as previous db.json)
        eventList = new ArrayList<>();

        eventList.add(Event.builder()
                .id(123L)
                .category("animal welfare")
                .title("Cat Adoption Day")
                .description("Find your new feline friend at this event.")
                .location("Meow Town")
                .date("January 28, 2022")
                .time("12:00")
                .petAllowed(true)
                .organizer("Kat Laydee")
                .build());

        eventList.add(Event.builder()
                .id(456L)
                .category("food")
                .title("Community Gardening")
                .description("Join us as we tend to the community edible plants.")
                .location("Flora City")
                .date("March 14, 2022")
                .time("10:00")
                .petAllowed(true)
                .organizer("Fern Pollin")
                .build());

        eventList.add(Event.builder()
                .id(789L)
                .category("sustainability")
                .title("Beach Cleanup")
                .description("Help pick up trash along the shore.")
                .location("Playa Del Carmen")
                .date("July 22, 2022")
                .time("11:00")
                .petAllowed(false)
                .organizer("Carey Wales")
                .build());

        eventList.add(Event.builder()
                .id(1001L)
                .category("animal welfare")
                .title("Dog Adoption Day")
                .description("Find your new canine friend at this event.")
                .location("Woof Town")
                .date("August 28, 2022")
                .time("12:00")
                .petAllowed(true)
                .organizer("Dawg Dahd")
                .build());

        eventList.add(Event.builder()
                .id(1002L)
                .category("food")
                .title("Canned Food Drive")
                .description("Bring your canned food to donate to those in need.")
                .location("Tin City")
                .date("September 14, 2022")
                .time("3:00")
                .petAllowed(true)
                .organizer("Kahn Opiner")
                .build());

        eventList.add(Event.builder()
                .id(1003L)
                .category("sustainability")
                .title("Highway Cleanup")
                .description("Help pick up trash along the highway.")
                .location("Highway 50")
                .date("July 22, 2022")
                .time("11:00")
                .petAllowed(false)
                .organizer("Brody Kill")
                .build());
    }

    @Override
    public Integer getEventSize() {
        return eventList.size();
    }

    @Override
    public List<Event> getEvents(Integer pageSize, Integer page) {
        // High-level, cleaner pagination per the handout
        pageSize = pageSize == null ? eventList.size() : pageSize;
        page = page == null ? 1 : page;
        int firstIndex = (page - 1) * pageSize;

        // NOTE: this subList may throw IndexOutOfBounds when out of range;
        // the controller already try/catches as required by the lab handout.
        return eventList.subList(firstIndex, firstIndex + pageSize);
    }

    @Override
    public Event getEvent(Long id) {
        // Cleaner with streams, per the handout
        return eventList.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Event createEvent(Event event) {
        // Generate new ID if not provided
        if (event.getId() == null) {
            Long maxId = eventList.stream()
                    .mapToLong(Event::getId)
                    .max()
                    .orElse(0L);
            event = Event.builder()
                    .id(maxId + 1)
                    .category(event.getCategory())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .location(event.getLocation())
                    .date(event.getDate())
                    .time(event.getTime())
                    .petAllowed(event.getPetAllowed())
                    .organizer(event.getOrganizer())
                    .build();
        }
        eventList.add(event);
        return event;
    }
}
