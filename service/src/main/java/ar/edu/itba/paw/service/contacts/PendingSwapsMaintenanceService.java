package ar.edu.itba.paw.service.contacts;

import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PendingSwapsMaintenanceService {
    static final String DAILY_EXPIRATION_CRON = "0 0 2 * * *";
    static final String DAILY_EXPIRATION_ZONE = "America/Argentina/Buenos_Aires";

    private final ContactDao contactDao;

    public PendingSwapsMaintenanceService(ContactDao contactDao) {
        this.contactDao = contactDao;
    }

    @Scheduled(cron = DAILY_EXPIRATION_CRON, zone = DAILY_EXPIRATION_ZONE)
    @Transactional
    public void updateExpiredTripStatuses() {
        List<Contact> pendingSwapsExpired = contactDao.findExpiredPendingSwaps();

        if (pendingSwapsExpired.isEmpty()) {
            return;
        }

        for (Contact swap : pendingSwapsExpired) {
            contactDao.updateStatus(swap.getId(), SwapStatus.EXPIRED);
        }
    }
}
