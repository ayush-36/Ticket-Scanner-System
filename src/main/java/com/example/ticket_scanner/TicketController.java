package com.example.ticket_scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private GoogleSheetsService sheetsService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/scan")
    public ResponseEntity<?> scanTicket(@RequestBody Map<String, String> body) {
        String ticketId = body.get("ticketId");
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid ticket"));
        }
        Ticket ticket = optionalTicket.get();
        if (ticket.isScanned()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ticket already scanned"));
        }
        ticket.setScanned(true);
        ticketRepository.save(ticket);
        sheetsService.logScan(ticketId);
        return ResponseEntity.ok(Map.of("message", "Ticket scanned successfully"));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateTicket(@RequestBody Map<String, String> body) {
        try {
            String referenceId = body.get("referenceId");
            String email = body.get("email");

            Ticket ticket = new Ticket(referenceId, false);
            ticketRepository.save(ticket);

            QRCodeGenerator qrCodeGenerator = null;

            String qrBase64 = qrCodeGenerator.generateQRCode(referenceId);

            String subject = "Your Ticket QR Code";
            String message = "Please find attached your ticket QR code.";
            emailService.sendEmailWithQRCode(email, referenceId, message, qrBase64);

            return ResponseEntity.ok(Map.of("message", "Ticket generated and emailed successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Error generating ticket"));
        }
    }
}
