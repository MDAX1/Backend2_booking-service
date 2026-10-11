package com.backend1.backend1.config;

import com.backend1.backend1.model.Booking;
import com.backend1.backend1.model.Room;
import com.backend1.backend1.model.RoomType;
import com.backend1.backend1.repository.BookingRepository;
import com.backend1.backend1.repository.RoomRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (roomRepository.count() > 0) return;

        List<Room> rooms = roomRepository.saveAll(List.of(
                room("101", RoomType.SINGLE, 0, "895"),
                room("102", RoomType.SINGLE, 0, "895"),
                room("103", RoomType.SINGLE, 0, "895"),
                room("104", RoomType.SINGLE, 0, "950"),
                room("201", RoomType.DOUBLE, 0, "1250"),
                room("202", RoomType.DOUBLE, 0, "1250"),
                room("203", RoomType.DOUBLE, 1, "1450"),
                room("204", RoomType.DOUBLE, 1, "1450"),
                room("205", RoomType.DOUBLE, 2, "1650"),
                room("301", RoomType.DOUBLE, 0, "1600"),
                room("302", RoomType.DOUBLE, 1, "1850"),
                room("303", RoomType.DOUBLE, 2, "2100")
        ));

        LocalDate today = LocalDate.now();

        bookingRepository.saveAll(List.of(
                // Past bookings
                booking(1L, rooms.get(4),  today.minusDays(47), today.minusDays(43), 2),
                booking(2L, rooms.get(0),  today.minusDays(37), today.minusDays(35), 1),
                booking(3L, rooms.get(11), today.minusDays(26), today.minusDays(22), 4),
                booking(4L, rooms.get(5),  today.minusDays(17), today.minusDays(12), 2),
                booking(5L, rooms.get(1),  today.minusDays(7),  today.minusDays(4),  1),
                booking(9L, rooms.get(8),  today.minusDays(20), today.minusDays(16), 3),
                booking(10L, rooms.get(2), today.minusDays(10), today.minusDays(8),  1),

                // Current bookings
                booking(6L, rooms.get(9),  today.minusDays(2),  today.plusDays(3),   2),
                booking(7L, rooms.get(6),  today,               today.plusDays(6),   3),

                // Upcoming bookings
                booking(8L, rooms.get(1),  today.plusDays(5),   today.plusDays(9),   1),
                booking(1L, rooms.get(10), today.plusDays(14),  today.plusDays(19),  2),
                booking(2L, rooms.get(8),  today.plusDays(20),  today.plusDays(25),  3),
                booking(3L, rooms.get(3),  today.plusDays(30),  today.plusDays(32),  1),
                booking(4L, rooms.get(11), today.plusDays(45),  today.plusDays(51),  4)
        ));
    }

    private Room room(String number, RoomType type, int extraBeds, String price) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setType(type);
        r.setExtraBeds(extraBeds);
        r.setPricePerNight(new BigDecimal(price));
        return r;
    }

    private Booking booking(Long customerId, Room room, LocalDate checkIn, LocalDate checkOut, int guests) {
        Booking b = new Booking();
        b.setCustomerId(customerId);
        b.setRoom(room);
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        b.setNumberOfGuests(guests);
        return b;
    }
}
