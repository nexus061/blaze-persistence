/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.testsuite.update.subview.nested.creatable.model;

import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.UpdatableEntityView;

import java.util.Date;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
@UpdatableEntityView
@EntityView(Document.class)
public abstract class UpdatableDocumentView {
    
    @IdMapping
    public abstract Long getId();

    public abstract Long getVersion();

    public abstract String getName();

    public abstract void setName(String name);

    public abstract Date getLastModified();
    
    public abstract void setLastModified(Date date);

    public abstract UpdatableResponsiblePersonView getResponsiblePerson();

    public abstract void setResponsiblePerson(UpdatableResponsiblePersonView responsiblePerson);

}
