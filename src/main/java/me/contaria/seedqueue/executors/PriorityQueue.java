package me.contaria.seedqueue.executors;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityQueue implements BlockingQueue<Runnable> {
    // stores the last entry any worker thread executed a task from in case the task creates subtasks
    private static final ThreadLocal<SeedQueueEntry> PARENT_ENTRY = new ThreadLocal<>();

    private final Map<SeedQueueEntry, BlockingQueue<Runnable>> queues = new HashMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final Object waitForTasks = new Object();

    // returns the queue for the SeedQueueEntry associated with the calling thread
    private synchronized @NotNull BlockingQueue<Runnable> queue() {
        return this.queues.computeIfAbsent(SeedQueue.getThreadLocalEntry().orElse(PARENT_ENTRY.get()), entry -> new LinkedBlockingQueue<>());
    }

    // returns the queue for the SeedQueueEntry with the highest priority
    private synchronized @Nullable Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> first() {
        PARENT_ENTRY.remove();
        if (this.isEmpty()) {
            return null;
        }

        int priority = Integer.MIN_VALUE;
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = null;
        for (Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> e : this.queues.entrySet()) {
            int p = e.getKey().getPriority();
            if (p > priority && !e.getValue().isEmpty()) {
                priority = p;
                entry = e;
                if (priority >= 100) {
                    break;
                }
            }
        }

        if (entry == null) {
            return null;
        }
        PARENT_ENTRY.set(entry.getKey());
        return entry;
    }

    // checks if the given queue is empty and removes the entry if that's the case
    private synchronized void checkEmpty(Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry) {
        if (entry.getValue().isEmpty()) {
            this.queues.remove(entry.getKey());
        }
    }

    private void increment() {
        this.size.incrementAndGet();
        synchronized (this.waitForTasks) {
            this.waitForTasks.notify();
        }
    }

    private void decrement() {
        this.size.decrementAndGet();
    }

    @Override
    public synchronized boolean add(@NotNull Runnable runnable) {
        if (this.queue().add(runnable)) {
            this.increment();
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean offer(@NotNull Runnable runnable) {
        if (this.queue().offer(runnable)) {
            this.increment();
            return true;
        }
        return false;
    }

    @Override
    public synchronized Runnable remove() {
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = this.first();
        if (entry == null) {
            return null;
        }
        Runnable runnable = entry.getValue().remove();
        this.decrement();
        this.checkEmpty(entry);
        return runnable;
    }

    @Override
    public synchronized Runnable poll() {
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = this.first();
        if (entry == null) {
            return null;
        }
        Runnable runnable = entry.getValue().poll();
        if (runnable != null) {
            this.decrement();
            this.checkEmpty(entry);
        }
        return runnable;
    }

    @Override
    public synchronized Runnable element() {
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = this.first();
        if (entry == null) {
            return null;
        }
        return entry.getValue().element();
    }

    @Override
    public synchronized Runnable peek() {
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = this.first();
        if (entry == null) {
            return null;
        }
        return entry.getValue().peek();
    }

    @Override
    public synchronized void put(@NotNull Runnable runnable) throws InterruptedException {
        this.queue().put(runnable);
        this.increment();
    }

    @Override
    public synchronized boolean offer(Runnable runnable, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        if (this.queue().offer(runnable, timeout, unit)) {
            this.increment();
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public Runnable take() throws InterruptedException {
        Runnable runnable;
        while ((runnable = this.poll()) == null) {
            synchronized (this.waitForTasks) {
                this.waitForTasks.wait();
            }
        }
        return runnable;
    }

    @Nullable
    @Override
    public synchronized Runnable poll(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        Map.Entry<SeedQueueEntry, BlockingQueue<Runnable>> entry = this.first();
        if (entry == null) {
            return null;
        }
        Runnable runnable = entry.getValue().poll(timeout, unit);
        if (runnable != null) {
            this.decrement();
            this.checkEmpty(entry);
        }
        return runnable;
    }

    @Override
    public synchronized boolean remove(Object o) {
        for (BlockingQueue<Runnable> queue : this.queues.values()) {
            if (queue.remove(o)) {
                this.decrement();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        this.queues.clear();
        this.size.set(0);
    }

    @Override
    public int size() {
        return this.size.get();
    }

    @Override
    public boolean isEmpty() {
        return this.size.get() == 0;
    }

    @Override
    public synchronized boolean contains(Object o) {
        for (BlockingQueue<Runnable> queue : this.queues.values()) {
            if (queue.contains(o)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public synchronized Object[] toArray() {
        List<Runnable> list = new ArrayList<>();
        this.drainTo(list);
        return list.toArray();
    }

    @NotNull
    @Override
    public synchronized <T> T[] toArray(@NotNull T[] a) {
        List<Runnable> list = new ArrayList<>();
        this.drainTo(list);
        return list.toArray(a);
    }

    @Override
    public synchronized int drainTo(@NotNull Collection<? super Runnable> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int oldSize = c.size();
        for (BlockingQueue<Runnable> queue : this.queues.values()) {
            c.addAll(queue);
        }
        this.queues.clear();
        return c.size() - oldSize;
    }

    @Override
    public int remainingCapacity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Runnable> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<Runnable> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(@NotNull Collection<? super Runnable> c, int maxElements) {
        throw new UnsupportedOperationException();
    }
}
