CREATE TABLE IF NOT EXISTS AppUser (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password TEXT NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    locale  VARCHAR(10) DEFAULT 'en',
    bio TEXT,
    travel_prefs TEXT
);


CREATE TABLE IF NOT EXISTS IMAGE(
    id           SERIAL PRIMARY KEY,
    filename     TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes   INT  NOT NULL,
    data         BYTEA NOT NULL,
    owner_id     INT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_image_owner FOREIGN KEY (owner_id) REFERENCES AppUser(id)
);


CREATE TABLE IF NOT EXISTS ROOM(
    id SERIAL PRIMARY KEY,
    owner_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    room_type VARCHAR(20) NOT NULL,
    bed_type VARCHAR(20) NOT NULL,
    private_bathroom BOOLEAN DEFAULT FALSE,
    private_kitchen BOOLEAN DEFAULT FALSE,
    amenities JSONB,
    image_id INT,

    CONSTRAINT fk_room_user FOREIGN KEY (owner_id) REFERENCES AppUser(id),
    CONSTRAINT fk_room_image FOREIGN KEY (image_id) REFERENCES IMAGE(id) ON DELETE CASCADE

    );

CREATE TABLE IF NOT EXISTS ROOM_AVAILABILITY(
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    CONSTRAINT fk_availability_room FOREIGN KEY (room_id) REFERENCES ROOM(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS CONTACT (
    id SERIAL PRIMARY KEY,
    requested_room_id INT NOT NULL, -- SWAP ASKED FIRST INTERACTION (THE ONE OF THE POST, MAIN ROOM)
    is_swap BOOLEAN NOT NULL,
    money_offer NUMERIC(10,2),
    offer_user_id INT, -- PERSON WHO STARTED SWAP
    room_offer_id INT, -- IF ROOM WAS OFFERED FOR SWAP
    requested_start_date DATE NOT NULL, -- MAIN ROOM START DATE
    requested_end_date DATE NOT NULL, -- MAIN ROOM END DATE
    offered_start_date DATE, -- DATE SELECTED FOR OFFERED ROOM
    offered_end_date DATE, -- DATE SELECTED FOR OFFERED ROOM -- PERSON WHO HAS CONTROL OF ACCEPT
    contact_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_trip INT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT fk_contact_user
    FOREIGN KEY (offer_user_id) REFERENCES AppUser(id) ON DELETE CASCADE,

    CONSTRAINT fk_contact_room_requested
    FOREIGN KEY (requested_room_id) REFERENCES ROOM(id) ON DELETE CASCADE,

    CONSTRAINT fk_contact_room_offered
    FOREIGN KEY (room_offer_id) REFERENCES ROOM(id) ON DELETE CASCADE,

    CONSTRAINT fk_contact_trip
    FOREIGN KEY (id_trip) REFERENCES TRIP(id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS verification_token (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(6) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, --'EMAIL_VERIFICATION', 'PASSWORD_RESET'
    expiry_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS review (
    id SERIAL PRIMARY KEY,
    contact_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    rating NUMERIC(5,2),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_contact FOREIGN KEY (contact_id) REFERENCES contact(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (reviewer_id) REFERENCES AppUser(id)
    );

CREATE TABLE IF NOT EXISTS GROUP_TRIP(
    id SERIAL PRIMARY KEY,
    id_user INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    CONSTRAINT fk_group_trip_user FOREIGN KEY (id_user) REFERENCES AppUser(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS TRIP(
     id SERIAL PRIMARY KEY,
     id_group_trip INT NOT NULL,
     country VARCHAR(100) NOT NULL,
     start_date DATE NOT NULL,
     end_date DATE NOT NULL,

     CONSTRAINT fk_trip_group_trip FOREIGN KEY (id_group_trip) REFERENCES GROUP_TRIP(id) ON DELETE CASCADE
);
