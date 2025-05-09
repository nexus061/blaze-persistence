/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.criteria.impl;

import com.blazebit.persistence.ConfigurationProperties;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.criteria.BlazeAggregateFunctionExpression;
import com.blazebit.persistence.criteria.BlazeCollectionJoin;
import com.blazebit.persistence.criteria.BlazeCriteriaBuilder;
import com.blazebit.persistence.criteria.BlazeCriteriaDelete;
import com.blazebit.persistence.criteria.BlazeCriteriaQuery;
import com.blazebit.persistence.criteria.BlazeCriteriaUpdate;
import com.blazebit.persistence.criteria.BlazeExpression;
import com.blazebit.persistence.criteria.BlazeFunctionExpression;
import com.blazebit.persistence.criteria.BlazeJoin;
import com.blazebit.persistence.criteria.BlazeListJoin;
import com.blazebit.persistence.criteria.BlazeMapJoin;
import com.blazebit.persistence.criteria.BlazeOrder;
import com.blazebit.persistence.criteria.BlazeOrderedSetAggregateFunctionExpression;
import com.blazebit.persistence.criteria.BlazePath;
import com.blazebit.persistence.criteria.BlazeRoot;
import com.blazebit.persistence.criteria.BlazeSetJoin;
import com.blazebit.persistence.criteria.BlazeWindow;
import com.blazebit.persistence.criteria.BlazeWindowFunctionExpression;
import com.blazebit.persistence.criteria.impl.expression.AbstractExpression;
import com.blazebit.persistence.criteria.impl.expression.AbstractPredicate;
import com.blazebit.persistence.criteria.impl.expression.BetweenPredicate;
import com.blazebit.persistence.criteria.impl.expression.BinaryArithmeticExpression;
import com.blazebit.persistence.criteria.impl.expression.BooleanExpressionPredicate;
import com.blazebit.persistence.criteria.impl.expression.BooleanLiteralPredicate;
import com.blazebit.persistence.criteria.impl.expression.ComparisonPredicate;
import com.blazebit.persistence.criteria.impl.expression.ComparisonPredicate.ComparisonOperator;
import com.blazebit.persistence.criteria.impl.expression.CompoundPredicate;
import com.blazebit.persistence.criteria.impl.expression.CompoundSelectionImpl;
import com.blazebit.persistence.criteria.impl.expression.ExistsPredicate;
import com.blazebit.persistence.criteria.impl.expression.GeneralCaseExpression;
import com.blazebit.persistence.criteria.impl.expression.InPredicate;
import com.blazebit.persistence.criteria.impl.expression.IsEmptyPredicate;
import com.blazebit.persistence.criteria.impl.expression.IsNullPredicate;
import com.blazebit.persistence.criteria.impl.expression.LikePredicate;
import com.blazebit.persistence.criteria.impl.expression.LiteralExpression;
import com.blazebit.persistence.criteria.impl.expression.MemberOfPredicate;
import com.blazebit.persistence.criteria.impl.expression.NotPredicate;
import com.blazebit.persistence.criteria.impl.expression.NullLiteralExpression;
import com.blazebit.persistence.criteria.impl.expression.ParameterExpressionImpl;
import com.blazebit.persistence.criteria.impl.expression.QuantifiableSubqueryExpression;
import com.blazebit.persistence.criteria.impl.expression.SimpleCaseExpression;
import com.blazebit.persistence.criteria.impl.expression.UnaryMinusExpression;
import com.blazebit.persistence.criteria.impl.expression.function.AggregationFunctionExpressionImpl;
import com.blazebit.persistence.criteria.impl.expression.function.CoalesceFunction;
import com.blazebit.persistence.criteria.impl.expression.function.ConcatFunction;
import com.blazebit.persistence.criteria.impl.expression.function.CurrentDateFunction;
import com.blazebit.persistence.criteria.impl.expression.function.CurrentTimeFunction;
import com.blazebit.persistence.criteria.impl.expression.function.CurrentTimestampFunction;
import com.blazebit.persistence.criteria.impl.expression.function.FunctionExpressionImpl;
import com.blazebit.persistence.criteria.impl.expression.function.LocateFunction;
import com.blazebit.persistence.criteria.impl.expression.function.NullifFunction;
import com.blazebit.persistence.criteria.impl.expression.function.OrderedSetAggregationFunction;
import com.blazebit.persistence.criteria.impl.expression.function.SizeFunction;
import com.blazebit.persistence.criteria.impl.expression.function.SubstringFunction;
import com.blazebit.persistence.criteria.impl.expression.function.TrimFunction;
import com.blazebit.persistence.criteria.impl.expression.function.WindowFunctionExpressionImpl;
import com.blazebit.persistence.criteria.impl.path.AbstractJoin;
import com.blazebit.persistence.criteria.impl.path.AbstractPath;
import com.blazebit.persistence.criteria.impl.path.CollectionAttributeJoin;
import com.blazebit.persistence.criteria.impl.path.ListAttributeJoin;
import com.blazebit.persistence.criteria.impl.path.MapAttributeJoin;
import com.blazebit.persistence.criteria.impl.path.PluralAttributePath;
import com.blazebit.persistence.criteria.impl.path.RootImpl;
import com.blazebit.persistence.criteria.impl.path.SetAttributeJoin;
import com.blazebit.persistence.criteria.impl.support.CriteriaBuilderSupport;
import com.blazebit.persistence.parser.EntityMetamodel;

