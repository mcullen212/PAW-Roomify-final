package ar.edu.itba.paw.service.contacts;

import ar.edu.itba.paw.interfaces.exceptions.*;
import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.interfaces.persistence.TripContactDao;
import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ContactPage;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.ContactView;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;
import ar.edu.itba.paw.service.reviews.ReviewEligibilityService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    private final ContactDao contactDao;
    private final TripContactDao tripContactDao;
    private final UserService userService;
    private final RoomService roomService;
    private final EmailService emailService;
    private final RoomAvailabilityService availabilityService;
    private final TripService tripService;
    private final ReviewEligibilityService reviewEligibilityService;


    public ContactServiceImpl(final ContactDao contactDao, final TripContactDao tripContactDao, final UserService userService, final RoomService roomService, EmailService emailService,
                              RoomAvailabilityService availabilityService, TripService tripService,
                              ReviewEligibilityService reviewEligibilityService) {
        this.contactDao = contactDao;
        this.tripContactDao = tripContactDao;
        this.userService = userService;
        this.roomService = roomService;
        this.emailService = emailService;
        this.availabilityService = availabilityService;
        this.tripService = tripService;
        this.reviewEligibilityService = reviewEligibilityService;
    }


    @Transactional
    @Override
    public Contact createContact(long roomRequestedId,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 boolean isSwap,
                                 BigDecimal moneyOffer,
                                 String username,
                                 Long roomOfferedId) {

        User user = userService.findUserByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Room roomRequested = roomService.findRoomById(roomRequestedId)
                .orElseThrow(() -> new RoomNotFoundException(roomRequestedId));

        long ownerId = roomRequested.getOwner().getId();

        LocalDate today = LocalDate.now();

        if(startDate == null || endDate == null){
            throw new BookedDateException("Please select start date and end date.","specific.errors.swap.null.dates",roomRequestedId);
        }
        if (!endDate.isAfter(startDate)) {
            throw new BookedDateException("End date must be after start date.", "room.error.endDate", roomRequestedId);
        }
        if (startDate.isBefore(today)) {
            throw new BookedDateException("Start date must be today or later.","specific.errors.date.startBeforeToday",roomRequestedId);
        }

        if(!isSwap){
            if (moneyOffer == null || moneyOffer.compareTo(BigDecimal.ZERO) <= 0) {
                throw new SwapException("Money offer is null or negative value", "specific.errors.moneyOffer.required", roomRequestedId);
            }
            if (contactDao.replySameSwapMoney(roomRequestedId, startDate, endDate, moneyOffer)) {
                throw new BookedDateException("You can't make the exact same swap.", "specific.errors.same.room.swap", roomRequestedId);
            }
        }
        DateRange dateRangeRequested = new DateRange(startDate, endDate);

        if(!availabilityService.inAvailabilityRanges(roomRequestedId, startDate, endDate)){
            throw new BookedDateException("The room selected has no availability for these dates","specific.errors.room.outOfAvailabilityRange",roomRequestedId);
        }

            if(contactDao.hasAcceptedContactInRangeRequestedSide(roomRequestedId, dateRangeRequested) || contactDao.hasAcceptedContactInRangeOfferedSide(roomRequestedId, dateRangeRequested)){
            throw new BookedDateException("This room is already booked on this date.","specific.errors.room.alreadyBooked",roomRequestedId);
        }

        if (user.getId() == ownerId) {
            throw new BookedDateException("You cannot book your own room.","specific.errors.cannotBookOwnRoom", roomRequestedId);
        }

        LocalDateTime now = LocalDateTime.now();

        Room roomOffered = null;

        if(isSwap){

            if(roomOfferedId == null){
                throw new SwapException("You must offer a room when swapping", "specific.errors.room.swap.room.offer.null", roomRequestedId);
            }

            if(contactDao.replySameSwap(roomRequestedId, startDate, endDate, roomOfferedId)){
                throw new BookedDateException("You can't make the exact same swap.", "specific.errors.same.room.swap", roomRequestedId);
            }

            roomOffered = roomService.findRoomById(roomOfferedId)
                    .orElseThrow(() -> new RoomNotFoundException(roomOfferedId));

            if (roomOffered.getOwner().getId() != user.getId()) {
                throw new SwapException("You can only offer one of your own rooms.", "specific.errors.room.swap.room.offer.notOwner", roomOfferedId);
            }

            long daysRequested = ChronoUnit.DAYS.between(startDate, endDate);
            int page = 1;
            int pageSize = 12;
            List<DateRange> acceptedDatesRequested = contactDao.contactAcceptedDatesForRoom(roomOfferedId, page, pageSize);
            List<DateRange> acceptedDatesOffered = contactDao.contactAcceptedDatesForRoomOffered(roomOfferedId, page, pageSize);
            DateRange offeredAvailabilityRange = availabilityService.getAvailabilityDatesForRoom(roomOfferedId);

            acceptedDatesRequested.addAll(acceptedDatesOffered);
            long maxDaysAvailable = datesAvailable(acceptedDatesRequested,offeredAvailabilityRange);


            if(daysRequested > maxDaysAvailable){
                throw new BookedDateException("You cannot book a room for more consecutive days than the availability you have.","specific.errors.swap.daysExceeded", roomRequestedId);
            }
        }

        return contactDao.create( user.getId(), roomRequested, roomOffered, now, SwapStatus.PENDING, isSwap, moneyOffer, dateRangeRequested, null);
    }

    @Override
    public List<Contact> upcomingTrips(String email, LocalDate today, int page, int pageSize) {
        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return contactDao.findUpcomingTripsByUserId(user.getId(), today, page, pageSize);
    }

    @Override
    public List<Boolean> upcomingTripsCanBeCancelled(String email, LocalDate today, int page, int pageSize){
        List<Contact> upcomingTrips = upcomingTrips(email, today, page, pageSize);
        return cancelUpComingTrips(upcomingTrips, today);
    }

    private List<Boolean> cancelUpComingTrips(List<Contact> upcomingTrips, LocalDate today) {
        List<Boolean> result = new ArrayList<>(upcomingTrips.size());
        for (Contact trip : upcomingTrips) {
            LocalDate earliestStart = earliestStartDate(trip);
            result.add(hasMoreThanAWeek(today, earliestStart));
        }
        return result;
    }

    @Override
    public List<String> upcomingUsersOffering(String email, LocalDate today, int page, int pageSize) {
        List<Contact> trips = upcomingTrips(email, today, page, pageSize);
        return getOfferUsers(trips);
    }

    @Override
    public int countUpcomingTrips(String username, LocalDate today){
        User user = userService.findUserByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return contactDao.countUpcomingTripsByUserId(user.getId(), today);
    }

    @Override
    public List<Contact> receivedSwaps(String email, int page, int pageSize) {
        final User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return contactDao.findPendingReceivedByUserPage(user.getId(), page, pageSize);
    }

    @Override
    public List<String> receivedUsersOffering(String username, int page, int pageSize) {
        final List<Contact> content = receivedSwaps(username, page, pageSize);

        final List<String> usersOffering = new ArrayList<>(content.size());
        for (final Contact c : content) {
            if (c.getOfferUser() != null && c.getOfferUser().getEmail() != null) {
                usersOffering.add(c.getOfferUser().getEmail());
            } else {
                usersOffering.add("—");
            }
        }
        return usersOffering;
    }


    @Override
    public long countReceivedSwaps(final String email) {
        final User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return contactDao.countPendingReceivedUser(user.getId());
    }

    @Override
    public List<Contact> pastTrips(String email, LocalDate today, int page, int pageSize) {
        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return contactDao.findPastTripsByEmail(user.getId(), today, page, pageSize);
    }

    private List<String> getOfferUsers(List<Contact> trips) {
        List<String> usersOffering = new ArrayList<>();
        for(Contact trip : trips){
            String username = trip.getOfferUser().getName();
            usersOffering.add(username);
        }
        return usersOffering;
    }

    @Override
    public List<String> pastUsersOffering(String email, LocalDate today, int page, int pageSize) {
        List<Contact> swaps = pastTrips(email, today, page, pageSize);
        return getOfferUsers(swaps);
    }

    private long datesAvailable(List<DateRange> datesAccepted, DateRange offeredAvailability){
        LocalDate cursor = offeredAvailability.getStartDate();
        long maxFreeDays = 0;

        if(datesAccepted.isEmpty()){
            return ChronoUnit.DAYS.between(offeredAvailability.getStartDate(), offeredAvailability.getEndDate());
        }

        // ordenar las fechas ocupadas
        datesAccepted.sort(Comparator.comparing(DateRange::getStartDate));

        for (DateRange busy : datesAccepted) {
            if (busy.getStartDate().isAfter(cursor)) {
                long gap = ChronoUnit.DAYS.between(cursor, busy.getStartDate());
                maxFreeDays = Math.max(maxFreeDays, gap);
            }

                cursor = busy.getEndDate().plusDays(1);
        }

        // también considerar el espacio libre al final
        if (cursor.isBefore(offeredAvailability.getEndDate())) {
            long gap = ChronoUnit.DAYS.between(cursor, offeredAvailability.getEndDate());
            maxFreeDays = Math.max(maxFreeDays, gap);
        }

        return maxFreeDays;
    }

    @Override
    @Transactional
    public Contact acceptedContact(long contactId, LocalDate startDate, LocalDate endDate){
        Contact contact = contactDao.findContactById(contactId).orElseThrow(() -> new ContactNotFoundException(contactId));

        if(!contact.getStatus().equals(SwapStatus.PENDING)){
            throw new InvalidContactStateException("This contact is not pending");
        }
        expirePendingContactIfNeeded(contact, LocalDate.now());

        return acceptPendingContact(contact, startDate, endDate);
    }

    @Override
    @Transactional
    public Contact updateContactStatus(long contactId, SwapStatus status, LocalDate checkIn, LocalDate checkOut, String email) {
        if (status == null || (status != SwapStatus.ACCEPTED && status != SwapStatus.REJECTED && status != SwapStatus.CANCELED)) {
            throw new InvalidContactUpdateException("Only ACCEPTED, REJECTED or CANCELED are valid contact updates");
        }

        Contact contact = contactDao.findContactById(contactId).orElseThrow(() -> new ContactNotFoundException(contactId));
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (status == SwapStatus.CANCELED) {
            validateNoDates(checkIn, checkOut);
            return cancelPendingOrAcceptedContact(contact, user, LocalDate.now());
        }

        if (contact.getRoomRequested().getOwner().getId() != user.getId()) {
            throw new ForbiddenUserOperationException("Only the requested room owner can accept or reject this contact");
        }
        if (contact.getStatus() != SwapStatus.PENDING) {
            throw new InvalidContactStateException("This contact is not pending");
        }
        expirePendingContactIfNeeded(contact, LocalDate.now());

        if (status == SwapStatus.REJECTED) {
            validateNoDates(checkIn, checkOut);
            return rejectPendingContact(contact);
        }

        return acceptPendingContact(contact, checkIn, checkOut);
    }

    private Contact cancelPendingOrAcceptedContact(Contact contact, User cancellingUser, LocalDate date) {
        if (!belongsToContact(cancellingUser, contact)) {
            throw new ForbiddenUserOperationException("Only contact participants can cancel this contact");
        }
        if (contact.getStatus() != SwapStatus.ACCEPTED && contact.getStatus() != SwapStatus.PENDING) {
            throw new InvalidContactStateException("Only accepted or pending contacts can be canceled");
        }
        expirePendingContactIfNeeded(contact, date);
        if(contact.getStatus() == SwapStatus.ACCEPTED){
            LocalDate firstDate = earliestStartDate(contact);
            boolean canCancel = hasMoreThanAWeek(date, firstDate);

            if (!canCancel) {
                throw new CancelException("Cancellations can only be made more than 7 days in advance.");
            }
        }

        contactDao.cancelSwap(contact.getId());
        User userToNotify = getUserToNotify(contact, cancellingUser);

        if (userToNotify != null) {
            emailService.sendSwapCancellationNotification(
                    userToNotify,
                    cancellingUser,
                    contact.getRoomRequested(),
                    contact.getStatus()
            );
        }

        contact.setStatus(SwapStatus.CANCELED);
        return contact;
    }

    private void expirePendingContactIfNeeded(Contact contact, LocalDate today) {
        if (contact.getStatus() == SwapStatus.PENDING
                && contact.getRequestedRange() != null
                && contact.getRequestedRange().getStartDate().isBefore(today)) {
            contactDao.updateStatus(contact.getId(), SwapStatus.EXPIRED);
            contact.setStatus(SwapStatus.EXPIRED);
            throw new InvalidContactStateException("This contact has expired");
        }
    }

    private boolean belongsToContact(User user, Contact contact) {
        return user.getId() == contact.getRoomRequested().getOwner().getId()
                || (contact.getOfferUser() != null && user.getId() == contact.getOfferUser().getId());
    }

    private Contact acceptPendingContact(Contact contact, LocalDate startDate, LocalDate endDate) {
        final Contact updatedContact;

        if(contact.isSwap()){
            validateSwapAcceptanceDates(contact, startDate, endDate);
            DateRange newRange = new DateRange(startDate, endDate);

            if(getStayDurationDays(contact.getId()) < ChronoUnit.DAYS.between(startDate, endDate)){
                throw new MaxDaysExceededException("Booking more days than allowed.", "swap.date.range.exceeded", contact.getId(), contact.getRoomOffered().getId());
            }

            if(contactDao.hasAcceptedContactInRangeRequestedSide(contact.getRoomOffered().getId(), newRange) || contactDao.hasAcceptedContactInRangeOfferedSide(contact.getRoomOffered().getId(), newRange)){
                throw new BookedDateException("Dates selected are already booked.","swap.room.already.booked", contact.getId(), contact.getRoomOffered().getId());
            }

            updatedContact = contactDao.confirmedDateRange(contact.getId(), newRange);
        } else{
            validateNoDates(startDate, endDate);
            updatedContact = contactDao.acceptMoneyOffer(contact.getId());
        }

        notifyPendingUserReject(updatedContact);

        User userOwner = contact.getRoomRequested().getOwner();
        User userOffer = contact.getOfferUser();
        emailService.sendSwapAcceptedRequester(userOffer, userOwner);
        emailService.sendSwapAcceptedOwner(userOffer, userOwner);
        return updatedContact;
    }

    private void validateSwapAcceptanceDates(Contact contact, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidContactUpdateException("Swap acceptance requires checkIn and checkOut");
        }
        if (!endDate.isAfter(startDate)) {
            throw new InvalidContactUpdateException("checkOut must be after checkIn");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidContactUpdateException("checkIn must be today or later");
        }
        if (contact.getRoomOffered() == null) {
            throw new InvalidContactUpdateException("Swap contact has no offered room");
        }
    }

    private void validateNoDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn != null || checkOut != null) {
            throw new InvalidContactUpdateException("Dates are only valid when accepting a room swap");
        }
    }

    @Async
    @Override
    public void notifyPendingUserReject(Contact acceptedContact){
        List<Contact> deletedContacts = contactDao.findPendingRequestedOverlap(acceptedContact.getId());
        contactDao.deletePendingRequestedOverlap(acceptedContact.getId());

        for(Contact deletedContact : deletedContacts){
            emailService.sendSwapOwnerAcceptedOtherOffer(
                    deletedContact.getOfferUser(),
                    deletedContact.getRoomRequested().getOwner(),
                    deletedContact.getRoomRequested(),
                    deletedContact.getRequestedRange()
            );
        }
    }

    @Override
    @Transactional
    public void rejectedContact(long contactId){
        Contact contact = contactDao.findContactById(contactId).orElseThrow(() -> new ContactNotFoundException(contactId));
        if(!contact.getStatus().equals(SwapStatus.PENDING)){
            throw new InvalidContactStateException("This contact is not pending");
        }
        expirePendingContactIfNeeded(contact, LocalDate.now());

        rejectPendingContact(contact);
    }

    private Contact rejectPendingContact(Contact contact) {

        User owner = contact.getRoomRequested().getOwner();
        User userOffer = contact.getOfferUser();

        contactDao.updateStatus(contact.getId(), SwapStatus.REJECTED);
        contact.setStatus(SwapStatus.REJECTED);
        emailService.sendSwapRejectedRequester(userOffer,owner,contact.getRoomRequested());
        return contact;
    }

    @Override
    public Optional<Contact> getContactById(long contactId) {
        return contactDao.findContactById(contactId);
    }

    @Override
    public boolean exists(long contactId) {
        return contactDao.findContactById(contactId).isPresent();
    }

    @Override
    public List<Contact> findByOwnerEmail(String email) {
        User user = userService.findUserByEmail(email).orElseThrow(()-> new IllegalArgumentException("User not found")); //TODO
        return contactDao.findByOwnerId(user.getId());
    }

    @Override
    public List<Contact> findAcceptedRequestsByUser(String email, int page, int pageSize){
        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found")); //TODO
        return contactDao.findAcceptedRequestsByUser(user.getId(), page, pageSize);
    }

    @Override
    public List<Contact> findAcceptedOffersByUser(String email, int page, int pageSize){
        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found")); //TODO
        return contactDao.findAcceptedOffersByUser(user.getId(), page, pageSize);
    }

    @Override
    public List<DateRange> contactAcceptedDatesForRoomBetween(long roomId, LocalDate startDate, LocalDate endDate) {
        return contactDao.contactAcceptedDatesForRoomBetween(roomId, startDate, endDate);
    }

    @Override
    public long getRoomOfferedId(long contactId){
        return contactDao.getRoomOfferedId(contactId);
    }

    @Override
    public boolean isSwap(long contactId) {
        return contactDao.isSwap(contactId);
    }

    private List<Contact> getAllAcceptedContacts(String email, int page, int pageSize) {
        List<Contact> acceptedRequests = findAcceptedRequestsByUser(email, page, pageSize);
        List<Contact> acceptedOffers = findAcceptedOffersByUser(email, page, pageSize);

        List<Contact> allAccepted = acceptedRequests.stream().collect(Collectors.toList());
        acceptedOffers.forEach(contact -> {
            if (allAccepted.stream().noneMatch(c -> c.getId() == contact.getId())) {
                allAccepted.add(contact);
            }
        });
        return allAccepted;
    }


    @Override
    public int countPastTripActions(String username, LocalDate today){
        User user = userService.findUserByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return contactDao.countPastTripsByEmail(user.getId(), today);
    }

    @Override
    public boolean hasActiveContacts(long roomId) {
        return contactDao.hasActiveContacts(roomId);
    }
    public int getStayDurationDays(long contactId){
            return contactDao.getStayDurationDays(contactId);
    }


    private LocalDate earliestStartDate(Contact trip) {
        LocalDate reqStart = trip.getRequestedRange().getStartDate();
        if (trip.isSwap() && trip.getOfferedRange() != null) {
            LocalDate offStart = trip.getOfferedRange().getStartDate();
            return offStart.isBefore(reqStart) ? offStart : reqStart;
        }
        return reqStart;
    }
    private boolean hasMoreThanAWeek(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to);
        return days > 7;
    }

    @Transactional
    @Override
    public void cancelSwap(long contactId, String username, LocalDate date) {
        Contact contactToCancel = contactDao.findContactById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));

        if(contactToCancel.getStatus() == SwapStatus.ACCEPTED){
            LocalDate firstDate = earliestStartDate(contactToCancel);
            boolean canCancel = hasMoreThanAWeek(date, firstDate);

            if (!canCancel)
                throw new CancelException("Cancellations can only be made more than 7 days in advance.");

        }

        contactDao.cancelSwap(contactId);

        User cancellingUser = userService.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        User userToNotify = getUserToNotify(contactToCancel, cancellingUser);

        if (userToNotify != null) {
            emailService.sendSwapCancellationNotification(
                    userToNotify,
                    cancellingUser,
                    contactToCancel.getRoomRequested(), contactToCancel.getStatus()
            );
        }
    }

    private static User getUserToNotify(Contact contactToCancel, User cancellingUser) {
        User owner = contactToCancel.getRoomRequested().getOwner();
        User requester = contactToCancel.getOfferUser(); // quien inició el swap

        User userToNotify;
        if (requester != null && cancellingUser.getId() == requester.getId()) {

            userToNotify = owner;
        } else if (cancellingUser.getId() == owner.getId()) {
            // Cancela el dueño → avisar al que pidió
            userToNotify = requester;
        } else {
            userToNotify = owner;
        }
        return userToNotify;
    }

    @Override
    public Room userIsGoingTo(long contactId, String email) {
        User user = userService.findUserByEmail(email).orElseThrow(()-> new UsernameNotFoundException("User not found"));

        Contact contact = getContactById(contactId).orElseThrow(()-> new ContactNotFoundException(contactId));

        if(user.getId() != contact.getOfferUserId() && user.getId() != contact.getRoomRequested().getOwner().getId()){
            throw new ForbiddenUserOperationException("User is not the guest or the host", "error.contact.userNotParticipant");
        }

        if(!isSwap(contactId) && user.getId() == contact.getRoomRequested().getOwner().getId()){
            throw new ForbiddenUserOperationException("User cannot leave review of their own room for a non-swap", "error.review.ownNonSwapRoom");
        }

        if(isSwap(contactId) && user.getId() == contact.getRoomRequested().getOwner().getId()){
            return contact.getRoomOffered();
        }

        return contact.getRoomRequested();
    }

    private User findUser(String email){
        return userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    @Override
    public List<Contact> canceledTripsPage(String email, int page, int pageSize) {
        User user = findUser(email);
        return contactDao.findCanceledByUserPage(user.getId(), page, pageSize);
    }

    @Override
    public long countCanceledTrips(String email) {
        User user = findUser(email);
        return contactDao.countCancelSwapsUser(user.getId());
    }

    public List<Contact> expiredSwapsPage(String email, int page, int pageSize){
        User user = findUser(email);
        return contactDao.findExpiredByUserPage(user.getId(), page, pageSize);

    }

    public long countExpiredSwaps(String email){
        User user = findUser(email);
        return contactDao.countExpiredSwapsUser(user.getId());

    }

    @Override
    public ContactPage findContactsPage(String email, ContactView view, int page, int pageSize) {
        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);
        final LocalDate today = LocalDate.now();
        final ContactView contactView = view != null ? view : ContactView.SENT;

        final List<Contact> contacts;
        final long totalItems;

        switch (contactView) {
            case SENT:
                contacts = getRequestedSwapsByUser(email, safePage, safePageSize);
                totalItems = countRequestedSwaps(email);
                break;
            case RECEIVED:
                contacts = receivedSwaps(email, safePage, safePageSize);
                totalItems = countReceivedSwaps(email);
                break;
            case ACTIVE:
                contacts = upcomingTrips(email, today, safePage, safePageSize);
                totalItems = countUpcomingTrips(email, today);
                break;
            case CANCELED:
                contacts = canceledTripsPage(email, safePage, safePageSize);
                totalItems = countCanceledTrips(email);
                break;
            case PAST:
                contacts = pastTrips(email, today, safePage, safePageSize);
                totalItems = countPastTripActions(email, today);
                break;
            case EXPIRED:
                contacts = expiredSwapsPage(email, safePage, safePageSize);
                totalItems = countExpiredSwaps(email);
                break;
            default:
                throw new IllegalArgumentException("Invalid contacts view");
        }

        Map<Long, Boolean> pendingReviews = contactView == ContactView.PAST
                ? buildPendingReviewMap(email, contacts, today)
                : Collections.emptyMap();

        return new ContactPage(contacts, totalItems, safePage, safePageSize, pendingReviews);
    }

    @Transactional
    @Override
    public ContactPage findContactsPage(String email, ContactView view, Long tripId, int page, int pageSize) {
        if (tripId == null) {
            return findContactsPage(email, view, page, pageSize);
        }

        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);
        final Trip trip = tripService.findTripById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!email.equals(trip.getGroupTrip().getOwner().getEmail())) {
            throw new TripContactsNotOwnerException(tripId);
        }

        final List<Contact> contacts = tripContactDao.tripContactList(tripId, safePage, safePageSize)
                .stream()
                .map(TripContact::getContact)
                .collect(Collectors.toList());
        final long totalItems = tripContactDao.countByTripId(tripId);

        return new ContactPage(contacts, totalItems, safePage, safePageSize);
    }

    private Map<Long, Boolean> buildPendingReviewMap(String email, List<Contact> contacts, LocalDate today) {
        if (contacts.isEmpty()) {
            return Collections.emptyMap();
        }

        User user = findUser(email);
        Map<Long, Boolean> pendingReviews = new HashMap<>();
        for (Contact contact : contacts) {
            pendingReviews.put(contact.getId(), reviewEligibilityService.isReviewPending(contact, user, today));
        }
        return pendingReviews;
    }

    @Override
    public List<Contact> getRequestedSwapsByUser(String username, int page, int pageSize) {
        User user = userService.findUserByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return contactDao.findRequestedSwapsByUserPage(user.getId(), page, pageSize);
    }

    @Override
    public List<String> getRequestedOwners(String username, int page, int pageSize) {
        List<Contact> contacts = getRequestedSwapsByUser(username, page, pageSize);
        return getOwnerUsers(contacts);
    }

    private List<String> getOwnerUsers(List<Contact> trips) {
        List<String> usersOwner = new ArrayList<>(trips.size());
        for (Contact trip : trips) {
            String username = trip.getRoomRequested().getOwner().getName();
            usersOwner.add(username);
        }
        return usersOwner;
    }

    @Override
    public long countRequestedSwaps(String username) {
        User user = userService.findUserByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return contactDao.countRequestedSwapsByUser(user.getId());
    }

    @Transactional
    @Override
    public List<Contact> findAvailableContactsForTrip(String email, long tripId, int page, int pageSize) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Trip trip = tripService.findTripById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        List<Contact> contacts = contactDao.findAvailableContactsForTrip(user.getId(), trip, page, pageSize);

        long userId = user.getId();
        String tripCountry = trip.getCountry().toUpperCase();
        DateRange tripRange = trip.getDateRange();

        return contacts.stream()
                .filter(contact -> {
                    if (contact.getRoomRequested().getOwner().getId() == userId) {
                        if (contact.isSwap() && contact.getRoomOffered() != null && contact.getOfferedRange() != null) {
                            boolean countryMatch = contact.getRoomOffered().getCountry().toUpperCase().equals(tripCountry);
                            boolean dateOverlap = datesOverlap(contact.getOfferedRange(), tripRange);

                            return countryMatch && dateOverlap;
                        }
                        return false;
                    }

                    if (contact.getOfferUser() != null && contact.getOfferUser().getId() == userId) {

                        boolean countryMatch = contact.getRoomRequested().getCountry().toUpperCase().equals(tripCountry);
                        boolean dateOverlap = datesOverlap(contact.getRequestedRange(), tripRange);

                        return countryMatch && dateOverlap;
                    }

                    return false;
                })
                .collect(Collectors.toList());
    }

    private boolean datesOverlap(DateRange contactRange, DateRange tripRange) {
        return contactRange.getStartDate().isBefore(tripRange.getEndDate())
                && contactRange.getEndDate().isAfter(tripRange.getStartDate());
    }

    @Override
    public boolean isAccepter(String email, long contactId){
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Contact contact = getContactById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));

        if((contact.getStatus() != SwapStatus.PENDING) || (contact.isSwap() && contact.getRoomOffered() == null)) {
            return false;
        }

        return user.getId() == contact.getRoomRequested().getOwner().getId();
    }

    @Override
    public boolean belongsToSwap(String email, long contactId){
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Contact contact = getContactById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        return (user.getId() == contact.getRoomRequested().getOwner().getId()) || (user.getId() == contact.getOfferUserId());
    }
    @Override
    public BigDecimal getTotalMoneyEarnedByUser(long userId) {
        return contactDao.getTotalMoneyEarnedByUser(userId);
    }

    @Override
    public BigDecimal getTotalMoneySpentByUser(long userId) {
        return contactDao.getTotalMoneySpentByUser(userId);
    }

    @Override
    public long countCompletedSwapsByUser(long userId) {
        return contactDao.countCompletedSwapsByUser(userId);
    }


    @Transactional
    @Override
    public List<Contact> findContactsForRoom(long roomId){
        return contactDao.findContactsForRoom(roomId);
    }

    @Async
    @Override
    public void notifyRoomUpdate(long roomId, String oldTitle, String oldDescription, List<Amenity> oldAmenities,
                                 String newTitle, String newDescription, List<Amenity> newAmenities, List<Contact> contacts) {

        Map<String, Object> vars = new HashMap<>();
        vars.put("roomId", roomId);
        vars.put("roomUrl", "/room/" + roomId);
        vars.put("oldTitle", oldTitle);
        vars.put("newTitle", newTitle);
        vars.put("oldDescription", oldDescription != null ? oldDescription : "N/A");
        vars.put("newDescription", newDescription != null ? newDescription : "N/A");
        vars.put("oldAmenities", oldAmenities != null ? String.join(", ", oldAmenities.stream().map(Amenity::toString).toList()) : "N/A");
        vars.put("newAmenities", newAmenities != null ? String.join(", ", newAmenities.stream().map(Amenity::toString).toList()) : "N/A");
        String subjectKey = "email.room.updated.subject";

        for (Contact contact : contacts) {
            User recipient = contact.getRoomRequested().getId() == roomId ? contact.getOfferUser() : contact.getRoomRequested().getOwner();
            Map<String, Object> emailVars = new HashMap<>(vars);
            emailVars.put("recipientName", recipient.getName() != null ? recipient.getName() : "User");
            emailService.sendRoomUpdateNotification(recipient, emailVars, subjectKey);
        }
    }
}
