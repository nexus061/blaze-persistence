/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.impl.metamodel.attribute;

import com.blazebit.persistence.view.impl.metamodel.EmbeddableOwner;
import com.blazebit.persistence.view.impl.metamodel.ManagedViewTypeImplementor;
import com.blazebit.persistence.view.impl.metamodel.MetamodelBuildingContext;
import com.blazebit.persistence.view.impl.metamodel.MethodAttributeMapping;
import com.blazebit.persistence.view.metamodel.MappingAttribute;
import com.blazebit.persistence.view.metamodel.MethodMultiListAttribute;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Christian Beikov
 * @since 1.5.0
 */
public class MappingMethodMultiListAttribute<X, Y, C extends Collection<Y>> extends AbstractMethodListAttribute<X, C> implements MappingAttribute<X, List<C>>, MethodMultiListAttribute<X, Y, C> {

    public MappingMethodMultiListAttribute(ManagedViewTypeImplementor<X> viewType, MethodAttributeMapping mapping, MetamodelBuildingContext context, int attributeIndex, int dirtyStateIndex, EmbeddableOwner embeddableMapping) {
        super(viewType, mapping, context, attributeIndex, dirtyStateIndex, embeddableMapping);
    }

    @Override
    public boolean isCorrelated() {
        return false;
    }
}