import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Predicate.BooleanOperator;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Christian Beikov
 * @since 1.2.0
 */
public class BlazeCriteriaBuilderImpl implements BlazeCriteriaBuilder, CriteriaBuilderSupport, Serializable {

    private static final long serialVersionUID = 1L;

    private final EntityMetamodel metamodel;
    private final CriteriaBuilderFactory cbf;
    private final boolean negationWrapper;
    private final boolean valueAsParameter;

    public BlazeCriteriaBuilderImpl(CriteriaBuilderFactory cbf) {
        this.metamodel = cbf.getService(EntityMetamodel.class);
        this.cbf = cbf;
        String negationWrapper = cbf.getProperty(ConfigurationProperties.CRITERIA_NEGATION_WRAPPER);
        this.negationWrapper = negationWrapper == null || negationWrapper.isEmpty() || Boolean.parseBoolean(negationWrapper);
        String valueAsParameter = cbf.getProperty(ConfigurationProperties.CRITERIA_VALUE_AS_PARAMETER);
        this.valueAsParameter = valueAsParameter == null || valueAsParameter.isEmpty() || Boolean.parseBoolean(valueAsParameter);
    }

    public EntityMetamodel getEntityMetamodel() {
        return metamodel;
    }

    public CriteriaBuilderFactory getCriteriaBuilderFactory() {
        return cbf;
    }

    public <T extends AbstractPredicate> AbstractPredicate negate(T predicate) {
        if (negationWrapper) {
            return new NotPredicate(this, predicate);
        } else {
            return predicate.copyNegated();
        }
    }

    @Override
    public BlazeCriteriaQuery<Object> createQuery() {
        return new BlazeCriteriaQueryImpl<Object>(this, Object.class);
    }

    @Override
    public <T> BlazeCriteriaQuery<T> createQuery(Class<T> resultClass) {
        return new BlazeCriteriaQueryImpl<T>(this, resultClass);
    }

    @Override
    public BlazeCriteriaQuery<Tuple> createTupleQuery() {
        return new BlazeCriteriaQueryImpl<Tuple>(this, Tuple.class);
    }

