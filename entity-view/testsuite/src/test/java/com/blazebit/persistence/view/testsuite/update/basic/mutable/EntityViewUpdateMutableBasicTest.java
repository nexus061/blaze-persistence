/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.view.testsuite.update.basic.mutable;

import com.blazebit.persistence.testsuite.base.jpa.assertion.AssertStatementBuilder;
import com.blazebit.persistence.testsuite.base.jpa.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.jpa.category.NoEclipselink;
import com.blazebit.persistence.testsuite.entity.Document;
import com.blazebit.persistence.view.FlushMode;
import com.blazebit.persistence.view.FlushStrategy;
import com.blazebit.persistence.view.change.ChangeModel;
import com.blazebit.persistence.view.change.SingularChangeModel;
import com.blazebit.persistence.view.spi.type.MutableStateTrackable;
import com.blazebit.persistence.view.testsuite.update.basic.AbstractEntityViewUpdateBasicTest;
import com.blazebit.persistence.view.testsuite.update.basic.mutable.model.UpdatableDocumentBasicView;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.*;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
@RunWith(Parameterized.class)
// NOTE: No Datanucleus support yet
@Category({ NoDatanucleus.class, NoEclipselink.class})
public class EntityViewUpdateMutableBasicTest extends AbstractEntityViewUpdateBasicTest<UpdatableDocumentBasicView> {

    public EntityViewUpdateMutableBasicTest(FlushMode mode, FlushStrategy strategy, boolean version) {
        super(mode, strategy, version, UpdatableDocumentBasicView.class);
    }

    @Parameterized.Parameters(name = "{0} - {1} - VERSIONED={2}")
    public static Object[][] combinations() {
        return MODE_STRATEGY_VERSION_COMBINATIONS;
    }

    @Test
    public void testSaveTo() {
        final UpdatableDocumentBasicView docView = getDoc1View();

        // When
        docView.setName("newDoc");
        saveTo(docView, doc1);

        // Then
        assertEquals("newDoc", doc1.getName());
    }

    @Test
    public void testSimpleUpdate() {
        // Given & When
        final UpdatableDocumentBasicView docView = simpleUpdate();
        Long oldVersion = docView.getVersion();
        if (!isFullMode()) {
            SingularChangeModel<UpdatableDocumentBasicView> changeModel = evm.getChangeModel(docView);
            assertTrue(changeModel.isDirty());
            assertEquals(ChangeModel.ChangeKind.MUTATED, changeModel.getKind());

            ChangeModel<?> nameChange = changeModel.get("name");
            assertTrue(nameChange.isDirty());
            assertEquals(ChangeModel.ChangeKind.UPDATED, nameChange.getKind());

            assertEquals("doc1", nameChange.getInitialState());
            assertEquals("newDoc", nameChange.getCurrentState());
            assertEquals(Arrays.asList(nameChange), changeModel.getDirtyChanges());
        }
        updateAndAssertChangesFlushed(docView);

        // Then
        assertVersionDiff(oldVersion, docView.getVersion(), 1, 1);
        fullFetchUpdateAndReload(docView);
        assertVersionDiff(oldVersion, docView.getVersion(), 1, isQueryStrategy() ? 2 : 1);
        assertEquals("newDoc", docView.getName());
        assertEquals(doc1.getName(), docView.getName());
        assertEquals(doc1.getVersion(), docView.getVersion());
    }

