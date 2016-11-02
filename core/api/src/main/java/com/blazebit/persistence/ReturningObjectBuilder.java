/*
 * Copyright 2014 - 2016 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence;

import java.util.List;

/**
 * An object build provides the returning bindings that should be used by a {@link ReturningModificationCriteriaBuilder} and provides methods for
 * transforming tuples into the target type <code>T</code>.
 *
 * @param <T> The type that this builder produces
 * @author Christian Beikov
 * @since 1.1.0
 */
public interface ReturningObjectBuilder<T> {

    /**
     * Applies the returning bindings necessary for this object builder to work.
     *
     * @param returningBuilder The returningBuilder on which to apply the returning bindings
     */
    public <X extends ReturningBuilder<X>> void applyReturning(X returningBuilder);

    /**
     * Builds an object of the target type <code>T</code> from the given tuple.
     *
     * @param tuple The result tuple
     * @return The target object
     */
    public T build(Object[] tuple);

    /**
     * Transforms the given list and returns the result.
     *
     * @param list The list to be transformed
     * @return The resulting list
     */
    public List<T> buildList(List<T> list);
}
