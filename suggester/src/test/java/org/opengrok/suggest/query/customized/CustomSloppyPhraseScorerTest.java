/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 */
package org.opengrok.suggest.query.customized;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TwoPhaseIterator;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;
import org.opengrok.suggest.query.PhraseScorer;
import org.opengrok.suggest.query.data.HashIntsHolder;

import java.io.IOException;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

public class CustomSloppyPhraseScorerTest {

    public static void test(
            final int slop,
            final int offset,
            final String[] terms,
            final Integer[] expectedPositions
    ) throws IOException {
        RAMDirectory dir = new RAMDirectory();

        try (IndexWriter iw = new IndexWriter(dir, new IndexWriterConfig())) {
            Document doc = new Document();
            doc.add(new TextField("test", "zero one two three four five six seven eight nine ten", Field.Store.NO));

            iw.addDocument(doc);
        }

        CustomPhraseQuery query = new CustomPhraseQuery(slop, "test", terms);
        query.offset = offset;

        try (IndexReader ir = DirectoryReader.open(dir)) {
            IndexSearcher is = new IndexSearcher(ir);

            Weight w = query.createWeight(is, false, 1);

            LeafReaderContext context = ir.getContext().leaves().get(0);

            Scorer scorer = w.scorer(context);

            TwoPhaseIterator it = scorer.twoPhaseIterator();

            int correctDoc = -1;

            int docId;
            while ((docId = it.approximation().nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                if (it.matches()) {
                    correctDoc = docId;
                }
            }

            assertThat((HashIntsHolder) ((PhraseScorer) scorer).getPositions(correctDoc), contains(expectedPositions));
        }
    }

    @Test
    public void simpleTestAfter() throws IOException {
        test(2, 2, new String[] {"five", "six"}, new Integer[] {7, 8, 9});
    }

    @Test
    public void simpleTestBefore() throws IOException {
        test(2, -1, new String[] {"five", "six"}, new Integer[] {2, 3, 4});
    }

}
