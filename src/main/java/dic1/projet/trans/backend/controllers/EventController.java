package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.CreateEventRequest;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import dic1.projet.trans.backend.dtos.EventSearchRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    // add a new event
    @PostMapping("/event")
    public Event createEvent(@RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    // get all events
    @GetMapping("/allEvents")
    public Iterable<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    // get event by id
    @GetMapping("/event/{id}")
    public Optional<Event> getEventById(@PathVariable String id) {
        return eventService.getEvent(id);
    }

    // update event
    @PutMapping("/event/{id}")
    public Event updateEvent(@PathVariable String id, @RequestBody CreateEventRequest request) {
        return eventService.updateEvent(id, request);
    }

    // delete event
    @DeleteMapping("/event/{id}")
    public void deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
    }

    @GetMapping("/search")
    public List<Event> searchEvents(
            @RequestParam(required = false, name = "title") String title,
            @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "typeEvent") String typeEvent,
            @RequestParam(required = false, name = "eventStatus") String eventStatus) {
        
        List<Event> events = eventService.searchEvents(title, date, location, typeEvent, eventStatus);
        return events;
    }
}
