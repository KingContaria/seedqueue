package me.contaria.seedqueue.debug;

import java.util.HashSet;
import java.util.function.Consumer;

public class CallbackSet<T> extends HashSet<T> {

    private final Consumer<T> addCallback;
    private final Consumer<T> removeCallback;
    private final Runnable clearCallback;

    public CallbackSet(Consumer<T> addCallback, Consumer<T> removeCallback, Runnable clearCallback) {
        this.addCallback = addCallback;
        this.removeCallback = removeCallback;
        this.clearCallback = clearCallback;
    }

    @Override
    public boolean add(T t) {
        if (super.add(t)) {
            this.addCallback.accept(t);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            this.removeCallback.accept((T) o);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        this.clearCallback.run();
        super.clear();
    }
}
