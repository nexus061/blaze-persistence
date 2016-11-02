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

package com.blazebit.persistence.impl.openjpa;

import com.blazebit.apt.service.ServiceProvider;
import com.blazebit.persistence.ObjectBuilder;
import com.blazebit.persistence.spi.QueryTransformer;
import java.util.logging.Logger;
import javax.persistence.TypedQuery;
import org.apache.openjpa.persistence.OpenJPAQuerySPI;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
@ServiceProvider(QueryTransformer.class)
public class OpenJPAQueryTransformer implements QueryTransformer {
    
    public OpenJPAQueryTransformer() {
        Logger.getLogger(OpenJPAQueryTransformer.class.getName()).warning("The OpenJPA integration is experimental and should not be used in production yet!");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> TypedQuery<X> transformQuery(TypedQuery<?> query, ObjectBuilder<X> objectBuilder) {
        OpenJPAQuerySPI<?> nativeQuery = query.unwrap(OpenJPAQuerySPI.class);
        nativeQuery.addAggregateListener(new ObjectBuilderAggregateListenerAdapter(objectBuilder));
        return (TypedQuery<X>) query;
    }

}
