/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.testsuite.update.subview.inverse.subtype.model;

import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.view.EntityView;

/**
 *
 * @author Christian Beikov
 * @since 1.3.0
 */
@EntityView(Document.class)
public interface DocumentIdView extends IdHolderView<Long> {

}
