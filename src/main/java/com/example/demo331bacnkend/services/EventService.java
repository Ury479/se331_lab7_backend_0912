package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface EventService {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Page<Event> getEvents(String title, Pageable pageable);
    Event getEvent(Long id);
    Event createEvent(Event event);

    Event save(Event event);
}
