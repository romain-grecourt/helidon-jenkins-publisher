package io.helidon.build.publisher.plugin;

import java.util.Objects;

/**
 * Wildcard matcher.
 */
final class Matcher {

    private static final char WILDCARD = '*';

    private Matcher() {
    }

    /**
     * Matches the given value with a pattern that may contain wildcard(s)
     * character that can filter any sub-sequence in the value.
     *
     * @param val the string to filter
     * @param pattern the pattern to use for matching
     * @return returns {@code true} if pattern matches, {@code false} otherwise
     */
    public static boolean match(String val, String pattern) {

        Objects.requireNonNull(val);
        Objects.requireNonNull(pattern);

        if (pattern.isEmpty()) {
            // special case for empty pattern
            // matches if val is also empty
            return val.isEmpty();
        }

        int valIdx = 0;
        int patternIdx = 0;
        boolean matched = true;
        while (matched) {
            int wildcardIdx = pattern.indexOf(WILDCARD, patternIdx);
            if (wildcardIdx >= 0) {
                // pattern has unprocessed wildcard(s)
                int patternOffset = wildcardIdx - patternIdx;
                if (patternOffset > 0) {
                    // filter the sub pattern before the wildcard
                    String subPattern = pattern.substring(patternIdx, wildcardIdx);
                    int idx = val.indexOf(subPattern, valIdx);
                    if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD) {
                        // if expanding a wildcard
                        // the sub-segment needs to contain the sub-pattern
                        if (idx < valIdx) {
                            matched = false;
                            break;
                        }
                    } else if (idx != valIdx) {
                        // not expanding a wildcard
                        // the sub-segment needs to start with the sub-pattern
                        matched = false;
                        break;
                    }
                    valIdx = idx + subPattern.length();
                }
                patternIdx = wildcardIdx + 1;
            } else {
                String subPattern = pattern.substring(patternIdx);
                String subSegment = val.substring(valIdx);
                if (patternIdx > 0 && pattern.charAt(patternIdx - 1) == WILDCARD) {
                    // if expanding a wildcard
                    // sub-segment needs to end with sub-pattern
                    if (!subSegment.endsWith(subPattern)) {
                        matched = false;
                    }
                } else if (!subSegment.equals(subPattern)) {
                    // not expanding a wildcard
                    // the sub-segment needs to stricly filter the sub-pattern
                    matched = false;
                }
                break;
            }
        }
        return matched;
    }
}
