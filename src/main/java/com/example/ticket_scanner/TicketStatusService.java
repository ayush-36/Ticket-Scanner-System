package com.example.ticket_scanner;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TicketStatusService {

    private final ConcurrentHashMap<String, Boolean> ticketMap = new ConcurrentHashMap<>();

    public boolean isScanned(String referenceId) {
        return ticketMap.getOrDefault(referenceId, false);
    }

    public void markScanned(String referenceId) {
        ticketMap.put(referenceId, true);
    }
}
