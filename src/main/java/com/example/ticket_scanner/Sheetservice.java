package com.example.ticket_scanner;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class Sheetservice {

    private static Sheets sheetsService = null;
    private static String spreadsheetId = null;

    public Sheetservice(@Value("${google.sheet.id}") String spreadsheetId,@Value("${sheet.credentials.path}") String credentialsPath) throws Exception {
        this.spreadsheetId = spreadsheetId;

        // Load credentials.json from resources
        InputStream in = getClass().getClassLoader().getResourceAsStream("credentials.json");
        if (in == null) {
            throw new RuntimeException("Cannot find credentials.json in resources folder");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));
        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Ticket Scanner")
                .build();
    }

    public static void appendTicket(String ticketId) throws Exception {
        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(ticketId)));

        sheetsService.spreadsheets().values()
                .append(spreadsheetId, "Sheet1!A:A", body)
                .setValueInputOption("RAW")
                .execute();
    }

    public Map<String, String> getParticipantData(String referenceId) throws Exception {
        String range = "Sheet1!A2:C"; // Adjust if needed, skip header row
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            return null;
        }

        for (List<Object> row : values) {
            if (row.size() >= 3) {
                String refId = row.get(1).toString();
                if (refId.equals(referenceId)) {
                    Map<String, String> data = new HashMap<>();
                    data.put("name", row.get(0).toString());
                    data.put("referenceId", refId);
                    data.put("email", row.get(2).toString());
                    return data;
                }
            }
        }

        return null;
    }


    public String getReferenceIdByEmail(String email) throws Exception {
        // Read all rows from your sheet
        String range = "Sheet1!A:C"; // assuming columns: Name | ReferenceId | Email
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        if (response.getValues() == null) {
            return null;
        }

        for (java.util.List<Object> row : response.getValues()) {
            if (row.size() >= 3) {
                String rowEmail = row.get(2).toString(); // Email is in column C (index 2)
                if (rowEmail.equalsIgnoreCase(email)) {
                    return row.get(1).toString(); // ReferenceId is in column B (index 1)
                }
            }
        }

        return null; // Not found
    }

        public List<Map<String, String>> getAllParticipants() throws Exception {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, "Sheet1!A2:E")
                    .execute();

            List<Map<String, String>> participants = new ArrayList<>();
            List<List<Object>> values = response.getValues();

            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    List<Object> row = values.get(i);
                    Map<String, String> participant = new HashMap<>();
                    participant.put("name", row.size() > 0 ? row.get(0).toString() : "");
                    participant.put("referenceId", row.size() > 1 ? row.get(1).toString() : "");
                    participant.put("email", row.size() > 2 ? row.get(2).toString() : "");
                    participant.put("qrSent", row.size() > 3 ? row.get(3).toString() : "");
                    participant.put("scanned", row.size() > 4 ? row.get(4).toString() : "");
                    participant.put("rowIndex", String.valueOf(i + 2));
                    participants.add(participant);
                }
            }

            return participants;
        }
    // Mark QR as sent
    public void markQrSent(int rowIndex) throws Exception {
        ValueRange body = new ValueRange().setValues(
                Collections.singletonList(Collections.singletonList("SENT"))
        );
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, "Sheet1!D" + rowIndex, body)
                .setValueInputOption("RAW")
                .execute();
    }

    // Mark ticket as scanned
    public void markScanned(int rowIndex) throws Exception {
        ValueRange body = new ValueRange().setValues(
                Collections.singletonList(Collections.singletonList("OK"))
        );
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, "Sheet1!E" + rowIndex, body)
                .setValueInputOption("RAW")
                .execute();
    }
    }

