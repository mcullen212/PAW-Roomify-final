package ar.edu.itba.paw.service.rooms;

import ar.edu.itba.paw.interfaces.exceptions.ForbiddenUserOperationException;
import ar.edu.itba.paw.interfaces.exceptions.RoomHasActiveSwapsException;
import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.RoomValidationException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ImageDTO;
import ar.edu.itba.paw.model.DTO.RoomCardResult;
import ar.edu.itba.paw.model.DTO.RoomCreateRequest;
import ar.edu.itba.paw.model.DTO.RoomCreationResult;
import ar.edu.itba.paw.model.DTO.RoomReviewStats;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.*;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RoomServiceImpl implements RoomService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomServiceImpl.class);
    private final RoomDao roomDao;
    private final UserService userService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final ImageService imageService;
    private final ContactDao contactDao;
    private final ReviewDao reviewDao;
    private final EmailService emailService;

    @Autowired
    public RoomServiceImpl(RoomDao roomDao, UserService userService,
                           RoomAvailabilityService roomAvailabilityService, ImageService imageService,
                           ContactDao contactDao, ReviewDao reviewDao, EmailService emailService) {
        this.roomDao = roomDao;
        this.userService = userService;
        this.roomAvailabilityService = roomAvailabilityService;
        this.imageService = imageService;
        this.contactDao = contactDao;
        this.reviewDao = reviewDao;
        this.emailService = emailService;
    }


    private String amenitiesJson(List<Amenity> amenities) {
        return amenities != null
                ? amenities.stream()
                .map(Amenity::toString)
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", ", "[", "]"))
                : "[]";
    }

    @Override
    @Transactional
    public Room createRoomAndAvailability(String email, String title, String country, String city, String description,
                                          String roomType, String bedType, Boolean privateBathroom, Boolean privateKitchen,
                                          List<String> amenities, ImageDTO[] roomImage, List<DateRange> dateRanges, BigDecimal dayPrice) {
        Image image = uploadLegacyRoomImage(email, roomImage);

        return createRoomWithAvailability(email, new RoomCreateRequest(
                title,
                country,
                city,
                description,
                roomType,
                bedType,
                privateBathroom,
                privateKitchen,
                amenities,
                dateRanges,
                dayPrice,
                image.getId()
        )).getRoom();
    }

    private RoomType parseRoomType(String roomType) {
        if (roomType == null || roomType.isBlank()) {
            throw new RoomValidationException("Room type is required", "room.roomType.notNull");
        }
        try {
            return RoomType.valueOf(roomType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RoomValidationException("Invalid room type", "room.roomType.invalid");
        }
    }

    private BedType parseBedType(String bedType) {
        if (bedType == null || bedType.isBlank()) {
            throw new RoomValidationException("Bed type is required", "room.bedType.notNull");
        }
        try {
            return BedType.valueOf(bedType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RoomValidationException("Invalid bed type", "room.bedType.invalid");
        }
    }

    private List<Amenity> parseAmenities(List<String> amenities) {
        if (amenities == null) {
            return Collections.emptyList();
        }
        try {
            return amenities.stream()
                    .filter(Objects::nonNull)
                    .map(Amenity::parse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RoomValidationException("Invalid amenity", "validation.amenity.invalid");
        }
    }

    @Override
    @Transactional
    public Room createRoomAndAvailability(String email, String title, String country, String city, String description,
                                          RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen,
                                          List<Amenity> amenities, ImageDTO[] roomImage, List<DateRange> dateRanges,BigDecimal dayPrice) {
        List<String> amenityNames = amenities == null
                ? Collections.emptyList()
                : amenities.stream().map(Amenity::name).collect(Collectors.toList());

        Image image = uploadLegacyRoomImage(email, roomImage);

        return createRoomWithAvailability(email, new RoomCreateRequest(
                title,
                country,
                city,
                description,
                roomType == null ? null : roomType.name(),
                bedType == null ? null : bedType.name(),
                privateBathroom,
                privateKitchen,
                amenityNames,
                dateRanges,
                dayPrice,
                image.getId()
        )).getRoom();
    }

    @Override
    @Transactional
    public RoomCreationResult createRoomWithAvailability(String email, RoomCreateRequest request) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return createRoomWithAvailability(user.getId(), request);
    }

    @Override
    @Transactional
    public RoomCreationResult createRoomWithAvailability(RoomCreateRequest request) {
        if (request == null) {
            throw new RoomValidationException("Room data is required", "room.data.required");
        }
        if (request.getUserId() == null) {
            throw new RoomValidationException("User is required", "room.user.required");
        }
        return createRoomWithAvailability(request.getUserId(), request);
    }

    @Override
    @Transactional
    public RoomCreationResult createRoomWithAvailability(long ownerId, RoomCreateRequest request) {
        if (request == null) {
            throw new RoomValidationException("Room data is required", "room.data.required");
        }

        Long imageId = request.getImageId();
        try {
            RoomType roomType = parseRoomType(request.getRoomType());
            BedType bedType = parseBedType(request.getBedType());
            boolean privateBathroom = Boolean.TRUE.equals(request.getPrivateBathroom());
            boolean privateKitchen = Boolean.TRUE.equals(request.getPrivateKitchen());
            List<Amenity> amenities = parseAmenities(request.getAmenities());
            List<DateRange> dateRanges = request.getDateRanges();

            validateRoomCreation(roomType, privateBathroom, privateKitchen, imageId, dateRanges, request.getDayPrice());

            User user = userService.findUserById(ownerId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Image image = resolveOwnedUnassignedImage(imageId, user.getId());

            String attrsJson = amenitiesJson(amenities);

            Room room = roomDao.create(
                    user.getId(), request.getTitle(), request.getCountry(), request.getCity(), request.getDescription(),
                    roomType, bedType,
                    privateBathroom, privateKitchen, attrsJson, image.getId(), request.getDayPrice()
            );

            List<RoomAvailability> availabilities = new ArrayList<>();
            for (DateRange range : dateRanges) {
                availabilities.add(roomAvailabilityService.addAvailability(room.getId(), range));
            }

            return new RoomCreationResult(room, availabilities);
        } catch (RuntimeException e) {
            if (imageId != null) {
                cleanupOwnedUnassignedImage(ownerId, imageId);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteRoom(long roomId) {
        Room room = roomDao.findRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (contactDao.hasActiveContacts(roomId)) {
            throw new RoomHasActiveSwapsException(roomId);
        }
        handlePendingContactsBeforeRoomDeletion(roomId);
        Long imageId = room.getImageId();
        if (!roomDao.deleteRoom(roomId)) {
            throw new IllegalStateException("Room could not be deleted");
        }
        if (imageId != null) {
            imageService.deleteImage(imageId);
        }
    }

    @Override
    @Transactional
    public Room updateRoom(long roomId, String title, String description, List<String> amenities, BigDecimal dayPrice) {
        roomDao.findRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        validateRoomUpdate(title, dayPrice);
        List<Amenity> parsedAmenities = parseAmenities(amenities);
        String attrsJson = amenitiesJson(parsedAmenities);
        if (!roomDao.updateRoom(roomId, title, description, attrsJson, dayPrice)) {
            throw new IllegalStateException("Room could not be updated");
        }
        return roomDao.findRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private void handlePendingContactsBeforeRoomDeletion(long roomId) {
        List<Contact> pendingContacts = contactDao.findPendingContactsForRoom(roomId);

        for (Contact contact : pendingContacts) {
            User recipient = pendingContactDeletionRecipient(contact, roomId);
            Room deletedRoom = contact.getRoomRequested().getId() == roomId
                    ? contact.getRoomRequested()
                    : contact.getRoomOffered();

            if (recipient != null && recipient.getId() != deletedRoom.getOwner().getId()) {
                emailService.sendPendingContactRoomDeletedNotification(
                        recipient,
                        deletedRoom.getOwner().getName(),
                        deletedRoom.getOwner().getEmail(),
                        deletedRoom.getTitle(),
                        deletedRoom.getCountry()
                );
            }
        }

        contactDao.deletePendingContactsForRoom(roomId);
    }

    private User pendingContactDeletionRecipient(Contact contact, long roomId) {
        if (contact.getRoomRequested().getId() == roomId) {
            return contact.getOfferUser();
        }
        return contact.getRoomRequested().getOwner();
    }

    private void validateRoomUpdate(String title, BigDecimal dayPrice) {
        if (title == null || title.isBlank()) {
            throw new RoomValidationException("Title is required", "room.title.notBlank");
        }
        if (dayPrice == null || dayPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RoomValidationException("Day price must be greater than zero", "room.dayPrice.min");
        }
    }

    private Image uploadLegacyRoomImage(String email, ImageDTO[] roomImages) {
        if (roomImages == null || roomImages.length == 0 || roomImages[0] == null) {
            throw new RoomValidationException("Image is required", "room.image.notNull");
        }
        return imageService.uploadRoomImage(email, roomImages[0]);
    }

    private Image resolveOwnedUnassignedImage(long imageId, long ownerId) {
        Optional<Image> image = imageService.findOwnedUnassignedImage(imageId, ownerId);
        if (image.isPresent()) {
            return image.get();
        }

        Image existingImage = imageService.findImageById(imageId)
                .orElseThrow(() -> new RoomValidationException("Image not found", "room.image.notFound"));
        if (existingImage.getOwner() == null || existingImage.getOwner().getId() != ownerId) {
            throw new ForbiddenUserOperationException("Image does not belong to user");
        }
        throw new RoomValidationException("Image is already associated with a room", "room.image.alreadyAssociated");
    }

    private void cleanupOwnedUnassignedImage(long ownerId, long imageId) {
        imageService.findImageById(imageId)
                .filter(image -> image.getOwner() != null && image.getOwner().getId() == ownerId)
                .ifPresent(image -> imageService.deleteIfUnassigned(imageId));
    }

    private void validateRoomCreation(RoomType roomType, boolean privateBathroom, boolean privateKitchen,
                                      Long imageId, List<DateRange> dateRanges, BigDecimal dayPrice) {
        if (roomType == RoomType.STUDIO && (!privateKitchen || !privateBathroom)) {
            throw new RoomValidationException("Studio rooms must have private bathroom and private kitchen", "room.studio.missing");
        }
        if (imageId == null) {
            throw new RoomValidationException("Image is required", "room.image.notNull");
        }
        if (dayPrice == null || dayPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RoomValidationException("Day price must be greater than zero", "room.dayPrice.min");
        }
        if (dateRanges == null || dateRanges.isEmpty()) {
            throw new RoomValidationException("Availability is required", "room.availability.required");
        }

        List<DateRange> sortedRanges = new ArrayList<>();
        for (DateRange range : dateRanges) {
            if (range == null || range.getStartDate() == null || range.getEndDate() == null) {
                throw new RoomValidationException("You must insert starting and ending dates", "room.availability.incomplete");
            }
            if (!range.getStartDate().isBefore(range.getEndDate())) {
                throw new RoomValidationException("Start date must be before end date", "room.error.endDate");
            }
            if (range.getStartDate().isBefore(LocalDate.now())) {
                throw new RoomValidationException("Start date cannot be in the past", "room.error.startDate");
            }
            sortedRanges.add(range);
        }

        sortedRanges.sort(Comparator.comparing(DateRange::getStartDate));
        for (int i = 1; i < sortedRanges.size(); i++) {
            DateRange previous = sortedRanges.get(i - 1);
            DateRange current = sortedRanges.get(i);
            if (!current.getStartDate().isAfter(previous.getEndDate())) {
                throw new RoomValidationException("Availability ranges cannot overlap", "room.error.overlappingDates");
            }
        }
    }

    @Override
    public List<String> validateRoom(long ownerId, RoomType roomType, BedType bedType,
                                     boolean privateBathroom, boolean privateKitchen) {
        List<String> errors = new ArrayList<>(); //PREGUNTAR

        if (userService.findUserById(ownerId).isEmpty()) {
            errors.add("room.owner.notFound");
        }
        if (roomType == RoomType.STUDIO && (!privateKitchen || !privateBathroom)) {
            errors.add("room.studio.missing");
        }
        return errors;
    }

    @Override
    public Optional<Room> findRoomById(long id) {
        return roomDao.findRoomById(id);
    }

    @Override
    public long getOwnerId(long id) {
        return findRoomById(id).orElseThrow(() -> new IllegalArgumentException("Room not found with id " + id)).getOwner().getId();
    }

    @Override
    public boolean isOwner(String email, long id){
        Room room = findRoomById(id).orElseThrow(() -> new IllegalArgumentException("Room not found with id " + id));
        User owner = userService.findUserByEmail(email).orElseThrow(()->new IllegalArgumentException("User not found with email " + email));
        return room.getOwner().getId() == owner.getId();
    }

    @Override
    public List<Room> getRoomsPaginated(int page, int pageSize) {
        if (page < 1) page = 1;
        return roomDao.getRoomsPaginated(page, pageSize);
    }

    @Override
    public int getTotalPages(int pageSize) {
        int totalRooms = roomDao.countRooms();
        return (int) Math.ceil((double) totalRooms / pageSize);
    }

    @Override
    public RoomSearchCriteria buildSearchCriteria(String destination, String checkIn, String checkOut,
                                                  String roomType, String bedType, Boolean privateBathroom,
                                                  Boolean privateKitchen, Long ownerId, List<String> amenities) {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setDestination(destination);
        criteria.setPrivateBathroom(privateBathroom);
        criteria.setPrivateKitchen(privateKitchen);
        criteria.setOwnerId(ownerId);
        criteria.setAmenities(parseAmenities(amenities));

        if (checkIn != null && !checkIn.isBlank()) {
            criteria.setCheckIn(LocalDate.parse(checkIn));
        }
        if (checkOut != null && !checkOut.isBlank()) {
            criteria.setCheckOut(LocalDate.parse(checkOut));
        }
        if (roomType != null && !roomType.isBlank()) {
            criteria.setRoomType(RoomType.valueOf(roomType.toUpperCase(Locale.ROOT)));
        }
        if (bedType != null && !bedType.isBlank()) {
            criteria.setBedType(BedType.valueOf(bedType.toUpperCase(Locale.ROOT)));
        }

        return criteria;
    }

    @Override
    public List<Room> searchRooms(RoomSearchCriteria criteria, int page, int pageSize) {
        return roomDao.searchRooms(criteria, page, pageSize);
    }

    @Override
    public List<RoomCardResult> searchRoomCards(RoomSearchCriteria criteria, int page, int pageSize) {
        List<Room> rooms = searchRooms(criteria, page, pageSize);
        List<Long> roomIds = rooms.stream()
                .map(Room::getId)
                .collect(Collectors.toList());
        Map<Long, RoomReviewStats> statsByRoomId = reviewDao.getReviewStatsByRoomIds(roomIds);

        return rooms.stream()
                .map(room -> new RoomCardResult(room, statsByRoomId.get(room.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public int countSearchRooms(RoomSearchCriteria criteria) {
        return roomDao.countSearchRooms(criteria);
    }

    @Override
    public List<Room> findRoomsByOwnerEmailPaging(String email, int page, int pageSize){
        long ownerId = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return roomDao.findRoomsByOwnerIdPaging(ownerId, page, pageSize);
    }

    @Override
    public List<Room> findRoomsByOwnerEmailPagingSwap(String email, int page, int pageSize){
        long ownerId = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return roomDao.findRoomsByOwnerIdPagingForSwap(ownerId, page, pageSize);
    }


    public boolean checkBothDates(LocalDate checkIn, LocalDate checkOut) {
        return ((checkIn == null) ^ (checkOut == null));
    }

    @Override
    public int countRoomsByOwnerEmail(String email){
        long ownerId = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return roomDao.countRoomsByOwnerId(ownerId);
    }

    @Override
    public int countRoomsByOwnerEmailSwap(String email){
        long ownerId = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return roomDao.countRoomsByOwnerIdForSwap(ownerId);
    }

    @Override
    public List<Room> findRoomsByOwnerId(long id, int page, int pageSize){
        return roomDao.findRoomsByOwnerIdPaging(id, page, pageSize);
    }

    @Override
    public int countRoomsByOwnerId(long ownerId) {
        return roomDao.countRoomsByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> filterSuggestedRoomsForTrip(List<Room> suggestedRooms, List<TripContact> currentTripContacts, Trip trip, User loggedUser) {

        // IDs de los rooms ya asociados al trip
        List<Long> currentRoomIds = currentTripContacts.stream().map(tc -> { Contact c = tc.getContact();
                    Room room = (c.getRoomRequested().getOwner().getId() == loggedUser.getId()) ? c.getRoomOffered() : c.getRoomRequested();
                    return room.getId();
                }).distinct().collect(Collectors.toList());

        // Filtramos: si no está asociado, pasa. Si está, revisamos si tiene disponibilidad adicional.
        return suggestedRooms.stream()
                .filter(room -> !currentRoomIds.contains(room.getId()) || hasExtraAvailability(room, currentTripContacts, trip, loggedUser))
                .toList();
    }
    private boolean hasExtraAvailability(Room room, List<TripContact> currentTripContacts, Trip trip, User loggedUser) {
        List<RoomAvailability> availabilities = roomAvailabilityService.getAvailabilities(room.getId());

        LocalDate tripStart = trip.getDateRange().getStartDate();
        LocalDate tripEnd = trip.getDateRange().getEndDate();

        // Rango(s) ya reservado(s) de este room dentro del trip
        List<DateRange> bookedRanges = currentTripContacts.stream()
                .filter(tc -> {
                    Contact c = tc.getContact();
                    Room bookedRoom = (c.getRoomRequested().getOwner().getId() == loggedUser.getId() ) ? c.getRoomOffered() : c.getRoomRequested();
                    return bookedRoom.getId() == room.getId();
                })
                .map(tc -> {Contact c = tc.getContact();
                    return (c.getRoomRequested().getOwner().getId() == loggedUser.getId()) ? c.getOfferedRange() : c.getRequestedRange();
                }).filter(Objects::nonNull).toList();

        // Por cada disponibilidad del room
        for (RoomAvailability av : availabilities) {
            // Solo consideramos disponibilidades dentro del rango del trip
            if (av.getRange().getEndDate().isBefore(tripStart) || av.getRange().getStartDate().isAfter(tripEnd))
                continue;
            // Chequear si hay al menos un día dentro del trip no cubierto por reservas
            LocalDate start = av.getRange().getStartDate().isBefore(tripStart) ? tripStart : av.getRange().getStartDate();
            LocalDate end = av.getRange().getEndDate().isAfter(tripEnd) ? tripEnd : av.getRange().getEndDate();

            boolean dayFree = start.datesUntil(end.plusDays(1))
                    .anyMatch(d -> bookedRanges.stream().noneMatch(br -> !d.isBefore(br.getStartDate()) && !d.isAfter(br.getEndDate())));

            if (dayFree)
                return true; // tiene al menos un día libre dentro del trip
        }
        return false;
    }


}
