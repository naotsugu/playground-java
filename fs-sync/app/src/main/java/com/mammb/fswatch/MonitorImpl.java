/*
 * Copyright 2026-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mammb.fswatch;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MonitorImpl implements Monitor {

    /** List of listeners. */
    private final transient List<Event.Listener> listeners = new CopyOnWriteArrayList<>();

    private Path root;

    /**
     * Adds a file system listener.
     * @param listener The file system listener.
     */
    public void addListener(final Event.Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a file system listener.
     * @param listener The file system listener.
     */
    public void removeListener(final Event.Listener listener) {
        if (listener != null) {
            listeners.removeIf(listener::equals);
        }
    }

}
