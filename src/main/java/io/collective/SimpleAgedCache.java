package io.collective;

import java.time.Clock;

public class SimpleAgedCache {

    private final Clock clock;
    private ExpirableEntry head;

    private static class ExpirableEntry {
        Object key;
        Object value;
        long expiresAtMillis;
        ExpirableEntry next;

        ExpirableEntry(Object key, Object value, long expiresAtMillis){
            this.key = key;
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
            this.next = null;
        }

        boolean isExpired (long nowMillis){
            return nowMillis >= expiresAtMillis;
        }
    }

    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.head = null;
    }

    public SimpleAgedCache() {
        this(Clock.systemUTC());
    }

    private void cleanup(){
        long now = clock.millis();

        while (head != null && head.isExpired(now)){
            head = head.next;
        }

        ExpirableEntry previous = head;
        ExpirableEntry current = (head != null) ? head.next : null;

        while (current != null){
            if (current.isExpired(now)){
                previous.next = current.next;
                current = previous.next;
            } else {
                previous.next = current;
                current = current.next;
            }
        }
    }

    public void put(Object key, Object value, int retentionInMillis) {
        cleanup();
        long expiresAt = clock.millis() + retentionInMillis;

        ExpirableEntry current = head;
        while (current != null){
            if ((current.key == null && key == null) || (current.key != null && current.key.equals(key))) {
                current.value = value;
                current.expiresAtMillis = expiresAt;
                return;
            }

            current = current.next;
        }

        ExpirableEntry newEntry = new ExpirableEntry(key,value,expiresAt);
        newEntry.next = head;
        head = newEntry;
    }

    public boolean isEmpty() {
        cleanup();
        return head == null;
    }

    public int size() {
        cleanup();
        int count = 0;
        ExpirableEntry current = head;
        while (current != null){
            count++;
            current = current.next;
        }

        return count;
    }

    public Object get(Object key) {
        cleanup();
        ExpirableEntry current = head;

        while (current != null){
            if ((current.key == null && key == null) || (current.key != null && current.key.equals(key))) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }
}