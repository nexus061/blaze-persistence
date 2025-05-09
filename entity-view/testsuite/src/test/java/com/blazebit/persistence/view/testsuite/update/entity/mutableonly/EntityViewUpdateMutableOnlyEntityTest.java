/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.testsuite.update.entity.mutableonly;

import com.blazebit.persistence.testsuite.base.jpa.assertion.AssertStatementBuilder;
import com.blazebit.persistence.testsuite.base.jpa.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.jpa.category.NoEclipselink;
import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.testsuite.entity.Person;
import com.blazebit.persistence.view.FlushMode;
import com.blazebit.persistence.view.FlushStrategy;
import com.blazebit.persistence.view.testsuite.update.entity.AbstractEntityViewUpdateEntityTest;
import com.blazebit.persistence.view.testsuite.update.entity.mutableonly.model.UpdatableDocumentEntityView;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
@RunWith(Parameterized.class)
// NOTE: No Datanucleus support yet
@Category({ NoDatanucleus.class, NoEclipselink.class})
public class EntityViewUpdateMutableOnlyEntityTest extends AbstractEntityViewUpdateEntityTest<UpdatableDocumentEntityView> {

    public EntityViewUpdateMutableOnlyEntityTest(FlushMode mode, FlushStrategy strategy, boolean version) {
        super(mode, strategy, version, UpdatableDocumentEntityView.class);
    }

    @Test
    public void testSimpleUpdate() {
        // Given & When
        final UpdatableDocumentEntityView docView = simpleUpdate();

        // Then
        // Assert that not only the document is loaded, but also always the responsiblePerson
        // This might be unexpected for partial strategies
        // but since we don't know if entities are dirty, we need to be conservative and load the object
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (isQueryStrategy()) {
            builder.assertSelect()
                    .fetching(Person.class)
                    .and();
        } else {
            fullFetch(builder);
        }

        builder.update(Document.class)
                .validate();

        // Unfortunately, even after an update, we have to reload the entity to merge again
        AssertStatementBuilder afterBuilder = assertQueriesAfterUpdate(docView);
        if (isQueryStrategy()) {
            afterBuilder.assertSelect()
                    .fetching(Person.class)
                    .and();
            if (isFullMode()) {
                afterBuilder.update(Document.class);
            } else if (version) {
                versionUpdate(afterBuilder);
            }
        } else {
            afterBuilder.assertSelect()
                    .fetching(Document.class, Person.class)
                    .and();
            if (version) {
                afterBuilder.update(Document.class);
            }
        }
        afterBuilder.validate();
        assertEquals("newDoc", docView.getName());
        assertEquals(doc1.getName(), docView.getName());
    }

    @Test
    public void testUpdateWithEntity() {
        // Given & When
        try {
            updateWithEntity();
            fail("Expected the setter of a mutable only field to fail!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Updating the mutable-only attribute 'responsiblePerson'"));
        }
    }

    @Test
    public void testUpdateWithModifyExisting() {
        // Given & When
        final UpdatableDocumentEntityView docView = updateWithModifyExisting();

        // Then
        // Since we update the old responsiblePerson, load it along with the document for updating it later
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (isQueryStrategy()) {
            builder.assertSelect()
                    .fetching(Person.class)
                    .and();
            if (isFullMode()) {
                builder.update(Document.class);
            } else if (version) {
                versionUpdate(builder);
            }
        } else {
            fullFetch(builder);
            if (version) {
                versionUpdate(builder);
            }
        }

        builder.update(Person.class)
                .validate();

        AssertStatementBuilder afterBuilder = assertQueriesAfterUpdate(docView);
        if (isQueryStrategy()) {
            afterBuilder.assertSelect()
                    .fetching(Person.class)
                    .and();
            if (isFullMode()) {
                afterBuilder.update(Document.class);
            } else if (version) {
                versionUpdate(afterBuilder);
            }
        } else {
            afterBuilder.assertSelect()
                    .fetching(Document.class, Person.class)
                    .and();
            if (version) {
                afterBuilder.update(Document.class);
            }
        }
        afterBuilder.validate();
        assertEquals(doc1.getResponsiblePerson().getId(), docView.getResponsiblePerson().getId());
        assertEquals("newOwner", doc1.getResponsiblePerson().getName());
    }

    @Test
    public void testUpdateToNull() {
        // Given & When
        try {
            updateToNull();
            fail("Expected the setter of a mutable only field to fail!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Updating the mutable-only attribute 'responsiblePerson'"));
        }
    }

    @Override
    protected AssertStatementBuilder fullFetch(AssertStatementBuilder builder) {
        return builder.assertSelect()
                .fetching(Document.class)
                .fetching(Person.class)
                .and();
    }

    @Override
    protected AssertStatementBuilder fullUpdate(AssertStatementBuilder builder) {
        return builder.update(Document.class);
    }

    @Override
    protected AssertStatementBuilder versionUpdate(AssertStatementBuilder builder) {
        return builder.update(Document.class);
    }
}
