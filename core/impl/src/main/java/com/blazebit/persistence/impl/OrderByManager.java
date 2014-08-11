/*
 * Copyright 2014 Blazebit.
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

package com.blazebit.persistence.impl;

import com.blazebit.persistence.impl.expression.Expression;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Moritz Becker
 * @since 1.0
 */
public class OrderByManager extends AbstractManager {
    private final List<OrderByInfo> orderByInfos = new ArrayList<OrderByInfo>();
    
    OrderByManager(QueryGenerator queryGenerator, ParameterManager parameterManager) {
        super(queryGenerator, parameterManager);
    }
    
    boolean hasNonIdOrderBys(String idName) {
        if (orderByInfos.size() < 1) {
            return false;
        }
        
        for (OrderByInfo orderBy : orderByInfos) {
            if (!idName.equals(orderBy.getExpression().toString())) {
                return true;
            }
        }
        
        return false;
    }
    
    void orderBy(Expression expr, boolean ascending, boolean nullFirst){
        orderByInfos.add(new OrderByInfo(expr, ascending, nullFirst));
        registerParameterExpressions(expr);
    }
    
    void acceptVisitor(Expression.Visitor v){
        for (OrderByInfo orderBy : orderByInfos) {
            orderBy.getExpression().accept(v);
        }
    }
    
    void applyTransformer(ExpressionTransformer transformer){
        for (OrderByInfo orderBy : orderByInfos) {
            orderBy.setExpression(transformer.transform(orderBy.getExpression()));
        }
    }
    
    void buildSelectClauses(StringBuilder sb) {
        if (orderByInfos.isEmpty()) {
            return ;
        }
        
        queryGenerator.setQueryBuffer(sb);
        Iterator<OrderByInfo> iter = orderByInfos.iterator();
        OrderByInfo orderByInfo = iter.next();
        orderByInfo.getExpression().accept(queryGenerator);
        while (iter.hasNext()) {
            sb.append(", ");
            orderByInfo = iter.next();
            orderByInfo.getExpression().accept(queryGenerator);
        }
    }
    
    void buildOrderBy(StringBuilder sb) {
        if (orderByInfos.isEmpty()) {
            return ;
        }
        queryGenerator.setReplaceSelectAliases(true);
        queryGenerator.setQueryBuffer(sb);
        sb.append(" ORDER BY ");
        Iterator<OrderByInfo> iter = orderByInfos.iterator();
        applyOrderBy(sb, iter.next());
        while (iter.hasNext()) {
            sb.append(", ");
            applyOrderBy(sb, iter.next());
        }
        queryGenerator.setReplaceSelectAliases(false);
    }

    private void applyOrderBy(StringBuilder sb, OrderByInfo orderBy) {
        orderBy.getExpression().accept(queryGenerator);
        if (!orderBy.ascending) {
            sb.append(" DESC");
        } else {
            sb.append(" ASC");
        }
        if (orderBy.nullFirst) {
            sb.append(" NULLS FIRST");
        } else {
            sb.append(" NULLS LAST");
        }
    }
    
    private static class OrderByInfo extends NodeInfo {

        private boolean ascending;
        private boolean nullFirst;

        public OrderByInfo(Expression expression, boolean ascending, boolean nullFirst) {
            super(expression);
            this.ascending = ascending;
            this.nullFirst = nullFirst;
        }
    }
}
