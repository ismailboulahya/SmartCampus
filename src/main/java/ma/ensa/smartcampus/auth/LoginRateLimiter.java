package ma.ensa.smartcampus.auth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Deque<Long> times = attempts.get(email);
        if (times == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        times.removeIf(time -> now - time > WINDOW_MS);
        return times.size() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {
        attempts.computeIfAbsent(email, key -> new ArrayDeque<>()).add(System.currentTimeMillis());
    }

    public void reset(String email) {
        attempts.remove(email);
    }
}