    @Test
    public void testUpdateMutable() {
        // Given & When
        final UpdatableDocumentBasicView docView = updateMutable();
        Long oldVersion = docView.getVersion();
        if (!isFullMode()) {
            SingularChangeModel<UpdatableDocumentBasicView> changeModel = evm.getChangeModel(docView);
            assertTrue(changeModel.isDirty());
            assertEquals(ChangeModel.ChangeKind.MUTATED, changeModel.getKind());

            ChangeModel<?> lastModifiedChange = changeModel.get("lastModified");
            assertTrue(lastModifiedChange.isDirty());
            assertEquals(ChangeModel.ChangeKind.UPDATED, lastModifiedChange.getKind());

            assertEquals(new Date(EPOCH_2K), lastModifiedChange.getInitialState());
            assertEquals(new Date(0), lastModifiedChange.getCurrentState());
            assertEquals(Arrays.asList(lastModifiedChange), changeModel.getDirtyChanges());
        }
        updateAndAssertChangesFlushed(docView);

        // Then
        assertVersionDiff(oldVersion, docView.getVersion(), 1, 1);
        fullFetchUpdateAndReload(docView);
        assertVersionDiff(oldVersion, docView.getVersion(), 1, isQueryStrategy() ? 2 : 1);
        assertEquals(0, doc1.getLastModified().getTime());
        assertEquals(doc1.getVersion(), docView.getVersion());
    }

    @Test
    public void testMutateMutable() {
        // Given & When
        final UpdatableDocumentBasicView docView = mutateMutable();
        Long oldVersion = docView.getVersion();
        if (!isFullMode()) {
            SingularChangeModel<UpdatableDocumentBasicView> changeModel = evm.getChangeModel(docView);
            assertTrue(changeModel.isDirty());
            assertEquals(ChangeModel.ChangeKind.MUTATED, changeModel.getKind());

            ChangeModel<?> lastModifiedChange = changeModel.get("lastModified");
            assertTrue(lastModifiedChange.isDirty());
            assertEquals(ChangeModel.ChangeKind.UPDATED, lastModifiedChange.getKind());

            assertEquals(new Date(EPOCH_2K), lastModifiedChange.getInitialState());
            assertEquals(new Date(0), lastModifiedChange.getCurrentState());
            assertEquals(Arrays.asList(lastModifiedChange), changeModel.getDirtyChanges());
        }
        updateAndAssertChangesFlushed(docView);

        // Then
        assertVersionDiff(oldVersion, docView.getVersion(), 1, 1);
        fullFetchUpdateAndReload(docView);
        assertVersionDiff(oldVersion, docView.getVersion(), 1, isQueryStrategy() ? 2 : 1);
        assertEquals(0, doc1.getLastModified().getTime());
        assertEquals(doc1.getVersion(), docView.getVersion());
    }

    @Test
    public void testUpdateViaReference() {
        doc1.setArchived(true);
        final UpdatableDocumentBasicView docView = evm.getReference(UpdatableDocumentBasicView.class, doc1.getId());
        ((MutableStateTrackable) docView).$$_setVersion(doc1.getVersion());

        // When
        docView.setName("doc1");
        docView.setArchived(false);
        update(docView);

        // Then
        clearPersistenceContextAndReload();
        assertEquals("doc1", doc1.getName());
        assertEquals(false, doc1.isArchived());
        if (isFullMode()) {
            assertNull(doc1.getLastModified());
        } else {
            assertNotNull(doc1.getLastModified());
        }
    }

    private void fullFetchUpdateAndReload(UpdatableDocumentBasicView docView) {
        // Assert that not only the document is loaded and finally also updated
        AssertStatementBuilder builder = assertUnorderedQuerySequence();

        if (!isQueryStrategy()) {
            fullFetch(builder);
        }

        builder.update(Document.class)
                .validate();

        assertNoUpdateAndReload(docView);
    }

    @Override
    protected AssertStatementBuilder fullFetch(AssertStatementBuilder builder) {
        return builder.assertSelect()
                .fetching(Document.class)
                .and();
    }

    @Override
    protected AssertStatementBuilder fullUpdate(AssertStatementBuilder builder) {
        return builder.assertUpdate()
                .forEntity(Document.class)
                .and();
    }

    @Override
    protected AssertStatementBuilder versionUpdate(AssertStatementBuilder builder) {
        return builder.update(Document.class);
    }

}
