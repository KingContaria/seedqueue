package me.contaria.seedqueue.interfaces;

import java.io.IOException;

public interface SQLevelStorageSession {
    void seedQueue$createLock() throws IOException;
}
