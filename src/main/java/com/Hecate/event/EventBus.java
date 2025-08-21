package com.Hecate.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 事件总线 - 用于模块间通信
 */
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * 订阅指定类型的事件
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * 发布事件
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<Consumer<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<?> listener : eventListeners) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    /**
     * 取消订阅事件
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
}