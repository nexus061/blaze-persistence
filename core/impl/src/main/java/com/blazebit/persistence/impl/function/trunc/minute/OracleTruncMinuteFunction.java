/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.impl.function.trunc.minute;

/**
 * @author Jan-Willem Gmelig Meyling
 * @since 1.4.0
 */
public class OracleTruncMinuteFunction extends TruncMinuteFunction {

    public OracleTruncMinuteFunction() {
        super("TRUNC(?1, 'MI')");
    }

}
