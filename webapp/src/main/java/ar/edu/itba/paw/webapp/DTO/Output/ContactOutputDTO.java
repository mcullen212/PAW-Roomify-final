package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.swaps.Contact;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@XmlRootElement
public class ContactOutputDTO {
    private long id;
    private String contactDate;
    private String status;
    private boolean isSwap;
    private BigDecimal moneyOffer;
    private DateRangeOutputDTO requestedRange;
    private DateRangeOutputDTO offeredRange;
    private Long offerUserId;
    private String offerUserName;
    private long roomRequestedId;
    private long roomRequestedOwnerId;
    private String roomRequestedOwnerName;
    private Long roomOfferedId;
    private Long roomOfferedOwnerId;
    private String roomOfferedOwnerName;
    private Boolean pendingReview;
    private URI self;
    private Map<String, URI> links;

    public ContactOutputDTO() {
        // Required by Jersey/MOXy.
    }

    public ContactOutputDTO(final Contact contact, final UriInfo uriInfo) {
        this(contact, uriInfo, null);
    }

    public ContactOutputDTO(final Contact contact, final UriInfo uriInfo, final Boolean pendingReview) {
        this.id = contact.getId();
        this.contactDate = contact.getContactDate() != null ? contact.getContactDate().toString() : null;
        this.status = contact.getStatus() != null ? contact.getStatus().name() : null;
        this.isSwap = contact.isSwap();
        this.moneyOffer = contact.getMoneyOffer() != null ? contact.getMoneyOffer() : BigDecimal.ZERO;
        this.requestedRange = contact.getRequestedRange() != null ? new DateRangeOutputDTO(contact.getRequestedRange()) : null;
        this.offeredRange = contact.getOfferedRange() != null ? new DateRangeOutputDTO(contact.getOfferedRange()) : null;
        this.offerUserId = contact.getOfferUser() != null ? contact.getOfferUser().getId() : null;
        this.offerUserName = contact.getOfferUser() != null ? contact.getOfferUser().getName() : null;
        this.roomRequestedId = contact.getRoomRequested().getId();
        this.roomRequestedOwnerId = contact.getRoomRequested().getOwner().getId();
        this.roomRequestedOwnerName = contact.getRoomRequested().getOwner().getName();
        this.roomOfferedId = contact.getRoomOffered() != null ? contact.getRoomOffered().getId() : null;
        this.roomOfferedOwnerId = contact.getRoomOffered() != null ? contact.getRoomOffered().getOwner().getId() : null;
        this.roomOfferedOwnerName = contact.getRoomOffered() != null ? contact.getRoomOffered().getOwner().getName() : null;
        this.pendingReview = pendingReview;
        this.self = buildContactUri(contact, uriInfo);
        this.links = buildLinks(contact, uriInfo);
        if (Boolean.TRUE.equals(pendingReview)) {
            this.links.put("review", uriInfo.getBaseUriBuilder()
                    .path("reviews")
                    .build());
        }
    }

    private URI buildContactUri(final Contact contact, final UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder()
                .path("contacts")
                .path(String.valueOf(contact.getId()))
                .build();
    }

    private Map<String, URI> buildLinks(final Contact contact, final UriInfo uriInfo) {
        final Map<String, URI> contactLinks = new LinkedHashMap<>();
        contactLinks.put("self", buildContactUri(contact, uriInfo));
        if (contact.getOfferUser() != null) {
            contactLinks.put("offerUser", uriInfo.getBaseUriBuilder()
                    .path("users")
                    .path(String.valueOf(contact.getOfferUser().getId()))
                    .build());
        }
        contactLinks.put("roomRequested", uriInfo.getBaseUriBuilder()
                .path("rooms")
                .path(String.valueOf(contact.getRoomRequested().getId()))
                .build());

        if (contact.getRoomOffered() != null) {
            contactLinks.put("roomOffered", uriInfo.getBaseUriBuilder()
                    .path("rooms")
                    .path(String.valueOf(contact.getRoomOffered().getId()))
                    .build());
        }

        return contactLinks;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getContactDate() { return contactDate; }
    public void setContactDate(String contactDate) { this.contactDate = contactDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    @XmlElement(name = "isSwap")
    public boolean getIsSwap() { return isSwap; }
    public void setIsSwap(boolean swap) { isSwap = swap; }
    public BigDecimal getMoneyOffer() { return moneyOffer; }
    public void setMoneyOffer(BigDecimal moneyOffer) { this.moneyOffer = moneyOffer; }
    public DateRangeOutputDTO getRequestedRange() { return requestedRange; }
    public void setRequestedRange(DateRangeOutputDTO requestedRange) { this.requestedRange = requestedRange; }
    public DateRangeOutputDTO getOfferedRange() { return offeredRange; }
    public void setOfferedRange(DateRangeOutputDTO offeredRange) { this.offeredRange = offeredRange; }
    public Long getOfferUserId() { return offerUserId; }
    public void setOfferUserId(Long offerUserId) { this.offerUserId = offerUserId; }
    public String getOfferUserName() { return offerUserName; }
    public void setOfferUserName(String offerUserName) { this.offerUserName = offerUserName; }
    public long getRoomRequestedId() { return roomRequestedId; }
    public void setRoomRequestedId(long roomRequestedId) { this.roomRequestedId = roomRequestedId; }
    public long getRoomRequestedOwnerId() { return roomRequestedOwnerId; }
    public void setRoomRequestedOwnerId(long roomRequestedOwnerId) { this.roomRequestedOwnerId = roomRequestedOwnerId; }
    public String getRoomRequestedOwnerName() { return roomRequestedOwnerName; }
    public void setRoomRequestedOwnerName(String roomRequestedOwnerName) { this.roomRequestedOwnerName = roomRequestedOwnerName; }
    public Long getRoomOfferedId() { return roomOfferedId; }
    public void setRoomOfferedId(Long roomOfferedId) { this.roomOfferedId = roomOfferedId; }
    public Long getRoomOfferedOwnerId() { return roomOfferedOwnerId; }
    public void setRoomOfferedOwnerId(Long roomOfferedOwnerId) { this.roomOfferedOwnerId = roomOfferedOwnerId; }
    public String getRoomOfferedOwnerName() { return roomOfferedOwnerName; }
    public void setRoomOfferedOwnerName(String roomOfferedOwnerName) { this.roomOfferedOwnerName = roomOfferedOwnerName; }
    public Boolean getPendingReview() { return pendingReview; }
    public void setPendingReview(Boolean pendingReview) { this.pendingReview = pendingReview; }
    public URI getSelf() { return self; }
    public void setSelf(URI self) { this.self = self; }
    public Map<String, URI> getLinks() { return links; }
    public void setLinks(Map<String, URI> links) { this.links = links; }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                contactDate,
                status,
                isSwap,
                moneyOffer,
                requestedRange,
                offeredRange,
                offerUserId,
                offerUserName,
                roomRequestedId,
                roomRequestedOwnerId,
                roomRequestedOwnerName,
                roomOfferedId,
                roomOfferedOwnerId,
                roomOfferedOwnerName,
                pendingReview,
                self,
                links
        );
    }
}
