package com.haekitchenapp.recipeapp.utility;

public class QuantityUtils {

    // Float to fraction string
    public static String floatToFraction(float value) {
        if (value == 0f) return "0";

        int whole = (int) Math.floor(value);
        float fractional = value - whole;

        if (fractional == 0) {
            return String.valueOf(whole);
        }

        // Common kitchen denominators
        int[] denominators = {2, 3, 4, 8, 16};

        int bestNumerator = 0;
        int bestDenominator = 1;
        float bestDiff = Float.MAX_VALUE;

        for (int denom : denominators) {
            int num = Math.round(fractional * denom);
            float approx = (float) num / denom;
            float diff = Math.abs(fractional - approx);

            if (diff < bestDiff) {
                bestDiff = diff;
                bestNumerator = num;
                bestDenominator = denom;
            }
        }

        // Reduce fraction
        int gcd = gcd(bestNumerator, bestDenominator);
        bestNumerator /= gcd;
        bestDenominator /= gcd;

        // Handle whole + fraction
        if (bestNumerator == 0) {
            return String.valueOf(whole);
        } else if (whole > 0) {
            return whole + " " + bestNumerator + "/" + bestDenominator;
        } else {
            return bestNumerator + "/" + bestDenominator;
        }
    }



    // Fraction string to float
    public static float fractionToFloat(String quantity) {
        if (quantity == null || quantity.trim().isEmpty() ||
                "to taste".equalsIgnoreCase(quantity.trim())) {
            return 0.0f;
        }

        quantity = quantity.trim();

        try {
            // Check for mixed fraction like "1 1/4"
            if (quantity.contains(" ")) {
                String[] parts = quantity.split(" ");
                if (parts.length == 2) {
                    float whole = Float.parseFloat(parts[0]);
                    float frac = fractionToFloat(parts[1]); // reuse logic
                    return whole + frac;
                }
            }

            // Check for simple fraction "1/4"
            if (quantity.contains("/")) {
                String[] parts = quantity.split("/");
                if (parts.length == 2) {
                    float numerator = Float.parseFloat(parts[0].trim());
                    float denominator = Float.parseFloat(parts[1].trim());
                    if (denominator == 0) return 0.0f;
                    return numerator / denominator;
                }
            }

            // Otherwise it's decimal or integer
            return Float.parseFloat(quantity);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }


    // Helper to simplify fractions
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
