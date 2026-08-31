package com.bookfair.Stall_Reservation;

import com.bookfair.Stall_Reservation.dto.reservation.CreateBookingRequest;
import com.bookfair.Stall_Reservation.entity.Event;
import com.bookfair.Stall_Reservation.entity.Reservation;
import com.bookfair.Stall_Reservation.entity.Stall;
import com.bookfair.Stall_Reservation.entity.User;
import com.bookfair.Stall_Reservation.enums.BusinessCategory;
import com.bookfair.Stall_Reservation.enums.PaymentMethod;
import com.bookfair.Stall_Reservation.enums.StallSize;
import com.bookfair.Stall_Reservation.enums.StallType;
import com.bookfair.Stall_Reservation.enums.UserRole;
import com.bookfair.Stall_Reservation.repository.EventRepository;
import com.bookfair.Stall_Reservation.repository.ReservationRepository;
import com.bookfair.Stall_Reservation.repository.ReservationStallRepository;
import com.bookfair.Stall_Reservation.repository.StallRepository;
import com.bookfair.Stall_Reservation.repository.UserRepository;
import com.bookfair.Stall_Reservation.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "OIDC_ISSUER=https://mock-issuer.local"
})
public class BookingConcurrencyTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationStallRepository reservationStallRepository;

    @Test
    public void testConcurrentStallBooking() throws Exception {
        // 1. Setup Event and Stall
        String runId = java.util.UUID.randomUUID().toString().substring(0, 8);
        Event event = new Event();
        event.setName("Concurrency Test Book Fair " + runId);
        event.setLocation("Test Location");
        event.setEventDate(LocalDateTime.now().plusDays(30));
        event.setActive(true);
        event = eventRepository.save(event);

        Stall stall = new Stall();
        stall.setStallCode("TEST-" + runId);
        stall.setSize(StallSize.MEDIUM);
        stall.setPrice(new java.math.BigDecimal("10000.00"));
        stall.setBlocked(false);
        stall.setEvent(event);
        stall = stallRepository.save(stall);

        // 2. Setup 2 Vendor Users
        User vendorA = new User();
        vendorA.setName("Vendor A " + runId);
        vendorA.setEmail("vendorA_" + runId + "@concurrency.test");
        vendorA.setPhone("12345");
        vendorA.setRole(UserRole.STALL_VENDOR);
        vendorA.setSub("auth0|vendorA_sub_" + runId);
        vendorA.setActive(true);
        vendorA = userRepository.save(vendorA);

        User vendorB = new User();
        vendorB.setName("Vendor B " + runId);
        vendorB.setEmail("vendorB_" + runId + "@concurrency.test");
        vendorB.setPhone("54321");
        vendorB.setRole(UserRole.STALL_VENDOR);
        vendorB.setSub("auth0|vendorB_sub_" + runId);
        vendorB.setActive(true);
        vendorB = userRepository.save(vendorB);

        final Long eventId = event.getId();
        final Long stallId = stall.getId();
        final Long vendorAId = vendorA.getId();
        final Long vendorBId = vendorB.getId();

        // 3. Create concurrent booking request payloads
        CreateBookingRequest reqA = new CreateBookingRequest();
        reqA.setEventId(eventId);
        reqA.setStallIds(List.of(stallId));
        reqA.setGenreIds(List.of());
        reqA.setStallDescription("Vendor A Setup");
        reqA.setStallType(StallType.STANDARD);
        reqA.setPreferredStallSize(StallSize.MEDIUM);
        reqA.setNumberOfStallsRequired(1);
        reqA.setBusinessCategory(BusinessCategory.SERVICES);
        reqA.setReservationDate(LocalDate.now().plusDays(30));
        reqA.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        reqA.setAccountNumber("1111");
        reqA.setBankName("Test Bank");
        reqA.setAddress("Addr A");

        CreateBookingRequest reqB = new CreateBookingRequest();
        reqB.setEventId(eventId);
        reqB.setStallIds(List.of(stallId));
        reqB.setGenreIds(List.of());
        reqB.setStallDescription("Vendor B Setup");
        reqB.setStallType(StallType.STANDARD);
        reqB.setPreferredStallSize(StallSize.MEDIUM);
        reqB.setNumberOfStallsRequired(1);
        reqB.setBusinessCategory(BusinessCategory.SERVICES);
        reqB.setReservationDate(LocalDate.now().plusDays(30));
        reqB.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        reqB.setAccountNumber("2222");
        reqB.setBankName("Test Bank");
        reqB.setAddress("Addr B");

        // 4. Concurrent execution setup
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        List<Future<Object>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            startLatch.await(); // Sync threads starting line
            try {
                return reservationService.createPendingReservation(reqA, vendorAId);
            } catch (Exception e) {
                return e;
            } finally {
                finishLatch.countDown();
            }
        }));

        futures.add(executor.submit(() -> {
            startLatch.await(); // Sync threads starting line
            try {
                return reservationService.createPendingReservation(reqB, vendorBId);
            } catch (Exception e) {
                return e;
            } finally {
                finishLatch.countDown();
            }
        }));

        startLatch.countDown(); // Trigger simultaneous start
        finishLatch.await(); // Wait for both threads

        Object res1 = futures.get(0).get();
        Object res2 = futures.get(1).get();

        executor.shutdown();

        // 5. Verification
        int successCount = 0;
        int failureCount = 0;
        Exception failureException = null;

        if (res1 instanceof Reservation) successCount++;
        else if (res1 instanceof Exception) {
            failureCount++;
            failureException = (Exception) res1;
        }

        if (res2 instanceof Reservation) successCount++;
        else if (res2 instanceof Exception) {
            failureCount++;
            failureException = (Exception) res2;
        }

        // Verify exactly one booking succeeds and one fails
        assertEquals(1, successCount, "Exactly one booking must succeed");
        assertEquals(1, failureCount, "Exactly one booking must fail");

        assertNotNull(failureException, "Failure exception must not be null");
        assertTrue(failureException instanceof IllegalStateException, "Exception should be IllegalStateException");
        assertEquals("One or more selected stalls are no longer available.", failureException.getMessage());

        // Verify database state: only one reservation-stall mapping exists
        List<Long> bookedStallIds = reservationStallRepository.findBookedStallIdsByEventIdNoLock(eventId);
        assertEquals(1, bookedStallIds.size(), "Only one stall mapping should exist in database");
        assertEquals(stallId, bookedStallIds.get(0), "The booked stall ID must match");
    }
}
