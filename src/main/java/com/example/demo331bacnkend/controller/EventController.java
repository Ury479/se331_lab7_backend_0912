package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.entity.Event;
import com.example.demo331bacnkend.services.EventService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RestController
@RequestMapping // base path optional
public class EventController {

    // Prefer constructor injection; if you use Lombok, you can add @RequiredArgsConstructor on class.
    private final EventService eventService;
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** GET /events with pagination: _limit & _page */
    @GetMapping("/events") // 若你作业要求是 /event，请改为 "/event"
    public ResponseEntity<List<Event>> getEventLists(
            @RequestParam(value = "_limit", required = false) Integer perPage,
            @RequestParam(value = "_page",  required = false) Integer page,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "mode",  required = false, defaultValue = "default") String mode) {
        perPage = perPage == null ? 3 : perPage;
        page = page == null ? 1 : page;
        Page<Event> pageOutput;
        if (title == null) {
            pageOutput = eventService.getEvents(perPage, page);
        } else {
            String m = mode == null ? "default" : mode.toLowerCase();
            if ("and".equals(m)) {
                pageOutput = eventService.getEventsAnd(title, PageRequest.of(page - 1, perPage));
            } else if ("or".equals(m)) {
                pageOutput = eventService.getEventsOr(title, PageRequest.of(page - 1, perPage));
            } else {
                pageOutput = eventService.getEvents(title, PageRequest.of(page - 1, perPage));
            }
        }
        HttpHeaders responseHeader = new HttpHeaders();
        // Expose total count to frontend for pagination
        responseHeader.set("x-total-count",String.valueOf(pageOutput.getTotalElements()));
        return  new ResponseEntity<>(pageOutput.getContent(), responseHeader, HttpStatus.OK);
    }

    /** GET /events/{id} - fetch single event by id */
    @GetMapping("/events/{id}") // 若你作业要求是 /event/{id}，同步改这里
    public ResponseEntity<Event> getEvent(@PathVariable("id") Long id) {
        Event output = eventService.getEvent(id);
        // lab7 中的要求
        if (output != null) {
            return ResponseEntity.ok(output);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The given id is not found");
    }

    /** POST /events - create new event */
    @PostMapping("/events")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        try {
            // Basic validation
            if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event title is required");
            }
            if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event description is required");
            }
            if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event location is required");
            }
            if (event.getDate() == null || event.getDate().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event date is required");
            }
            
            Event createdEvent = eventService.createEvent(event);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (ResponseStatusException rse) {
            throw rse; // Re-throw validation errors
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create event: " + ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> updateEvent(@RequestBody Event event) {
        Event output = eventService.save(event);
        return ResponseEntity.ok(output);
    }
}
