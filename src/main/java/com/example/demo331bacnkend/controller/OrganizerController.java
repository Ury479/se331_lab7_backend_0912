package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.entity.Organizer;
import com.example.demo331bacnkend.services.OrganizerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** REST endpoints for organizers: pagination and get-by-id. */
@RestController
public class OrganizerController {

    private final OrganizerService organizerService;

    public OrganizerController(OrganizerService organizerService) {
        this.organizerService = organizerService;
    }

    // GET /organizers?_limit=...&_page=...
    @GetMapping("/organizers")
    public ResponseEntity<List<Organizer>> list(
            @RequestParam(value = "_limit", required = false) Integer perPage,
            @RequestParam(value = "_page",  required = false) Integer page) {

        int total = organizerService.getOrganizerSize();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-total-count", String.valueOf(total));

        try {
            List<Organizer> items = organizerService.getOrganizers(perPage, page);
            return new ResponseEntity<>(items, headers, HttpStatus.OK);
        } catch (IndexOutOfBoundsException ex) {
            // follow the same lab behavior as events: return empty with 200
            return new ResponseEntity<>(List.of(), headers, HttpStatus.OK);
        }
    }

    // GET /organizers/{id}
    @GetMapping("/organizers/{id}")
    public ResponseEntity<Organizer> getOne(@PathVariable Long id) {
        Organizer o = organizerService.getOrganizer(id);
        if (o != null) return ResponseEntity.ok(o);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The given id is not found");
    }
}
