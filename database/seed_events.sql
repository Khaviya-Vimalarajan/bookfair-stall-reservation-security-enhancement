USE `bookfair`;

-- Clear existing events/stalls to avoid duplicates
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `stalls`;
TRUNCATE TABLE `events`;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert Events
INSERT INTO `events` (`id`, `name`, `description`, `location`, `event_date`, `stalls_booked`, `image_url`, `active`, `cancellation_days`, `created_by_admin_id`, `created_at`, `updated_at`) VALUES
(1, 'Colombo International Book Fair 2026', 'The largest and most anticipated book fair in Sri Lanka, bringing together publishers, authors, and book lovers from across the globe.', 'BMICH, Colombo 07', '2026-10-15 09:00:00.000000', 0, 'https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?auto=format&fit=crop&q=80&w=800', 1, 7, NULL, NOW(), NOW()),
(2, 'Galle Literary Fair 2026', 'Celebrate literature, art, and music inside the historic Galle Fort. Engage in workshops, panel discussions, and local exhibitions.', 'Galle Fort, Galle', '2026-11-20 10:00:00.000000', 0, 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&q=80&w=800', 1, 7, NULL, NOW(), NOW()),
(3, 'Kandy Art & Book Fair 2026', 'A beautiful gathering highlighting art, culture, poetry, and publishing in the hills of Kandy.', 'Kandy City Centre, Kandy', '2026-12-05 09:30:00.000000', 0, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&q=80&w=800', 1, 7, NULL, NOW(), NOW()),
(4, 'Jaffna Book Fair 2026', 'Bringing readers and publishers together in the northern peninsula for cultural exchanges and book launches.', 'Jaffna Cultural Centre, Jaffna', '2026-12-20 09:00:00.000000', 0, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&q=80&w=800', 1, 7, NULL, NOW(), NOW());

-- Insert Stalls for Colombo (Event 1)
INSERT INTO `stalls` (`stall_code`, `stall_size`, `price`, `blocked`, `event_id`) VALUES
('A01', 'LARGE', 15000.00, 0, 1),
('A02', 'LARGE', 15000.00, 0, 1),
('B01', 'MEDIUM', 10000.00, 0, 1),
('B02', 'MEDIUM', 10000.00, 0, 1),
('C01', 'SMALL', 5000.00, 0, 1),
('C02', 'SMALL', 5000.00, 0, 1);

-- Insert Stalls for Galle (Event 2)
INSERT INTO `stalls` (`stall_code`, `stall_size`, `price`, `blocked`, `event_id`) VALUES
('G01', 'LARGE', 12000.00, 0, 2),
('G02', 'MEDIUM', 8000.00, 0, 2),
('G03', 'SMALL', 4000.00, 0, 2);

-- Insert Stalls for Kandy (Event 3)
INSERT INTO `stalls` (`stall_code`, `stall_size`, `price`, `blocked`, `event_id`) VALUES
('K01', 'LARGE', 10000.00, 0, 3),
('K02', 'MEDIUM', 6000.00, 0, 3),
('K03', 'SMALL', 3000.00, 0, 3);

-- Insert Stalls for Jaffna (Event 4)
INSERT INTO `stalls` (`stall_code`, `stall_size`, `price`, `blocked`, `event_id`) VALUES
('J01', 'LARGE', 10000.00, 0, 4),
('J02', 'MEDIUM', 7000.00, 0, 4),
('J03', 'SMALL', 3500.00, 0, 4);
