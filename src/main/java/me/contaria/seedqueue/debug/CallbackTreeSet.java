package me.contaria.seedqueue.debug;

import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class CallbackTreeSet<T> extends TreeSet<T> {

    private final Consumer<T> addCallback;
    private final Consumer<T> removeCallback;
    private final Runnable clearCallback;

    public CallbackTreeSet(Comparator<? super T> comparator, Consumer<T> addCallback, Consumer<T> removeCallback, Runnable clearCallback) {
        super(comparator);
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