    @Override
    public <T> BlazeCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
        return new BlazeCriteriaUpdateImpl<T>(this, targetEntity, null);
    }

    @Override
    public <T> BlazeCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity, String alias) {
        return new BlazeCriteriaUpdateImpl<T>(this, targetEntity, alias);
    }

    @Override
    public <T> BlazeCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
        return new BlazeCriteriaDeleteImpl<T>(this, targetEntity, null);
    }

    @Override
    public <T> BlazeCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity, String alias) {
        return new BlazeCriteriaDeleteImpl<T>(this, targetEntity, alias);
    }

    /**********************
     * Selection stuff
     **********************/

    private List<Selection<?>> wrapSelectionItems(List<Selection<?>> selections) {
        List<Selection<?>> copy = null;

        for (int i = 0; i < selections.size(); i++) {
            Selection<?> selection = selections.get(i);
            if (selection.isCompoundSelection()) {
                // TODO: rethink if this restriction shouldn't be relaxed
                if (selection.getJavaType().isArray()) {
                    throw new IllegalArgumentException("Illegal array selection in multiselect selections");
                }
                if (Tuple.class.isAssignableFrom(selection.getJavaType())) {
                    throw new IllegalArgumentException("Illegal tuple selection in multiselect selections");
                }
            }

            Selection<?> copySelection = wrapSelection(selection);
            if (copy != null) {
                copy.add(copySelection);
            } else if (copySelection != selection) {
                copy = new ArrayList<Selection<?>>();
                for (int j = 0; j < i; j++) {
                    copy.add(selections.get(j));
                }
                copy.add(copySelection);
            }
        }

        if (copy != null) {
            return copy;
        }
        return selections;
    }

    @SuppressWarnings({"unchecked"})
    public <T> Selection<? extends T> wrapSelection(Selection<? extends T> selection) {
        if (selection instanceof Predicate) {
            return (Selection<? extends T>) selectCase().when((Predicate) selection, literal(true)).otherwise(literal(false));
        }

        return selection;
    }

    public <T> BlazeExpression<T> nullValue(Class<T> javaType) {
        if (valueAsParameter) {
            return new ParameterExpressionImpl<>(this, javaType, (T) null);
        } else {
            return new NullLiteralExpression<>(this, javaType);
        }
    }

    public <T> BlazeExpression<T> value(T value) {
        checkValue(value);
        if (valueAsParameter) {
            return new ParameterExpressionImpl<T>(this, value);
        } else {
            return new LiteralExpression<T>(this, value);
        }
    }

    @Override
    public CompoundSelection<Tuple> tuple(Selection<?>... selections) {
        return tuple(Arrays.asList(selections));
    }

    public CompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
        return new CompoundSelectionImpl<Tuple>(this, Tuple.class, wrapSelectionItems(selections));
    }

    @Override
    public CompoundSelection<Object[]> array(Selection<?>... selections) {
        return array(Arrays.asList(selections));
    }

    public CompoundSelection<Object[]> array(List<Selection<?>> selections) {
        return array(Object[].class, selections);
    }

    public <Y> CompoundSelection<Y> array(Class<Y> type, List<Selection<?>> selections) {
        return new CompoundSelectionImpl<Y>(this, type, wrapSelectionItems(selections));
    }

    @Override
    public <Y> CompoundSelection<Y> construct(Class<Y> result, Selection<?>... selections) {
        return construct(result, Arrays.asList(selections));
    }

    public <Y> CompoundSelection<Y> construct(Class<Y> result, List<Selection<?>> selections) {
        return new CompoundSelectionImpl<Y>(this, result, wrapSelectionItems(selections));
    }

    /**********************
     * Order by stuff
     **********************/

    @Override
    public BlazeOrder asc(Expression<?> x) {
        return new OrderImpl(x, true, false);
    }

    @Override
    public BlazeOrder desc(Expression<?> x) {
        return new OrderImpl(x, false, false);
    }

    @Override
    public BlazeOrder asc(Expression<?> x, boolean nullsFirst) {
        return new OrderImpl(x, true, nullsFirst);
    }

    @Override
    public BlazeOrder desc(Expression<?> x, boolean nullsFirst) {
        return new OrderImpl(x, false, nullsFirst);
    }

    /**********************
     * Predicates
     **********************/

    public Predicate wrap(Expression<Boolean> expression) {
        if (expression instanceof Predicate) {
            return (Predicate) expression;
        } else if (expression instanceof AbstractPath<?>) {
            return equal(expression, literal(Boolean.TRUE));
        } else {
            return new BooleanExpressionPredicate(this, false, expression);
        }
    }

    @Override
    public Predicate not(Expression<Boolean> expression) {
        return wrap(expression).not();
    }

    @Override
    public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
        return new CompoundPredicate(this, BooleanOperator.AND, x, y);
    }

    @Override
    public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
        return new CompoundPredicate(this, BooleanOperator.OR, x, y);
    }

    @Override
    public Predicate and(Predicate... restrictions) {
        return new CompoundPredicate(this, BooleanOperator.AND, restrictions);
    }

    @Override
    public Predicate or(Predicate... restrictions) {
        return new CompoundPredicate(this, BooleanOperator.OR, restrictions);
    }

    @Override
    public Predicate conjunction() {
        return new CompoundPredicate(this, BooleanOperator.AND);
    }

    @Override
    public Predicate disjunction() {
        return new CompoundPredicate(this, BooleanOperator.OR);
    }

    @Override
    public Predicate isTrue(Expression<Boolean> expression) {
        if (CompoundPredicate.class.isInstance(expression)) {
            final CompoundPredicate predicate = (CompoundPredicate) expression;
            if (predicate.getExpressions().size() == 0) {
                return new BooleanLiteralPredicate(this, predicate.getOperator() == BooleanOperator.AND);
            }
            return predicate;
        } else if (Predicate.class.isInstance(expression)) {
            return (Predicate) expression;
        }
        return equal(expression, literal(Boolean.TRUE));
    }

    @Override
    public Predicate isFalse(Expression<Boolean> expression) {
        if (CompoundPredicate.class.isInstance(expression)) {
            final CompoundPredicate predicate = (CompoundPredicate) expression;
            if (predicate.getExpressions().size() == 0) {
                return new BooleanLiteralPredicate(this, predicate.getOperator() == BooleanOperator.OR);
            }
            return predicate.not();
        } else if (Predicate.class.isInstance(expression)) {
            final Predicate predicate = (Predicate) expression;
            return predicate.not();
        }
        return equal(expression, literal(Boolean.FALSE));
    }

    @Override
    public Predicate isNull(Expression<?> x) {
        return new IsNullPredicate(this, false, x);
    }

    @Override
    public Predicate isNotNull(Expression<?> x) {
        return new IsNullPredicate(this, true, x);
    }

    @Override
    public Predicate equal(Expression<?> x, Expression<?> y) {
        return new ComparisonPredicate(this, ComparisonOperator.EQUAL, x, y);
    }

    @Override
    public Predicate notEqual(Expression<?> x, Expression<?> y) {
        return new ComparisonPredicate(this, ComparisonOperator.NOT_EQUAL, x, y);
    }

    @Override
    public Predicate equal(Expression<?> x, Object y) {
        return new ComparisonPredicate(this, ComparisonOperator.EQUAL, x, value(y));
    }

    @Override
    public Predicate notEqual(Expression<?> x, Object y) {
        return new ComparisonPredicate(this, ComparisonOperator.NOT_EQUAL, x, value(y));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN, x, y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN, x, y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN, x, value(y));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN, x, value(y));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, value(y));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, value(y));
    }

    @Override
    public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN, x, y);
    }

    @Override
    public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN, x, y);
    }

    @Override
    public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y);
    }

    @Override
    public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y);
    }

    @Override
    public Predicate gt(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN, x, value(y));
    }

    @Override
    public Predicate lt(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN, x, value(y));
    }

    @Override
    public Predicate ge(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate(this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, value(y));
    }

    @Override
    public Predicate le(Expression<? extends Number> x, Number y) {
        return new ComparisonPredicate(this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, value(y));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> expression, Y lowerBound, Y upperBound) {
        return new BetweenPredicate<Y>(this, false, expression, value(lowerBound), value(upperBound));
    }

    @Override
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> expression, Expression<? extends Y> lowerBound, Expression<? extends Y> upperBound) {
        return new BetweenPredicate<Y>(this, false, expression, lowerBound, upperBound);
    }

    @Override
    public <T> In<T> in(Expression<? extends T> expression) {
        return new InPredicate<T>(this, expression);
    }

    @Override
    public Predicate like(Expression<String> matchExpression, Expression<String> pattern) {
        return new LikePredicate(this, false, matchExpression, pattern);
    }

    @Override
    public Predicate like(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
        return new LikePredicate(this, false, matchExpression, pattern, escapeCharacter);
    }

    @Override
    public Predicate like(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
        return new LikePredicate(this, false, matchExpression, pattern, value(escapeCharacter));
    }

    @Override
    public Predicate like(Expression<String> matchExpression, String pattern) {
        return new LikePredicate(this, false, matchExpression, value(pattern));
    }

    @Override
    public Predicate like(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
        return new LikePredicate(this, false, matchExpression, value(pattern), escapeCharacter);
    }

    @Override
    public Predicate like(Expression<String> matchExpression, String pattern, char escapeCharacter) {
        return new LikePredicate(this, false, matchExpression, value(pattern), value(escapeCharacter));
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern) {
        return new LikePredicate(this, true, matchExpression, pattern);
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
        return new LikePredicate(this, true, matchExpression, pattern, escapeCharacter);
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
        return new LikePredicate(this, true, matchExpression, pattern, value(escapeCharacter));
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, String pattern) {
        return new LikePredicate(this, true, matchExpression, value(pattern));
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
        return new LikePredicate(this, true, matchExpression, value(pattern), escapeCharacter);
    }

    @Override
    public Predicate notLike(Expression<String> matchExpression, String pattern, char escapeCharacter) {
        return new LikePredicate(this, true, matchExpression, value(pattern), value(escapeCharacter));
    }

    /**********************
     * Parameter and literal
     **********************/

    @Override
    public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
        return new ParameterExpressionImpl<T>(this, paramClass);
    }

    @Override
    public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
        return new ParameterExpressionImpl<T>(this, paramClass, name);
    }

    @Override
    public <T> AbstractExpression<T> literal(T value) {
        checkValue(value);
        return new LiteralExpression<T>(this, value);
    }

    @Override
    public <T> AbstractExpression<T> nullLiteral(Class<T> resultClass) {
        return new NullLiteralExpression<T>(this, resultClass);
    }

    /**********************
     * Aggregate functions
     **********************/

    @Override
    public <N extends Number> BlazeAggregateFunctionExpression<Double> avg(Expression<N> x) {
        return new AggregationFunctionExpressionImpl<>(this, Double.class, "AVG", false, x);
    }

    @Override
    public <N extends Number> BlazeAggregateFunctionExpression<N> sum(Expression<N> x) {
        return new AggregationFunctionExpressionImpl<>(this, (Class<N>) x.getJavaType(), "SUM", false, x);
    }

    @Override
    public BlazeAggregateFunctionExpression<Long> sumAsLong(Expression<Integer> x) {
        return new AggregationFunctionExpressionImpl<>(this, Long.class, "SUM", false, x);
    }

    @Override
    public BlazeAggregateFunctionExpression<Double> sumAsDouble(Expression<Float> x) {
        return new AggregationFunctionExpressionImpl<>(this, Double.class, "SUM", false, x);
    }

    @Override
    public <N extends Number> BlazeAggregateFunctionExpression<N> max(Expression<N> x) {
        return new AggregationFunctionExpressionImpl<>(this, (Class<N>) x.getJavaType(), "MAX", false, x);
    }

    @Override
    public <N extends Number> BlazeAggregateFunctionExpression<N> min(Expression<N> x) {
        return new AggregationFunctionExpressionImpl<>(this, (Class<N>) x.getJavaType(), "MIN", false, x);
    }

    @Override
    public <X extends Comparable<? super X>> BlazeAggregateFunctionExpression<X> greatest(Expression<X> x) {
        return new AggregationFunctionExpressionImpl<>(this, (Class<X>) x.getJavaType(), "MAX", false, x);
    }

    @Override
    public <X extends Comparable<? super X>> BlazeAggregateFunctionExpression<X> least(Expression<X> x) {
        return new AggregationFunctionExpressionImpl<>(this, (Class<X>) x.getJavaType(), "MIN", false, x);
    }

    @Override
    public BlazeAggregateFunctionExpression<Long> count(Expression<?> x) {
        return new AggregationFunctionExpressionImpl<>(this, Long.class, "COUNT", false, x);
    }

    @Override
    public BlazeAggregateFunctionExpression<Long> countDistinct(Expression<?> x) {
        return new AggregationFunctionExpressionImpl<>(this, Long.class, "COUNT", true, x);
    }

    /**********************
     * Other functions
     **********************/

    @Override
    public <T> BlazeFunctionExpression<T> function(String name, Class<T> returnType, Expression<?>... arguments) {
        return new FunctionExpressionImpl<>(this, returnType, name, arguments);
    }

    @Override
    public <T> BlazeWindowFunctionExpression<T> windowFunction(String name, Class<T> type, Expression<?>... args) {
        return new WindowFunctionExpressionImpl<>(this, type, name, args);
    }

    @Override
    public <T> BlazeWindowFunctionExpression<T> windowDistinctFunction(String name, Class<T> type, Expression<?>... args) {
        return new AggregationFunctionExpressionImpl<>(this, type, name, true, args);
    }

    @Override
    public <T> BlazeAggregateFunctionExpression<T> aggregateFunction(String name, Class<T> type, Expression<?>... args) {
        return new AggregationFunctionExpressionImpl<>(this, type, name, false, args);
    }

    @Override
    public <T> BlazeAggregateFunctionExpression<T> aggregateDistinctFunction(String name, Class<T> type, Expression<?>... args) {
        return new AggregationFunctionExpressionImpl<>(this, type, name, true, args);
    }

    @Override
    public <T> BlazeOrderedSetAggregateFunctionExpression<T> orderedSetAggregateFunction(String name, Class<T> type, Expression<?>... args) {
        return new OrderedSetAggregationFunction<>(this, type, name, false, args);
    }

    @Override
    public <T> BlazeOrderedSetAggregateFunctionExpression<T> orderedSetAggregateDistinctFunction(String name, Class<T> type, Expression<?>... args) {
        return new OrderedSetAggregationFunction<>(this, type, name, true, args);
    }

    @Override
    public <N extends Number> Expression<N> abs(Expression<N> expression) {
        return new FunctionExpressionImpl<>(this, (Class<N>) expression.getJavaType(), "ABS", expression);
    }

    @Override
    public Expression<Double> sqrt(Expression<? extends Number> expression) {
        return new FunctionExpressionImpl<>(this, Double.class, "SQRT", expression);
    }

    @Override
    public Expression<java.sql.Date> currentDate() {
        return new CurrentDateFunction(this);
    }

    @Override
    public Expression<java.sql.Timestamp> currentTimestamp() {
        return new CurrentTimestampFunction(this);
    }

    @Override
    public Expression<java.sql.Time> currentTime() {
        return new CurrentTimeFunction(this);
    }

    @Override
    public Expression<String> substring(Expression<String> value, Expression<Integer> start) {
        return new SubstringFunction(this, value, start);
    }

    @Override
    public Expression<String> substring(Expression<String> value, int start) {
        return new SubstringFunction(this, value, value(start));
    }

    @Override
    public Expression<String> substring(Expression<String> value, Expression<Integer> start, Expression<Integer> length) {
        return new SubstringFunction(this, value, start, length);
    }

    @Override
    public Expression<String> substring(Expression<String> value, int start, int length) {
        return new SubstringFunction(this, value, value(start), value(length));
    }

    @Override
    public Expression<String> trim(Expression<String> trimSource) {
        return new TrimFunction(this, trimSource);
    }

    @Override
    public Expression<String> trim(Trimspec trimspec, Expression<String> trimSource) {
        return new TrimFunction(this, trimspec, trimSource);
    }

    @Override
    public Expression<String> trim(Expression<Character> trimCharacter, Expression<String> trimSource) {
        return new TrimFunction(this, trimCharacter, trimSource);
    }

    @Override
    public Expression<String> trim(Trimspec trimspec, Expression<Character> trimCharacter, Expression<String> trimSource) {
        return new TrimFunction(this, trimspec, trimCharacter, trimSource);
    }

    @Override
    public Expression<String> trim(char trimCharacter, Expression<String> trimSource) {
        return new TrimFunction(this, value(trimCharacter), trimSource);
    }

    @Override
    public Expression<String> trim(Trimspec trimspec, char trimCharacter, Expression<String> trimSource) {
        return new TrimFunction(this, trimspec, value(trimCharacter), trimSource);
    }

    @Override
    public Expression<String> lower(Expression<String> value) {
        return new FunctionExpressionImpl<>(this, String.class, "LOWER", value);
    }

    @Override
    public Expression<String> upper(Expression<String> value) {
        return new FunctionExpressionImpl<>(this, String.class, "UPPER", value);
    }

    @Override
    public Expression<Integer> length(Expression<String> value) {
        return new FunctionExpressionImpl<>(this, Integer.class, "LENGTH", value);
    }

    @Override
    public Expression<Integer> locate(Expression<String> string, Expression<String> pattern) {
        return new LocateFunction(this, pattern, string);
    }

    @Override
    public Expression<Integer> locate(Expression<String> string, Expression<String> pattern, Expression<Integer> start) {
        return new LocateFunction(this, pattern, string, start);
    }

    @Override
    public Expression<Integer> locate(Expression<String> string, String pattern) {
        return new LocateFunction(this, value(pattern), string);
    }

    @Override
    public Expression<Integer> locate(Expression<String> string, String pattern, int start) {
        return new LocateFunction(this, value(pattern), string, value(start));
    }

    @Override
    public Expression<String> concat(Expression<String> string1, Expression<String> string2) {
        return new ConcatFunction(this, string1, string2);
    }

    @Override
    public Expression<String> concat(Expression<String> string1, String string2) {
        return new ConcatFunction(this, string1, value(string2));
    }

    @Override
    public Expression<String> concat(String string1, Expression<String> string2) {
        return new ConcatFunction(this, value(string1), string2);
    }

    /**********************
     * Arithmetic operations
     **********************/

    @Override
    public <N extends Number> Expression<N> neg(Expression<N> expression) {
        return new UnaryMinusExpression<N>(this, expression);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> sum(Expression<? extends N> expression1, Expression<? extends N> expression2) {
        checkExpression(expression1);
        checkExpression(expression2);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression1.getJavaType(), expression2.getJavaType());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.ADD, expression1, expression2);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> prod(Expression<? extends N> expression1, Expression<? extends N> expression2) {
        checkExpression(expression1);
        checkExpression(expression2);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression1.getJavaType(), expression2.getJavaType());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.MULTIPLY, expression1, expression2);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> diff(Expression<? extends N> expression1, Expression<? extends N> expression2) {
        checkExpression(expression1);
        checkExpression(expression2);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression1.getJavaType(), expression2.getJavaType());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.SUBTRACT, expression1, expression2);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> sum(Expression<? extends N> expression, N value) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression.getJavaType(), value.getClass());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.ADD, expression, value(value));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> prod(Expression<? extends N> expression, N value) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression.getJavaType(), value.getClass());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.MULTIPLY, expression, value(value));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> diff(Expression<? extends N> expression, N value) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression.getJavaType(), value.getClass());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.SUBTRACT, expression, value(value));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> sum(N value, Expression<? extends N> expression) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(value.getClass(), expression.getJavaType());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.ADD, value(value), expression);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> prod(N value, Expression<? extends N> expression) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(value.getClass(), expression.getJavaType());
        return (BinaryArithmeticExpression<N>) new BinaryArithmeticExpression(this, resultType, BinaryArithmeticExpression.Operation.MULTIPLY, value(value), expression);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <N extends Number> Expression<N> diff(N value, Expression<? extends N> expression) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(value.getClass(), expression.getJavaType());
        return new BinaryArithmeticExpression<N>(this, resultType, BinaryArithmeticExpression.Operation.SUBTRACT, value(value), expression);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<Number> quot(Expression<? extends Number> expression1, Expression<? extends Number> expression2) {
        checkExpression(expression1);
        checkExpression(expression2);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression1.getJavaType(), expression2.getJavaType(), true);
        return new BinaryArithmeticExpression<Number>(this, resultType, BinaryArithmeticExpression.Operation.DIVIDE, expression1, expression2);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<Number> quot(Expression<? extends Number> expression, Number value) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(expression.getJavaType(), value.getClass(), true);
        return new BinaryArithmeticExpression<Number>(this, resultType, BinaryArithmeticExpression.Operation.DIVIDE, expression, value(value));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<Number> quot(Number value, Expression<? extends Number> expression) {
        checkValue(value);
        checkExpression(expression);
        final Class resultType = BinaryArithmeticExpression.determineResultType(value.getClass(), expression.getJavaType(), true);
        return new BinaryArithmeticExpression<Number>(this, resultType, BinaryArithmeticExpression.Operation.DIVIDE, value(value), expression);
    }

    @Override
    public Expression<Integer> mod(Expression<Integer> expression1, Expression<Integer> expression2) {
        checkExpression(expression1);
        checkExpression(expression2);
        return new BinaryArithmeticExpression<Integer>(this, Integer.class, BinaryArithmeticExpression.Operation.MOD, expression1, expression2);
    }

    @Override
    public Expression<Integer> mod(Expression<Integer> expression, Integer value) {
        checkValue(value);
        checkExpression(expression);
        return new BinaryArithmeticExpression<Integer>(this, Integer.class, BinaryArithmeticExpression.Operation.MOD, expression, value(value));
    }

    @Override
    public Expression<Integer> mod(Integer value, Expression<Integer> expression) {
        checkValue(value);
        checkExpression(expression);
        return new BinaryArithmeticExpression<Integer>(this, Integer.class, BinaryArithmeticExpression.Operation.MOD, value(value), expression);
    }

    private void checkValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("null value not allowed!");
        }
    }

    private void checkExpression(Expression<?> expression) {
        if (expression == null) {
            throw new IllegalArgumentException("null expression not allowed!");
        }
    }

    /**********************
     * Casting functions
     **********************/

    @Override
    public Expression<Long> toLong(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asLong();
    }

    @Override
    public Expression<Integer> toInteger(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asInteger();
    }

    public Expression<Float> toFloat(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asFloat();
    }

    @Override
    public Expression<Double> toDouble(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asDouble();
    }

    @Override
    public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asBigDecimal();
    }

    @Override
    public Expression<BigInteger> toBigInteger(Expression<? extends Number> expression) {
        return ((AbstractExpression<? extends Number>) expression).asBigInteger();
    }

    @Override
    public Expression<String> toString(Expression<Character> characterExpression) {
        return ((AbstractExpression<Character>) characterExpression).asString();
    }

    /**********************
     * Subquery expressions
     **********************/

    @Override
    public Predicate exists(Subquery<?> subquery) {
        return new ExistsPredicate(this, subquery);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <Y> Expression<Y> all(Subquery<Y> subquery) {
        return new QuantifiableSubqueryExpression<Y>(this, (Class<Y>) subquery.getJavaType(), subquery, QuantifiableSubqueryExpression.Quantor.ALL);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <Y> Expression<Y> some(Subquery<Y> subquery) {
        return new QuantifiableSubqueryExpression<Y>(this, (Class<Y>) subquery.getJavaType(), subquery, QuantifiableSubqueryExpression.Quantor.SOME);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <Y> Expression<Y> any(Subquery<Y> subquery) {
        return new QuantifiableSubqueryExpression<Y>(this, (Class<Y>) subquery.getJavaType(), subquery, QuantifiableSubqueryExpression.Quantor.ANY);
    }

    /**********************
     * Case-When stuff
     **********************/

    @Override
    public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
        return coalesce((Class<Y>) null, exp1, exp2);
    }

    public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
        return new CoalesceFunction<Y>(this, type).value(exp1).value(exp2);
    }

    @Override
    public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Y value) {
        return coalesce((Class<Y>) null, exp1, value(value));
    }

    public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Y value) {
        return new CoalesceFunction<Y>(this, type).value(exp1).value(value);
    }

    @Override
    public <T> Coalesce<T> coalesce() {
        return coalesce((Class<T>) null);
    }

    public <T> Coalesce<T> coalesce(Class<T> type) {
        return new CoalesceFunction<T>(this, type);
    }

    @Override
    public <Y> Expression<Y> nullif(Expression<Y> exp1, Expression<?> exp2) {
        return nullif(null, exp1, exp2);
    }

    public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Expression<?> exp2) {
        return new NullifFunction<Y>(this, type, exp1, exp2);
    }

    @Override
    public <Y> Expression<Y> nullif(Expression<Y> exp1, Y value) {
        return nullif(null, exp1, value(value));
    }

    public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Y value) {
        return new NullifFunction<Y>(this, type, exp1, value(value));
    }

    @Override
    public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
        // JDK 9 complains without the cast
        return /*(SimpleCase<C, R>) */selectCase((Class<R>) null, expression);
    }

    public <C, R> SimpleCase<C, R> selectCase(Class<R> type, Expression<? extends C> expression) {
        return new SimpleCaseExpression<C, R>(this, type, expression);
    }

    @Override
    public <R> Case<R> selectCase() {
        return selectCase((Class<R>) null);
    }

    public <R> Case<R> selectCase(Class<R> type) {
        return new GeneralCaseExpression<R>(this, type);
    }

    /**********************
     * Collection functions
     **********************/

    @Override
    public <C extends Collection<?>> Expression<Integer> size(C c) {
        int size = c == null ? 0 : c.size();
        return new LiteralExpression<Integer>(this, Integer.class, size);
    }

    @Override
    public <C extends Map<?, ?>> Expression<Integer> mapSize(C c) {
        int size = c == null ? 0 : c.size();
        return new LiteralExpression<Integer>(this, Integer.class, size);
    }

    @Override
    public <C extends Collection<?>> Expression<Integer> size(Expression<C> exp) {
        if (LiteralExpression.class.isInstance(exp)) {
            return size(((LiteralExpression<C>) exp).getLiteral());
        } else if (PluralAttributePath.class.isInstance(exp)) {
            return new SizeFunction(this, (PluralAttributePath<C>) exp);
        }
        throw illegalCollection(exp);
    }

    @Override
    public <C extends Map<?, ?>> Expression<Integer> mapSize(Expression<C> exp) {
        if (LiteralExpression.class.isInstance(exp)) {
            return mapSize(((LiteralExpression<C>) exp).getLiteral());
        } else if (PluralAttributePath.class.isInstance(exp)) {
            return new SizeFunction(this, (PluralAttributePath<C>) exp);
        }
        throw illegalCollection(exp);
    }

    @Override
    public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
        return new LiteralExpression<Collection<V>>(this, map.values());
    }

    @Override
    public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
        return new LiteralExpression<Set<K>>(this, map.keySet());
    }

    @Override
    public <C extends Collection<?>> Predicate isEmpty(Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new IsEmptyPredicate(this, false, (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <C extends Map<?, ?>> Predicate isMapEmpty(Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new IsEmptyPredicate(this, false, (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new IsEmptyPredicate(this, true, (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <C extends Map<?, ?>> Predicate isMapNotEmpty(Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new IsEmptyPredicate(this, true, (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isMember(E e, Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new MemberOfPredicate<E, C>(this, false, value(e), (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(E e, Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new MemberOfPredicate<E, C>(this, true, value(e), (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isMember(Expression<E> elementExpression, Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new MemberOfPredicate<E, C>(this, false, elementExpression, (PluralAttributePath<C>) collectionExpression);
    }

    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> elementExpression, Expression<C> collectionExpression) {
        if (!(collectionExpression instanceof PluralAttributePath<?>)) {
            throw illegalCollection(collectionExpression);
        }
        return new MemberOfPredicate<E, C>(this, true, elementExpression, (PluralAttributePath<C>) collectionExpression);
    }

    private RuntimeException illegalCollection(Expression<?> expression) {
        return new IllegalArgumentException("Illegal expression type! Expected plural expression but got: " + expression.getClass().getName());
    }

    /****************************
     * Treat support for JPA 2.1
     ***************************/

    @Override
    public <X, T, V extends T> BlazeJoin<X, V> treat(Join<X, T> join, Class<V> type) {
        return ((AbstractJoin<X, T>) join).treatJoin(type);
    }

    @Override
    public <X, T, E extends T> BlazeCollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
        return ((CollectionAttributeJoin<X, T>) join).treatJoin(type);
    }

    @Override
    public <X, T, E extends T> BlazeSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
        return ((SetAttributeJoin<X, T>) join).treatJoin(type);
    }

    @Override
    public <X, T, E extends T> BlazeListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
        return ((ListAttributeJoin<X, T>) join).treatJoin(type);
    }

    @Override
    public <X, K, T, V extends T> BlazeMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
        return ((MapAttributeJoin<X, K, T>) join).treatJoin(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X, T extends X> BlazePath<T> treat(Path<X> path, Class<T> type) {
        if (path instanceof AbstractJoin<?, ?>) {
            return ((AbstractJoin<?, X>) path).treatJoin(type);
        }
        return ((AbstractPath<X>) path).treatAs(type);
    }

    @Override
    public <X, T extends X> BlazeRoot<T> treat(Root<X> root, Class<T> type) {
        return ((RootImpl<X>) root).treatAs(type);
    }

    @Override
    public <X, T, V extends T> BlazeJoin<X, V> treat(BlazeJoin<X, T> join, Class<V> type) {
        return treat((Join<X, T>) join, type);
    }

    @Override
    public <X, T, E extends T> BlazeCollectionJoin<X, E> treat(BlazeCollectionJoin<X, T> join, Class<E> type) {
        return treat((CollectionJoin<X, T>) join, type);
    }

    @Override
    public <X, T, E extends T> BlazeSetJoin<X, E> treat(BlazeSetJoin<X, T> join, Class<E> type) {
        return treat((SetJoin<X, T>) join, type);
    }

    @Override
    public <X, T, E extends T> BlazeListJoin<X, E> treat(BlazeListJoin<X, T> join, Class<E> type) {
        return treat((ListJoin<X, T>) join, type);
    }

    @Override
    public <X, K, T, V extends T> BlazeMapJoin<X, K, V> treat(BlazeMapJoin<X, K, T> join, Class<V> type) {
        return treat((MapJoin<X, K, T>) join, type);
    }

    @Override
    public <X, T extends X> BlazePath<T> treat(BlazePath<X> path, Class<T> type) {
        return treat((Path<X>) path, type);
    }

    @Override
    public <X, T extends X> BlazeRoot<T> treat(BlazeRoot<X> root, Class<T> type) {
        return treat((Root<X>) root, type);
    }

    @Override
    public BlazeWindow window() {
        return new BlazeWindowImpl();
    }

    @Override
    public BlazeWindowFunctionExpression<Integer> rowNumber() {
        return new WindowFunctionExpressionImpl<>(this, Integer.class, "ROW_NUMBER");
    }

    @Override
    public BlazeWindowFunctionExpression<Integer> rank(Expression<?> expression) {
        return new WindowFunctionExpressionImpl<>(this, Integer.class, "RANK", expression);
    }

    @Override
    public BlazeWindowFunctionExpression<Integer> denseRank(Expression<?> expression) {
        return new WindowFunctionExpressionImpl<>(this, Integer.class, "DENSE_RANK", expression);
    }

    @Override
    public BlazeWindowFunctionExpression<Double> percentRank(Expression<?> expression) {
        return new WindowFunctionExpressionImpl<>(this, Double.class, "PERCENT_RANK", expression);
    }

    @Override
    public BlazeWindowFunctionExpression<Double> cumeDist(Expression<?> expression) {
        return new WindowFunctionExpressionImpl<>(this, Double.class, "CUME_DIST", expression);
    }

    @Override
    public BlazeWindowFunctionExpression<Integer> ntile(Expression<?> expression) {
        return new WindowFunctionExpressionImpl<>(this, Integer.class, "NTILE", expression);
    }

    @Override
    public <X> BlazeWindowFunctionExpression<X> lead(Expression<X> expression) {
        //noinspection unchecked
        return new WindowFunctionExpressionImpl<>(this, (Class<X>) expression.getJavaType(), "LEAD", expression);
    }

    @Override
    public <X> BlazeWindowFunctionExpression<X> lag(Expression<X> expression) {
        //noinspection unchecked
        return new WindowFunctionExpressionImpl<>(this, (Class<X>) expression.getJavaType(), "LAG", expression);
    }

    @Override
    public <X> BlazeWindowFunctionExpression<X> firstValue(Expression<X> expression) {
        //noinspection unchecked
        return new WindowFunctionExpressionImpl<>(this, (Class<X>) expression.getJavaType(), "FIRST_VALUE", expression);
    }

    @Override
    public <X> BlazeWindowFunctionExpression<X> lastValue(Expression<X> expression) {
        //noinspection unchecked
        return new WindowFunctionExpressionImpl<>(this, (Class<X>) expression.getJavaType(), "LAST_VALUE", expression);
    }

    @Override
    public <X> BlazeWindowFunctionExpression<X> nthValue(Expression<X> expression, Expression<Integer> index) {
        //noinspection unchecked
        return new WindowFunctionExpressionImpl<>(this, (Class<X>) expression.getJavaType(), "NTH_VALUE", expression, index);
    }

    @Override
    public <X> BlazeOrderedSetAggregateFunctionExpression<X> percentileContWithinGroup(Expression<Double> fraction, Expression<X> group, boolean ascending, boolean nullsFirst) {
        //noinspection unchecked
        return new OrderedSetAggregationFunction<>(this, (Class<X>) group.getJavaType(), "PERCENTILE_CONT", false, fraction)
                .withinGroup(ascending ? asc(group, nullsFirst) : desc(group, nullsFirst));
    }

    @Override
    public <X> BlazeOrderedSetAggregateFunctionExpression<X> percentileDiscWithinGroup(Expression<Double> fraction, Expression<X> group, boolean ascending, boolean nullsFirst) {
        //noinspection unchecked
        return new OrderedSetAggregationFunction<>(this, (Class<X>) group.getJavaType(), "PERCENTILE_DISC", false, fraction)
                .withinGroup(ascending ? asc(group, nullsFirst) : desc(group, nullsFirst));
    }

    @Override
    public <X> BlazeOrderedSetAggregateFunctionExpression<X> modeWithinGroup(Expression<X> group) {
        //noinspection unchecked
        return new OrderedSetAggregationFunction<>(this, (Class<X>) group.getJavaType(), "MODE", false).withinGroup(asc(group));
    }

    @Override
    public BlazeOrderedSetAggregateFunctionExpression<String> listagg(Expression<String> expression, Expression<String> separator) {
        return new OrderedSetAggregationFunction<>(this, String.class, "LISTAGG", false, expression, separator);
    }

    @Override
    public BlazeOrderedSetAggregateFunctionExpression<String> listaggDistinct(Expression<String> expression, Expression<String> separator) {
        return new OrderedSetAggregationFunction<>(this, String.class, "LISTAGG", true, expression, separator);
    }
}
