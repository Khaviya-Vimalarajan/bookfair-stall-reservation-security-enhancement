package com.bookfair.Stall_Reservation.repository;

import com.bookfair.Stall_Reservation.entity.Event;
import com.bookfair.Stall_Reservation.entity.Stall;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface StallRepository extends JpaRepository<Stall, Long> {

    List<Stall> findByEventIdOrderByStallCode(Long eventId);

    Optional<Stall> findByEventIdAndStallCode(Long eventId, String stallCode);

    List<Stall> findByEventAndBlockedFalse(Event event);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stall s WHERE s.id IN :ids")
    List<Stall> findAllByIdForUpdate(List<Long> ids);
}
