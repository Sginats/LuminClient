package com.lumin.luminclient.auto;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Human-behavior timing model.
 *
 * The whole point is to NOT look like a bot. Real humans:
 *  - have variable reaction times (not a fixed 200ms)
 *  - occasionally pause to read / think
 *  - have "bursts" of fast actions and then slow down
 *  - take breaks between sessions
 *  - are slower when tired / distracted
 *
 * This class produces delays sampled from realistic distributions.
 *
 * Design:
 *  - Reaction delay: log-normal-ish (fast but variable, right-skewed like real humans)
 *  - Thinking pause: occasional long delay before a decision
 *  - Micro-jitter: small random variation on every action
 *  - Session fatigue: reaction time creeps up the longer a session runs
 *  - Session breaks: hard stop after a max session length, then a long cool-down
 */
public final class HumanClock {

    // Base reaction time between GUI actions (ms)
    private final double baseMeanMs;
    private final double baseStdDevMs;

    // Probability of an extra "thinking" pause before an action
    private final double thinkProbability;
    private final long thinkMinMs;
    private final long thinkMaxMs;

    // Session limits
    private final long maxSessionMs;       // how long we can run before a break
    private final long minBreakMs;         // shortest break between sessions
    private final long maxBreakMs;         // longest break between sessions

    // Fatigue: how much slower we get per minute of session
    private final double fatiguePerMinuteMs;

    private long sessionStartMs = 0L;
    private long breakUntilMs = 0L;
    private boolean inBreak = false;

    public HumanClock(double baseMeanMs,
                      double baseStdDevMs,
                      double thinkProbability,
                      long thinkMinMs,
                      long thinkMaxMs,
                      long maxSessionMs,
                      long minBreakMs,
                      long maxBreakMs,
                      double fatiguePerMinuteMs) {
        this.baseMeanMs = baseMeanMs;
        this.baseStdDevMs = baseStdDevMs;
        this.thinkProbability = thinkProbability;
        this.thinkMinMs = thinkMinMs;
        this.thinkMaxMs = thinkMaxMs;
        this.maxSessionMs = maxSessionMs;
        this.minBreakMs = minBreakMs;
        this.maxBreakMs = maxBreakMs;
        this.fatiguePerMinuteMs = fatiguePerMinuteMs;
    }

    /** Default "believable human" profile. */
    public static HumanClock defaultProfile() {
        return new HumanClock(
                280.0,   // mean reaction ~280ms
                90.0,    // stddev ~90ms
                0.15,    // 15% of actions get an extra thinking pause
                400,     // thinking pause 0.4s - 2.5s
                2500,
                20 * 60_000L,  // 20 min max session
                45 * 60_000L,  // 45 min min break
                90 * 60_000L,  // 90 min max break
                2.0            // +2ms per minute of fatigue
        );
    }

    /** Start a new session (called after a break ends). */
    public void startSession(long nowMs) {
        sessionStartMs = nowMs;
        inBreak = false;
        breakUntilMs = 0L;
    }

    /** Check if we're currently in a mandatory break. */
    public boolean isInBreak(long nowMs) {
        if (inBreak) {
            if (nowMs >= breakUntilMs) {
                startSession(nowMs);
                return false;
            }
            return true;
        }
        // Not in break: check if we've exceeded max session length
        if (sessionStartMs > 0 && nowMs - sessionStartMs >= maxSessionMs) {
            enterBreak(nowMs);
            return true;
        }
        return false;
    }

    private void enterBreak(long nowMs) {
        inBreak = true;
        long breakLen = ThreadLocalRandom.current().nextLong(minBreakMs, maxBreakMs + 1);
        breakUntilMs = nowMs + breakLen;
    }

    /** Milliseconds until the current break ends, or 0 if not in break. */
    public long breakRemainingMs(long nowMs) {
        return inBreak ? Math.max(0, breakUntilMs - nowMs) : 0L;
    }

    /** Total delay before the next action, including all human factors. */
    public long nextActionDelayMs(long nowMs) {
        double reaction = sampleLogNormal(baseMeanMs, baseStdDevMs);
        double fatigue = fatigueMs(nowMs);
        double think = maybeThinkDelay();
        double jitter = ThreadLocalRandom.current().nextDouble(-25.0, 35.0);
        long total = (long) Math.max(80.0, reaction + fatigue + think + jitter);
        return total;
    }

    private double fatigueMs(long nowMs) {
        if (sessionStartMs <= 0) return 0.0;
        long minutes = Math.max(0, (nowMs - sessionStartMs) / 60_000L);
        return minutes * fatiguePerMinuteMs;
    }

    private double maybeThinkDelay() {
        Random r = ThreadLocalRandom.current();
        if (r.nextDouble() < thinkProbability) {
            return r.nextLong(thinkMinMs, thinkMaxMs + 1);
        }
        return 0.0;
    }

    /** Approximate log-normal sample (right-skewed, always positive). */
    private static double sampleLogNormal(double mean, double stdDev) {
        Random r = ThreadLocalRandom.current();
        // Convert to log-normal parameters
        double variance = stdDev * stdDev;
        double mu = Math.log(mean * mean / Math.sqrt(variance + mean * mean));
        double sigma = Math.sqrt(Math.log(1 + variance / (mean * mean)));
        double z = r.nextGaussian();
        return Math.exp(mu + sigma * z);
    }
}
