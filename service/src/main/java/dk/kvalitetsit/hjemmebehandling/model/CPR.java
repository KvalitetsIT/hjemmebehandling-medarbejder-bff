package dk.kvalitetsit.hjemmebehandling.model;

import java.time.LocalDate;

public record CPR(String value) {

    public CPR {
        if (!isValidFormat(value)) {
            throw new IllegalArgumentException("Invalid CPR format: " + value);
        }
        if (!isValidDate(value)) {
            throw new IllegalArgumentException("Invalid date in CPR: " + value);
        }
    }

    private static boolean isValidFormat(String value) {
        return value != null && value.matches("\\d{10}");
    }

    public String formatted() {
        // Adds a dash after the date part: DDMMYY-XXXX
        return value.substring(0, 6) + "-" + value.substring(6);
    }

    private static boolean isValidDate(String value) {
        try {
            // Parse date part
            String datePart = value.substring(0, 6);
            int serial7 = Character.getNumericValue(value.charAt(6));
            int year = extractFullYear(datePart.substring(4), serial7);
            int month = Integer.parseInt(datePart.substring(2, 4));
            int day = Integer.parseInt(datePart.substring(0, 2));
            LocalDate.of(year, month, day); // throws if invalid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // CPR logic: use the 7th digit to infer century
    private static int extractFullYear(String yy, int serial7) {
        int year = Integer.parseInt(yy);
        if (serial7 <= 3) {
            return 1900 + year;
        } else if (serial7 == 4 || serial7 == 9) {
            return (year < 37) ? 2000 + year : 1900 + year;
        } else if (serial7 >= 5 && serial7 <= 8) {
            return (year < 58) ? 2000 + year : 1800 + year;
        }
        throw new IllegalArgumentException("Invalid serial digit in CPR: " + serial7);
    }

    public boolean isMale() {
        int lastDigit = Character.getNumericValue(value.charAt(9));
        return lastDigit % 2 == 1;
    }

    public boolean isFemale() {
        return !isMale();
    }

    @Override
    public String toString() {
        return value;
    }
}

