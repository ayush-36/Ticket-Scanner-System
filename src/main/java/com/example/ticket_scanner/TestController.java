package com.example.ticket_scanner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TestController {

    private final Sheetservice sheetService;
    private final QRCodeGenerator qrService;
    private final EmailService emailService;
    private final TicketStatusService ticketStatusService;

    public TestController(Sheetservice sheetService, QRCodeGenerator qrService,
                          EmailService emailService, TicketStatusService ticketStatusService) {
        this.sheetService = sheetService;
        this.qrService = qrService;
        this.emailService = emailService;
        this.ticketStatusService = ticketStatusService;
    }

    @GetMapping("/generate-qr")
    public ResponseEntity<Map<String, Object>> generateQR(
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String email) throws Exception {

        Map<String, Object> resp = new HashMap<>();
        try {
            if ((referenceId == null || referenceId.isEmpty()) &&
                    (email == null || email.isEmpty())) {
                resp.put("status", "error");
                resp.put("message", "Either referenceId or email must be provided!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
            }

            if (referenceId != null && !referenceId.isEmpty() && (email == null || email.isEmpty())) {
                Map<String, String> data = sheetService.getParticipantData(referenceId);
                if (data == null || data.isEmpty()) {
                    resp.put("status", "error");
                    resp.put("message", "Reference ID not found!");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
                }
                email = data.get("email");
            } else if (email != null && !email.isEmpty() && (referenceId == null || referenceId.isEmpty())) {
                referenceId = sheetService.getReferenceIdByEmail(email);
                if (referenceId == null || referenceId.isEmpty()) {
                    resp.put("status", "error");
                    resp.put("message", "Email not found!");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
                }
            }

            if (ticketStatusService.isScanned(referenceId)) {
                resp.put("status", "conflict");
                resp.put("message", "Ticket already scanned");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
            }

            String qrBase64 = qrService.generateQRCode(referenceId);
            emailService.sendEmailWithQRCode(email, referenceId, "Here is your ticket QR code.", qrBase64);

            resp.put("status", "ok");
            resp.put("message", "QR sent");
            resp.put("referenceId", referenceId);
            resp.put("qrBase64", qrBase64);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.put("status", "error");
            resp.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        } }

    @PostMapping("/scan-ticket")
    public ResponseEntity<Map<String, Object>> scanTicket(@RequestParam String referenceId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            List<Map<String, String>> participants = sheetService.getAllParticipants();
            for (Map<String, String> participant : participants) {
                if (referenceId.equals(participant.get("referenceId"))) {
                    int rowIndex = Integer.parseInt(participant.get("rowIndex"));

                    if ("OK".equalsIgnoreCase(participant.get("scanned"))) {
                        resp.put("status", "already_scanned");
                        resp.put("message", "Reference ID " + referenceId + " has already been scanned!");
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
                    }

                    // Mark as scanned
                    sheetService.markScanned(rowIndex);

                    resp.put("status", "scanned");
                    resp.put("message", "Reference ID " + referenceId + " scanned successfully!");
                    return ResponseEntity.ok(resp);
                }
            }
            resp.put("status", "invalid");
            resp.put("message", "Invalid Reference ID!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("status", "error");
            resp.put("message", "Error scanning ticket!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }
}
