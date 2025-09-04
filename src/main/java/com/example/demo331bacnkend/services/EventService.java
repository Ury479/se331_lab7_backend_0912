package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Event;

import java.util.List;

public interface EventService {
    Integer getEventSize();
    List<Event> getEvents(Integer pageSize, Integer page);
    Event getEvent(Long id);
}
