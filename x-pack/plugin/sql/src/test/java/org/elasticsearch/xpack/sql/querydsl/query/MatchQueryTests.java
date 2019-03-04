/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.querydsl.query;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.sql.expression.FieldAttribute;
import org.elasticsearch.xpack.sql.expression.predicate.fulltext.MatchQueryPredicate;
import org.elasticsearch.xpack.sql.tree.Location;
import org.elasticsearch.xpack.sql.tree.LocationTests;
import org.elasticsearch.xpack.sql.type.DataType;
import org.elasticsearch.xpack.sql.type.EsField;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static org.elasticsearch.test.EqualsHashCodeTestUtils.checkEqualsAndHashCode;
import static org.elasticsearch.xpack.sql.tree.Location.EMPTY;
import static org.hamcrest.Matchers.equalTo;

public class MatchQueryTests extends ESTestCase {
    static MatchQuery randomMatchQuery() {
        return new MatchQuery(
            LocationTests.randomLocation(),
            randomAlphaOfLength(5),
            randomAlphaOfLength(5));
            // TODO add the predicate
    }

    public void testEqualsAndHashCode() {
        checkEqualsAndHashCode(randomMatchQuery(), MatchQueryTests::copy, MatchQueryTests::mutate);
    }

    private static MatchQuery copy(MatchQuery query) {
        return new MatchQuery(query.location(), query.name(), query.text(), query.predicate());
    }

    private static MatchQuery mutate(MatchQuery query) {
        List<Function<MatchQuery, MatchQuery>> options = Arrays.asList(
            q -> new MatchQuery(LocationTests.mutate(q.location()), q.name(), q.text(), q.predicate()),
            q -> new MatchQuery(q.location(), randomValueOtherThan(q.name(), () -> randomAlphaOfLength(5)), q.text(), q.predicate()),
            q -> new MatchQuery(q.location(), q.name(), randomValueOtherThan(q.text(), () -> randomAlphaOfLength(5)), q.predicate()));
            // TODO mutate the predicate
        return randomFrom(options).apply(query);
    }

    public void testQueryBuilding() {
        MatchQueryBuilder qb = getBuilder("lenient=true");
        assertThat(qb.lenient(), equalTo(true));

        qb = getBuilder("lenient=true;operator=AND");
        assertThat(qb.lenient(), equalTo(true));
        assertThat(qb.operator(), equalTo(Operator.AND));

        Exception e = expectThrows(IllegalArgumentException.class, () -> getBuilder("pizza=yummy"));
        assertThat(e.getMessage(), equalTo("illegal match option [pizza]"));

        e = expectThrows(IllegalArgumentException.class, () -> getBuilder("operator=aoeu"));
        assertThat(e.getMessage(), equalTo("No enum constant org.elasticsearch.index.query.Operator.AOEU"));
    }

    private static MatchQueryBuilder getBuilder(String options) {
        final Location location = new Location(1, 1);
        FieldAttribute fa = new FieldAttribute(EMPTY, "a", new EsField("af", DataType.KEYWORD, emptyMap(), true));
        final MatchQueryPredicate mmqp = new MatchQueryPredicate(location, fa, "eggplant", options);
        final MatchQuery mmq = new MatchQuery(location, "eggplant", "foo", mmqp);
        return (MatchQueryBuilder) mmq.asBuilder();
    }

    public void testToString() {
        final Location location = new Location(1, 1);
        FieldAttribute fa = new FieldAttribute(EMPTY, "a", new EsField("af", DataType.KEYWORD, emptyMap(), true));
        final MatchQueryPredicate mmqp = new MatchQueryPredicate(location, fa, "eggplant", "");
        final MatchQuery mmq = new MatchQuery(location, "eggplant", "foo", mmqp);
        assertEquals("MatchQuery@1:2[eggplant:foo]", mmq.toString());
    }
}
