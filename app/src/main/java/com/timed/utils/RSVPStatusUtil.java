package com.timed.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing RSVP (Répondez S'il Vous Plaît) statuses
 * Supports: Accepted (Yes), Declined (No), Tentative (Maybe), Pending
 */
public class RSVPStatusUtil {

    // RSVP Status Constants
    public static final String ACCEPTED = "accepted";      // Yes, I'll be there
    public static final String DECLINED = "declined";      // No, I can't make it
    public static final String TENTATIVE = "tentative";    // Maybe, still thinking
    public static final String PENDING = "pending";        // Awaiting response

    /**
     * Get human-readable label for RSVP status
     */
    public static String getStatusLabel(String status) {
        if (status == null) return "No Response";
        
        switch (status.toLowerCase()) {
            case ACCEPTED:
                return "Yes, I'll be there";
            case DECLINED:
                return "No, I can't make it";
            case TENTATIVE:
                return "Maybe, still thinking";
            case PENDING:
                return "Awaiting Response";
            default:
                return "Unknown";
        }
    }

    /**
     * Get emoji representation of RSVP status
     */
    public static String getStatusEmoji(String status) {
        if (status == null) return "❓";
        
        switch (status.toLowerCase()) {
            case ACCEPTED:
                return "✅";
            case DECLINED:
                return "❌";
            case TENTATIVE:
                return "❓";
            case PENDING:
                return "⏳";
            default:
                return "❓";
        }
    }

    /**
     * Check if status is a confirmed response
     */
    public static boolean isConfirmed(String status) {
        return ACCEPTED.equals(status) || DECLINED.equals(status);
    }

    /**
     * Check if status indicates attendance
     */
    public static boolean isAttending(String status) {
        return ACCEPTED.equals(status) || TENTATIVE.equals(status);
    }

    /**
     * Check if status is still awaiting response
     */
    public static boolean isAwaiting(String status) {
        return PENDING.equals(status) || status == null;
    }

    /**
     * Get color code for status (for UI display)
     */
    public static int getStatusColor(String status) {
        if (status == null) return 0xFF808080;  // Gray
        
        switch (status.toLowerCase()) {
            case ACCEPTED:
                return 0xFF4CAF50;  // Green
            case DECLINED:
                return 0xFFF44336;  // Red
            case TENTATIVE:
                return 0xFFFFC107;  // Amber
            case PENDING:
                return 0xFF2196F3;  // Blue
            default:
                return 0xFF808080;  // Gray
        }
    }

    /**
     * Get all valid RSVP statuses
     */
    public static String[] getAllStatuses() {
        return new String[]{ACCEPTED, DECLINED, TENTATIVE, PENDING};
    }

    /**
     * Count participants by status
     */
    public static Map<String, Integer> countByStatus(Map<String, String> participantStatus) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put(ACCEPTED, 0);
        counts.put(DECLINED, 0);
        counts.put(TENTATIVE, 0);
        counts.put(PENDING, 0);

        if (participantStatus != null) {
            for (String status : participantStatus.values()) {
                String normalizedStatus = status != null ? status.toLowerCase() : PENDING;
                if (counts.containsKey(normalizedStatus)) {
                    counts.put(normalizedStatus, counts.get(normalizedStatus) + 1);
                } else {
                    counts.put(PENDING, counts.get(PENDING) + 1);
                }
            }
        }

        return counts;
    }

    /**
     * Get attendance summary
     */
    public static String getAttendanceSummary(Map<String, String> participantStatus) {
        Map<String, Integer> counts = countByStatus(participantStatus);
        
        return String.format("✅ %d  ❌ %d  ❓ %d  ⏳ %d",
                counts.get(ACCEPTED),
                counts.get(DECLINED),
                counts.get(TENTATIVE),
                counts.get(PENDING));
    }
}
