package com.bookfair.Stall_Reservation.service.impl;

import com.bookfair.Stall_Reservation.entity.*;
import com.bookfair.Stall_Reservation.enums.ReservationStatus;
import com.bookfair.Stall_Reservation.enums.StallSize;
import com.bookfair.Stall_Reservation.service.EmailService;
import com.bookfair.Stall_Reservation.repository.*;
import com.bookfair.Stall_Reservation.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bookfair.Stall_Reservation.dto.event.CreateEventRequest;
import com.bookfair.Stall_Reservation.dto.event.CreateStallRequest;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final StallRepository stallRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStallRepository reservationStallRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public EventServiceImpl(EventRepository eventRepository,
                            StallRepository stallRepository,
                            ReservationRepository reservationRepository,
                            ReservationStallRepository reservationStallRepository,
                            UserRepository userRepository,
                            EmailService emailService) {
        this.eventRepository = eventRepository;
        this.stallRepository = stallRepository;
        this.reservationRepository = reservationRepository;
        this.reservationStallRepository = reservationStallRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Map<String, Object>> listUpcoming() {
        var events = eventRepository.findByActiveTrueOrderByEventDateAsc();
        return events.stream().map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "name", e.getName(),
                        "description",
                        e.getDescription() != null
                                ? (e.getDescription().length() > 200
                                ? e.getDescription().substring(0, 200) + "..."
                                : e.getDescription())
                                : "",
                        "location", e.getLocation() != null ? e.getLocation() : "",
                        "eventDate", e.getEventDate().toString(),
                        "imageUrl", e.getImageUrl() != null ? e.getImageUrl() : "",
                        "stallsBooked", reservationRepository.countActiveByEventId(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getById(Long id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null)
            return null;

        List<Stall> stalls = stallRepository.findByEventIdOrderByStallCode(id);
        long bookedCount = reservationRepository.countActiveByEventId(id);
        List<Long> bookedStallIds = reservationStallRepository.findBookedStallIdsByEventId(id);

        return Map.of(
                "id", event.getId(),
                "name", event.getName(),
                "description", event.getDescription() != null ? event.getDescription() : "",
                "location", event.getLocation() != null ? event.getLocation() : "",
                "eventDate", event.getEventDate().toString(),
                "imageUrl", event.getImageUrl() != null ? event.getImageUrl() : "",
                "stallsBooked", bookedCount,
                "bookedStallIds", bookedStallIds,
                "stalls", stalls.stream().map(s -> {
                    Map<String, Object> sm = new HashMap<>();
                    sm.put("id", s.getId());
                    sm.put("stallCode", s.getStallCode());
                    sm.put("size", s.getSize().name());
                    sm.put("price", s.getPrice());
                    sm.put("blocked", s.isBlocked());
                    sm.put("positionX", s.getPositionX());
                    sm.put("positionY", s.getPositionY());
                    return sm;
                }).collect(Collectors.toList()));
    }

    @Override
    public List<Map<String, Object>> getStallAvailability(Long eventId) {
        return stallRepository.findByEventIdOrderByStallCode(eventId).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "stallCode", s.getStallCode(),
                        "size", s.getSize().name(),
                        "price", s.getPrice(),
                        "blocked", s.isBlocked()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listAll(String search) {
        List<Event> events = eventRepository.findAllByOrderByEventDateDesc();
        LocalDateTime now = LocalDateTime.now();
        return events.stream()
                .filter(e -> e.isActive())
                .filter(e -> search == null || search.isBlank()
                        || e.getName().toLowerCase().contains(search.toLowerCase()))
                .map(e -> {
                    String period = e.getEventDate().isBefore(now) ? "past"
                            : (e.getEventDate().toLocalDate().equals(now.toLocalDate())
                            ? "present"
                            : "future");
                    return Map.<String, Object>of(
                            "id", e.getId(),
                            "name", e.getName(),
                            "location", e.getLocation() != null ? e.getLocation() : "",
                            "eventDate", e.getEventDate().toString(),
                            "period", period);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAdminDetail(Long id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null)
            return null;
        List<Stall> stalls = stallRepository.findByEventIdOrderByStallCode(id);
        List<Reservation> reservations = reservationRepository.findByEventId(id);
        Map<Long, String> stallToVendor = new HashMap<>();
        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.REFUNDED
                    || r.getStatus() == ReservationStatus.EVENT_REMOVED)
                continue;
            for (var rs : r.getStalls()) {
                stallToVendor.put(rs.getStall().getId(), r.getVendor().getName());
            }
        }
        return new HashMap<>(Map.of(
                "id", event.getId(),
                "name", event.getName(),
                "description", event.getDescription() != null ? event.getDescription() : "",
                "location", event.getLocation() != null ? event.getLocation() : "",
                "eventDate", event.getEventDate().toString(),
                "imageUrl", event.getImageUrl() != null ? event.getImageUrl() : "",
                "stalls", stalls.stream().map(s -> {
                    Map<String, Object> sm = new HashMap<>();
                    sm.put("id", s.getId());
                    sm.put("stallCode", s.getStallCode());
                    sm.put("size", s.getSize().name());
                    sm.put("price", s.getPrice());
                    sm.put("blocked", s.isBlocked());
                    sm.put("bookedBy", stallToVendor.getOrDefault(s.getId(), ""));
                    sm.put("positionX", s.getPositionX());
                    sm.put("positionY", s.getPositionY());
                    return sm;
                }).collect(Collectors.toList())));
    }

    @Override
    @Transactional
    public Long createEvent(CreateEventRequest body, Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        Event event = new Event();
        event.setName(body.getName());
        event.setDescription(body.getDescription());
        event.setLocation(body.getLocation());
        event.setEventDate(body.getEventDate());
        event.setImageUrl(body.getImageUrl() != null ? body.getImageUrl() : "");
        event.setCreatedBy(admin);

        eventRepository.save(event);

        for (CreateStallRequest s : body.getStalls()) {
            Stall stall = new Stall();

            stall.setEvent(event);
            stall.setStallCode(s.getStallCode());
            stall.setSize(s.getSize());
            stall.setPrice(s.getPrice());
            stall.setBlocked(s.isBlocked());

            if (s.getPositionX() != null) {
                stall.setPositionX(s.getPositionX());
            }

            if (s.getPositionY() != null) {
                stall.setPositionY(s.getPositionY());
            }

            stallRepository.save(stall);
        }
        return event.getId();
    }

    @Override
    @Transactional
    public void removeEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot remove past events");
        }
        List<Reservation> reservations = reservationRepository.findByEventId(id);
        for (Reservation r : reservations) {
            r.setStatus(ReservationStatus.EVENT_REMOVED);
            reservationRepository.save(r);
            emailService.sendEventRemovedNotice(r.getVendor().getEmail(), event.getName(),
                    r.getBookingId());
        }
        event.setActive(false);
        eventRepository.save(event);
    }
}
