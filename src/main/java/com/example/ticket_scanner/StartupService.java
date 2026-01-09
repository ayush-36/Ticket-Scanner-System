package com.example.ticket_scanner;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StartupService {

    private final Sheetservice sheetService;
    private final QRCodeGenerator qrService;
    private final EmailService emailService;
    private final TicketStatusService ticketStatusService;

    public StartupService(Sheetservice sheetService, QRCodeGenerator qrService,
                          EmailService emailService, TicketStatusService ticketStatusService) {
        this.sheetService = sheetService;
        this.qrService = qrService;
        this.emailService = emailService;
        this.ticketStatusService = ticketStatusService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateAndSendQRCodes() {
        try {
            // Fetch all participants from the sheet
            List<Map<String, String>> participants = sheetService.getAllParticipants();

            for (Map<String, String> participant : participants) {
                String email = participant.get("email");
                String referenceId = participant.get("referenceId");
                int rowIndex = Integer.parseInt(participant.get("rowIndex"));

                // Skip invalid rows
                if (email == null || email.isEmpty() || referenceId.isEmpty()) {
                    continue;
                }

                // Skip if QR already sent
                if ("SENT".equalsIgnoreCase(participant.get("qrSent"))) {
                    continue;
                }
                    // Generate QR code
                    String qrBase64 = qrService.generateQRCode(referenceId);

                    // Send the QR code email
                    emailService.sendEmailWithQRCode(email, referenceId,
                            "Please find attached your QR code.", qrBase64);

                    System.out.println("QR Code sent to " + email);

                     sheetService.markQrSent(rowIndex);
                }
            } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
        }
