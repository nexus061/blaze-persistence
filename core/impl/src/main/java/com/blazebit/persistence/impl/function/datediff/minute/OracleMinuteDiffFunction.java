/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.impl.function.datediff.minute;

import com.blazebit.persistence.spi.FunctionRenderContext;
import com.blazebit.persistence.spi.TemplateRenderer;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class OracleMinuteDiffFunction extends MinuteDiffFunction {

    private final TemplateRenderer bothParamRenderer;
    private final TemplateRenderer firstParamRenderer;
    private final TemplateRenderer secondParamRenderer;

    public OracleMinuteDiffFunction() {
        super("(extract(day from cast(?2 as timestamp) - cast(?1 as timestamp)) * " + (24 * 60) + " + extract(hour from cast(?2 as timestamp) - cast(?1 as timestamp)) * 60 + extract(minute from cast(?2 as timestamp) - cast(?1 as timestamp)))");
        // TODO: When parameters are part of more complex expressions this won't work, but we don't care about that for now
        this.bothParamRenderer = new TemplateRenderer("(select extract(day from t2 - t1) * " + (24 * 60) + " + extract(hour from t2 - t1) * 60 + extract(minute from t2 - t1) from (select cast(?1 as timestamp) as t1, cast(?2 as timestamp) as t2 from dual))");
        this.firstParamRenderer = new TemplateRenderer("(select extract(day from cast(?2 as timestamp) - t1) * " + (24 * 60) + " + extract(hour from cast(?2 as timestamp) - t1) * 60 + extract(minute from cast(?2 as timestamp) - t1) from (select cast(?1 as timestamp) as t1 from dual))");
        this.secondParamRenderer = new TemplateRenderer("(select extract(day from t2 - cast(?1 as timestamp)) * " + (24 * 60) + " + extract(hour from t2 - cast(?1 as timestamp)) * 60 + extract(minute from t2 - cast(?1 as timestamp)) from (select cast(?2 as timestamp) as t2 from dual))");
    }

    @Override
    protected void renderDiff(FunctionRenderContext context) {
        boolean firstContains = context.getArgument(0).contains("?");
        boolean secondContains = context.getArgument(1).contains("?");
        if (firstContains && context.getArgument(0).length() != 1
                || secondContains && context.getArgument(1).length() != 1) {
            throw new IllegalArgumentException("Only simple parameters or expressions are allowed because of a needed reordering in SQL which is otherwise not possible! Expressions ['" + context.getArgument(0) + "', '" + context.getArgument(1) + "'] do not comply!");
        }
        if (firstContains) {
            if (secondContains) {
                bothParamRenderer.start(context).addArgument(0).addArgument(1).build();
            } else {
                firstParamRenderer.start(context).addArgument(0).addArgument(1).build();
            }
        } else if (secondContains) {
            secondParamRenderer.start(context).addArgument(0).addArgument(1).build();
        } else {
            // Reuse the expression multiple times
            renderer.start(context).addArgument(0).addArgument(1).build();
        }
    }
}
