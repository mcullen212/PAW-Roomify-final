-- Image 1: Placeholder for Room 1
INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (1, 'paris_studio.jpg', 'image/jpeg', 102400, '0xDEADBEEF');

-- Image 2: Placeholder for Room 2
INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (2, 'ba_house.png', 'image/png', 512000, '0xCAFEBABE');

-- Image 3: New for new room
INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (3, 'ams_house.png', 'image/png', 512000, '0xAFFEBABE');

-- Images for amenity search tests
INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (4, 'amenity_wifi_pool.jpg', 'image/jpeg', 102400, '0xAAAA');

INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (5, 'amenity_heating.jpg', 'image/jpeg', 102400, '0xBBBB');

INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (6, 'amenity_wifi_only.jpg', 'image/jpeg', 102400, '0xCCCC');

INSERT INTO image (id, filename, content_type, size_bytes, data)
VALUES (7, 'amenity_ac.jpg', 'image/jpeg', 102400, '0xDDDD');

ALTER SEQUENCE image_id_seq RESTART WITH 100;

INSERT INTO AppUser (id, email, name, password, is_verified, locale, bio, travel_prefs)
VALUES (1, 'alice.owner@example.com', 'Alice Owner', 'hashed_pass_1', TRUE, 'en_US', 'Loves to host and travel light.', 'Solo, nature, budget');

INSERT INTO AppUser (id, email, name, password, is_verified, locale, bio, travel_prefs)
VALUES (2, 'bob.requester@example.com', 'Bob Requester', 'hashed_pass_2', TRUE, 'es_AR', 'Looking for a place to stay in Europe.', 'Groups, luxury, cities');

INSERT INTO AppUser (id, email, name, password, is_verified, locale, bio, travel_prefs)
VALUES (3, 'charlie.swapper@example.com', 'Charlie Swapper', 'hashed_pass_3', FALSE, 'fr_FR', NULL, NULL);

ALTER SEQUENCE appuser_id_seq RESTART WITH 100;

-- Room 1: Cozy Studio in Paris, Owner: Alice (id=1), Image id=1
INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id)
VALUES (1, 'Cozy Studio in Paris', 'A small, warm studio near the Eiffel Tower.', 'STUDIO', 'TWIN', TRUE, FALSE, '["Wifi", "Heating", "Essentials"]', 'Paris', 'France', 1, 1);

-- Room 2: Big House in Buenos Aires, Owner: Bob (id=2), Image id=2
INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id)
VALUES (2, 'Big House in Buenos Aires', 'Spacious house perfect for large groups.', 'HOME', 'KING', TRUE, TRUE, '["Wifi", "Pool", "Parking", "Kitchen"]', 'Buenos Aires', 'Argentina', 2, 2);

-- Rooms for amenity search tests
INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id, day_price)
VALUES (4, 'Amenity WiFi Pool Room', 'Room used for amenity filtering.', 'HOME', 'KING', TRUE, TRUE, '["WiFi", " Pool"]', 'Search City', 'Search Country', 1, 4, 100.00);

INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id, day_price)
VALUES (5, 'Amenity Heating Room', 'Room used for amenity filtering.', 'HOME', 'KING', TRUE, TRUE, '["Heating"]', 'Search City', 'Search Country', 1, 5, 100.00);

INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id, day_price)
VALUES (6, 'Amenity WiFi Only Room', 'Room used for amenity filtering.', 'HOME', 'KING', TRUE, TRUE, '["WiFi"]', 'Search City', 'Search Country', 1, 6, 100.00);

INSERT INTO room (id, title, description, room_type, bed_type, private_bathroom, private_kitchen, amenities, city, country, owner_id, image_id, day_price)
VALUES (7, 'Amenity AC Room', 'Room used for amenity filtering.', 'HOME', 'KING', TRUE, TRUE, '["WiFi", " Air Conditioning", " Pool"]', 'Search City', 'Search Country', 1, 7, 100.00);

ALTER SEQUENCE room_id_seq RESTART WITH 100;

-- Room 1: Available for all of March 2026
INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (1, 1, '2026-03-01', '2026-03-31');

-- Room 2: Available in January 2026
INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (2, 2, '2026-01-05', '2026-01-25');

-- Future availability for amenity search tests
INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (4, 4, '2099-01-01', '2099-01-31');

INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (5, 5, '2099-01-01', '2099-01-31');

INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (6, 6, '2099-01-01', '2099-01-31');

INSERT INTO room_availability (id, room_id, start_date, end_date)
VALUES (7, 7, '2099-01-01', '2099-01-31');

ALTER SEQUENCE room_availability_id_seq RESTART WITH 100;

-- GroupTrip 1: Owned by Alice (id=1). Spanning March to April 2026.
INSERT INTO group_trip (id, id_user, title, start_date, end_date)
VALUES (1, 1, 'European Tour', '2026-03-01', '2026-04-30');

