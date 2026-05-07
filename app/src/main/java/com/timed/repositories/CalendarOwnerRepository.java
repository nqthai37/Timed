package com.timed.repositories;

import com.timed.managers.UserManager;
import com.timed.models.CalendarModel;
import com.timed.models.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarOwnerRepository {
    private final UserRepository userRepository;
    private final Map<String, String> ownerNameCache = new HashMap<>();

    public CalendarOwnerRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void cacheOwnerName(User user) {
        if (user == null) {
            return;
        }
        String userId = user.getUid();
        String name = user.getName();
        if (userId == null || name == null) {
            return;
        }
        String normalized = name.trim();
        if (!normalized.isEmpty()) {
            ownerNameCache.put(userId, normalized);
        }
    }

    public void loadOwnerNames(List<CalendarModel> calendars, Runnable onComplete) {
        if (calendars == null || calendars.isEmpty()) {
            run(onComplete);
            return;
        }

        cacheOwnerName(UserManager.getInstance().getCurrentUser());

        if (userRepository == null) {
            run(onComplete);
            return;
        }

        Set<String> ownerIdsToFetch = new HashSet<>();
        for (CalendarModel calendar : calendars) {
            if (calendar == null) {
                continue;
            }
            String ownerId = calendar.getOwnerId();
            if (ownerId == null || ownerId.isEmpty()) {
                continue;
            }

            String cachedName = ownerNameCache.get(ownerId);
            if (cachedName != null) {
                calendar.setOwnerName(cachedName);
            } else {
                ownerIdsToFetch.add(ownerId);
            }
        }

        if (ownerIdsToFetch.isEmpty()) {
            run(onComplete);
            return;
        }

        AtomicInteger remaining = new AtomicInteger(ownerIdsToFetch.size());
        for (String ownerId : ownerIdsToFetch) {
            userRepository.getUser(ownerId)
                    .addOnSuccessListener(snapshot -> {
                        User owner = snapshot.toObject(User.class);
                        String ownerName = owner != null ? owner.getName() : null;
                        if (ownerName != null) {
                            ownerName = ownerName.trim();
                        }
                        if (ownerName != null && !ownerName.isEmpty()) {
                            ownerNameCache.put(ownerId, ownerName);
                            applyOwnerName(calendars, ownerId, ownerName);
                        }
                        if (remaining.decrementAndGet() == 0) {
                            run(onComplete);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            run(onComplete);
                        }
                    });
        }
    }

    private void applyOwnerName(List<CalendarModel> calendars, String ownerId, String ownerName) {
        if (calendars == null || ownerId == null || ownerName == null) {
            return;
        }
        for (CalendarModel calendar : calendars) {
            if (calendar != null && ownerId.equals(calendar.getOwnerId())) {
                calendar.setOwnerName(ownerName);
            }
        }
    }

    private void run(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }
}
