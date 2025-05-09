/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.impl.collection;

import com.blazebit.persistence.view.impl.entity.MapViewToEntityMapper;
import com.blazebit.persistence.view.impl.update.UpdateContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class MapPutAction<C extends Map<K, V>, K, V> implements MapAction<C> {

    private final K key;
    private final V value;
    private final V removedValueInView;

    public MapPutAction(K key, V value, Map<K, V> delegate) {
        this.key = key;
        this.value = value;
        this.removedValueInView = delegate == null ? null : delegate.get(key);
    }

    public MapPutAction(K key, V value, V removedValueInView) {
        this.key = key;
        this.value = value;
        this.removedValueInView = removedValueInView;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doAction(C map, UpdateContext context, MapViewToEntityMapper mapper, CollectionRemoveListener keyRemoveListener, CollectionRemoveListener valueRemoveListener) {
        V oldValue;
        if (mapper != null) {
            K k = key;
            V v = value;

            if (mapper.getKeyMapper() != null) {
                k = (K) mapper.getKeyMapper().applyToEntity(context, null, k);
            }
            if (mapper.getValueMapper() != null) {
                v = (V) mapper.getValueMapper().applyToEntity(context, null, v);
            }

            oldValue = map.put(k, v);
        } else {
            oldValue = map.put(key, value);
        }

        if (valueRemoveListener != null && oldValue != null) {
            valueRemoveListener.onCollectionRemove(context, removedValueInView);
        }
    }

    @Override
    public void undo(C map, Collection<?> removedKeys, Collection<?> addedKeys, Collection<?> removedElements, Collection<?> addedElements) {
        if (addedKeys.contains(key) || addedElements.contains(value)) {
            map.put(key, removedValueInView);
        }
    }

    @Override
    public Collection<Object> getAddedKeys() {
        return (Collection<Object>) Collections.singleton(key);
    }

    @Override
    public Collection<Object> getRemovedKeys() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Object> getAddedElements() {
        return (Collection<Object>) Collections.singletonList(value);
    }

    @Override
    public Collection<Object> getRemovedElements() {
        if (removedValueInView != null) {
            return (Collection<Object>) Collections.singleton(removedValueInView);
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<Object> getAddedKeys(C collection) {
        return (Collection<Object>) Collections.singleton(key);
    }

    @Override
    public Collection<Object> getRemovedKeys(C collection) {
        return Collections.emptyList();
    }

    @Override
    public Collection<Object> getAddedElements(C collection) {
        return (Collection<Object>) Collections.singletonList(value);
    }

    @Override
    public Collection<Object> getRemovedElements(C collection) {
        V oldValue = collection.get(key);
        if (oldValue != null && !oldValue.equals(value)) {
            return (Collection<Object>) Collections.singleton(oldValue);
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapAction<C> replaceObject(Object oldKey, Object oldValue, Object newKey, Object newValue) {
        if (oldKey == key || oldValue == value) {
            return new MapPutAction(newKey, newValue, removedValueInView);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapAction<C> replaceObjects(Map<Object, Object> objectMapping) {
        if (objectMapping == null) {
            return this;
        }
        Object newKey = objectMapping.get(key);
        Object newValue = objectMapping.get(value);
        Object newRemovedValueInView = objectMapping.get(removedValueInView);
        if (newKey != null) {
            if (newValue != null) {
                if (newRemovedValueInView == null) {
                    return new MapPutAction(newKey, newValue, removedValueInView);
                } else {
                    return new MapPutAction(newKey, newValue, newRemovedValueInView);
                }
            } else if (newRemovedValueInView != null) {
                return new MapPutAction(newKey, value, newRemovedValueInView);
            } else {
                return new MapPutAction(newKey, value, removedValueInView);
            }
        } else if (newValue != null) {
            if (newRemovedValueInView == null) {
                return new MapPutAction(key, newValue, removedValueInView);
            } else {
                return new MapPutAction(key, newValue, newRemovedValueInView);
            }
        } else if (newRemovedValueInView != null) {
            return new MapPutAction(key, value, newRemovedValueInView);
        } else {
            return this;
        }
    }

    @Override
    public void addAction(List<MapAction<C>> actions, Collection<Object> addedKeys, Collection<Object> removedKeys, Collection<Object> addedElements, Collection<Object> removedElements) {
        actions.add(this);
    }
}
