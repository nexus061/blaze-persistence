/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.impl.entity;

import com.blazebit.persistence.view.impl.EntityViewManagerImpl;
import com.blazebit.persistence.view.impl.accessor.AttributeAccessor;
import com.blazebit.persistence.view.impl.update.EntityViewUpdaterImpl;
import com.blazebit.persistence.view.spi.type.EntityViewProxy;
import com.blazebit.persistence.view.impl.update.UpdateContext;
import com.blazebit.persistence.view.metamodel.Type;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class LoadOrPersistViewToEntityMapper extends AbstractViewToEntityMapper {

    public LoadOrPersistViewToEntityMapper(String attributeLocation, EntityViewManagerImpl evm, Class<?> viewTypeClass, Set<Type<?>> readOnlyAllowedSubtypes, Set<Type<?>> persistAllowedSubtypes, Set<Type<?>> updateAllowedSubtypes,
                                           EntityLoader entityLoader, AttributeAccessor viewIdAccessor, AttributeAccessor entityIdAccessor, boolean persistAllowed, EntityViewUpdaterImpl owner, String ownerMapping, Map<Object, EntityViewUpdaterImpl> localCache) {
        super(attributeLocation, evm, viewTypeClass, readOnlyAllowedSubtypes, persistAllowedSubtypes, updateAllowedSubtypes, entityLoader, viewIdAccessor, entityIdAccessor, persistAllowed, owner, ownerMapping, localCache);
    }

    @Override
    public Object applyToEntity(UpdateContext context, Object entity, Object view) {
        Object object = flushToEntity(context, entity, view);
        if (object == null) {
            return loadEntity(context, view);
        }

        return object;
    }

    @Override
    public Object flushToEntity(UpdateContext context, Object entity, Object view) {
        if (view == null) {
            return null;
        }

        if (viewIdAccessor != null) {
            Object id = viewIdAccessor.getValue(view);

            if (shouldPersist(view, id)) {
                return persist(context, entity, view);
            }

            Class<?> viewTypeClass = getViewTypeClass(view);
            // If the view is read only, just skip to loading
            if (!viewTypeClasses.contains(viewTypeClass)) {
                // If not, check if it was persisted before
                if (persistAllowed && persistUpdater.containsKey(viewTypeClass) && !((EntityViewProxy) view).$$_isNew()) {
                    // If that create view object was previously persisted, we won't persist it again, nor update, but just load it
                } else {
                    throw new IllegalArgumentException("Couldn't load entity object for attribute '" + attributeLocation + "'. Expected subview of one of the types " + names(viewTypeClasses) + " but got: " + view);
                }
            }
        }
        return null;
    }
}