-- GroupTrip 2: Owned by Bob (id=2). Spanning January 2026.
INSERT INTO group_trip (id, id_user, title, start_date, end_date)
VALUES (2, 2, 'South America Getaway', '2026-01-01', '2026-01-31');

ALTER SEQUENCE group_trip_id_seq RESTART WITH 100;

-- Trip 1: France segment of European Tour (GroupTrip 1). March 2026.
INSERT INTO trip (id, id_group_trip, country, start_date, end_date)
VALUES (1, 1, 'France', '2026-03-08', '2026-03-16');

-- Trip 2: Argentina segment of South America Getaway (GroupTrip 2). January 2026.
INSERT INTO trip (id, id_group_trip, country, start_date, end_date)
VALUES (2, 2, 'Argentina', '2026-01-12', '2026-01-22');

ALTER SEQUENCE trip_id_seq RESTART WITH 100;

-- Contact 1: Money Offer for Room 1 (Paris) by Bob (id=2). Status: PENDING.
-- Requested Range: 2026-03-10 to 2026-03-15 (within Room 1 availability)
INSERT INTO contact (id, requested_room_id, contact_date, status, is_swap, money_offer, offer_user_id, room_offer_id, requested_start_date, requested_end_date, offered_start_date, offered_end_date)
VALUES (1, 1, current_timestamp, 'PENDING', FALSE, 500.00, 2, NULL, '2026-03-10', '2026-03-15', NULL, NULL);

-- Contact 2: Swap Offer for Room 2 (Buenos Aires) by Charlie (id=3), offering Room 1. Status: ACCEPTED.
-- Requested Range: 2026-01-15 to 2026-01-20 (within Room 2 availability)
-- Offered Range: 2026-04-01 to 2026-04-05 (needs to be available for the offered room, Room 1)
INSERT INTO contact (id, requested_room_id, contact_date, status, is_swap, money_offer, offer_user_id, room_offer_id, requested_start_date, requested_end_date, offered_start_date, offered_end_date)
VALUES (2, 2, current_timestamp, 'ACCEPTED', TRUE, NULL, 3, 1, '2026-01-15', '2026-01-20', '2026-04-01', '2026-04-05');

INSERT INTO contact (id, requested_room_id, contact_date, status, is_swap, money_offer, offer_user_id, room_offer_id, requested_start_date, requested_end_date, offered_start_date, offered_end_date)
VALUES (3, 1, current_timestamp, 'ACCEPTED', FALSE, 500.00, 2, NULL, '2026-03-20', '2026-03-25', NULL, NULL);


ALTER SEQUENCE contact_id_seq RESTART WITH 100;

-- TripContact 1: Links Trip 1 (France) to Contact 1 (Money Offer for Room 1)
INSERT INTO trip_contact (id, trip_id, contact_id, room_id_involved)
VALUES (1, 1, 1, 1);

-- TripContact 2: Links Trip 2 (Argentina) to Contact 2 (Swap for Room 2)
INSERT INTO trip_contact (id, trip_id, contact_id, room_id_involved)
VALUES (2, 2, 2, 2);

ALTER SEQUENCE trip_contact_id_seq RESTART WITH 100;

-- Token 1: Password Reset token for Alice (ID 1), who is verified
INSERT INTO verification_token (id, user_id, token, type, expiry_date)
VALUES (1, 1, 'D5E6F7', 'RESET_PASSWORD', CURRENT_TIMESTAMP + INTERVAL '1' HOUR);

-- Token 3: Expired
INSERT INTO verification_token (id, user_id, token, type, expiry_date)
VALUES (2, 1, 'DDD6F7', 'RESET_PASSWORD', '2024-01-01 00:00:00');

ALTER SEQUENCE verification_token_id_seq RESTART WITH 100;

-- Review 1 (ID 1): Alice reviews Bob (Contact 1). Applies to Room 1. (Older, Higher)
INSERT INTO REVIEW (id, contact_id, reviewer_id, rating, comment, created_at)
VALUES (1, 3, 1, 5.0, 'Fantastic experience with Bob.', CURRENT_TIMESTAMP - INTERVAL '2' DAY);

-- Review 2 (ID 2): Bob reviews Alice (Contact 1). Applies to Room 1. (Newer, Lower)
-- Note: This is a second review for the same contact/room to test sorting/averaging.
INSERT INTO REVIEW (id, contact_id, reviewer_id, rating, comment, created_at)
VALUES (2, 3, 2, 3.0, 'Room was good, but neighborhood was loud.', CURRENT_TIMESTAMP - INTERVAL '1' DAY);

-- Review 3 (ID 3): Charlie reviews Bob (Contact 2). Applies to Room 2. (Single review for Room 2)
INSERT INTO REVIEW (id, contact_id, reviewer_id, rating, comment, created_at)
VALUES (3, 2, 3, 4.0, 'Great swap deal, Bob was responsive.', CURRENT_TIMESTAMP);

ALTER SEQUENCE review_id_seq RESTART WITH 100;
