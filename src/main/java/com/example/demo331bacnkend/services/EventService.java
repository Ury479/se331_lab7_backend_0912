package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EventService {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Event getEvent(Long id);
    Event createEvent(Event event);

    Event save(Event event);
}
