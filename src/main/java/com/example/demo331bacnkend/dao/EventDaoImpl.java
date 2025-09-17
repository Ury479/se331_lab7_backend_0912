package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manual in-memory DAO implementation.
 * This bean is active only when profile "manual" is enabled.
 */
@Repository
@Profile("manual")
@RequiredArgsConstructor
public class EventDaoImpl implements EventDao {

    /** In-memory store for events */
    private final List<Event> eventList = new ArrayList<>();

    /** Seed initial data after bean construction */
    @PostConstruct
    public void init() {
        // Academic: Midterm Exam
        eventList.add(Event.builder()
                .id(1L)
                .category("Academic")
                .title("Midterm Exam")
                .description("A time for taking the exam")
                .location("CAMT Building")
                .date("3rd Sept")
                .time("3.00-4.00 pm.")
                .petAllowed(false)
                .organizer("CAMT")
                .build());

        // Academic: Commencement Day
        eventList.add(Event.builder()
                .id(2L)
                .category("Academic")
                .title("Commencement Day")
                .description("A time for celebration")
                .location("CMU Convention hall")
                .date("21th Jan")
                .time("8.00am-4.00 pm.")
                .petAllowed(false)
                .organizer("CMU")
                .build());

        // Cultural: Loy Krathong
        eventList.add(Event.builder()
                .id(3L)
                .category("Cultural")
                .title("Loy Krathong")
                .description("A time for Krathong")
                .location("Ping River")
                .date("21th Nov")
                .time("8.00-10.00 pm.")
                .petAllowed(false)
                .organizer("Chiang Mai")
                .build());

        // Cultural: Songkran
        eventList.add(Event.builder()
                .id(4L)
                .category("Cultural")
                .title("Songkran")
                .description("Let's Play Water")
                .location("Chiang Mai Moat")
                .date("13th April")
                .time("10.00am - 6.00 pm.")
                .petAllowed(true)
                .organizer("Chiang Mai Municipality")
                .build());
    }

    /** Return total size of events */
    @Override
    public Integer getEventSize() {
        return eventList.size();
    }

    /**
     * Return paginated events.
     * If pageSize is null -> return all.
     * If page is null -> treat as page 1.
     */
    @Override
    public Page<Event> getEvents(Integer pageSize, Integer page) {
        int size = (pageSize == null || pageSize <= 0) ? eventList.size() : pageSize;
        int p = (page == null || page <= 0) ? 0 : (page - 1); // PageRequest uses 0-based index

        int from = p * size;
        int to = Math.min(eventList.size(), from + size);
        List<Event> content = from >= eventList.size() ? new ArrayList<>() : new ArrayList<>(eventList.subList(from, to));
        return new PageImpl<>(content, PageRequest.of(p, size), eventList.size());
    }

    @Override
    public Page<Event> getEvents(String title, Pageable page) {
        List<Event> filtered = new ArrayList<>();
        for (Event e : eventList) {
            boolean titleMatch = e.getTitle() != null && e.getTitle().contains(title);
            boolean descMatch = e.getDescription() != null && e.getDescription().contains(title);
            if (titleMatch || descMatch) {
                filtered.add(e);
            }
        }
        int from = (int) page.getOffset();
        int to = Math.min(filtered.size(), from + page.getPageSize());
        List<Event> content = from >= filtered.size() ? new ArrayList<>() : new ArrayList<>(filtered.subList(from, to));
        return new PageImpl<>(content, page, filtered.size());
    }

    /** Find one by id */
    @Override
    public Event getEvent(Long id) {
        return eventList.stream()
                .filter(e -> Objects.equals(e.getId(), id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Event createEvent(Event event) {
        // naive id generation if missing
        if (event.getId() == null) {
            long nextId = eventList.stream()
                    .map(Event::getId)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L) + 1;
            event.setId(nextId);
        }
        eventList.add(event);
        return event;
    }

    @Override
    public Event save(Event event) {
        if (event.getId() == null) {
            return createEvent(event);
        }
        // replace existing by id
        for (int i = 0; i < eventList.size(); i++) {
            if (Objects.equals(eventList.get(i).getId(), event.getId())) {
                eventList.set(i, event);
                return event;
            }
        }
        eventList.add(event);
        return event;
    }
}
