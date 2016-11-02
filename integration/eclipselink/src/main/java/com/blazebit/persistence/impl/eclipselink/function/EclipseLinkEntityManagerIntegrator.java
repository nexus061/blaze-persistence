/*
 * Copyright 2014 - 2016 Blazebit.
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

package com.blazebit.persistence.impl.eclipselink.function;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.blazebit.persistence.impl.eclipselink.EclipseLinkJpaProvider;
import com.blazebit.persistence.spi.EntityManagerFactoryIntegrator;
import com.blazebit.persistence.spi.JpaProvider;
import com.blazebit.persistence.spi.JpaProviderFactory;
import com.blazebit.persistence.spi.JpqlFunction;
import com.blazebit.persistence.spi.JpqlFunctionGroup;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.jpa.JpaHelper;
import org.eclipse.persistence.platform.database.DatabasePlatform;

import com.blazebit.apt.service.ServiceProvider;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
@ServiceProvider(EntityManagerFactoryIntegrator.class)
public class EclipseLinkEntityManagerIntegrator implements EntityManagerFactoryIntegrator {
    
    private static final Logger LOG = Logger.getLogger(EntityManagerFactoryIntegrator.class.getName());
    
    /**
     * EclipseLink uses integer values for something they call selectors.
     * Apparently every operator needs a unique selector value. We choose 100.000 as
     * the base value from which we will increment further for all functions.
     */
    private int functionSelectorCounter = 100000;

    @Override
    public String getDbms(EntityManagerFactory entityManagerFactory) {
        return null;
    }

    @Override
    public JpaProviderFactory getJpaProviderFactory(EntityManagerFactory entityManagerFactory) {
        boolean eclipseLink24;
        String version;
        try {
            Class<?> versionClass = Class.forName("org.eclipse.persistence.Version");
            version = (String) versionClass.getMethod("getVersion").invoke(null);
            String[] versionParts = version.split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);

            eclipseLink24 = major > 2 || (major == 2 && minor >= 4);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported EclipseLink version", ex);
        }

        if (!eclipseLink24) {
            throw new IllegalArgumentException("Unsupported EclipseLink version " + version + "!");
        }
        return new JpaProviderFactory() {
            @Override
            public JpaProvider createJpaProvider(EntityManager em) {
                return new EclipseLinkJpaProvider(em);
            }
        };
    }

    @Override
    public Set<String> getRegisteredFunctions(EntityManagerFactory entityManagerFactory) {
        DatabasePlatform platform = JpaHelper.getDatabaseSession(entityManagerFactory).getPlatform();
        @SuppressWarnings("unchecked")
        Map<Integer, ExpressionOperator> platformOperators = platform.getPlatformOperators();
        Set<String> functions = new HashSet<String>(platformOperators.size());
        
        for (ExpressionOperator op : platformOperators.values()) {
            String name = (String) ExpressionOperator.getPlatformOperatorNames().get(op.getSelector());
            
            if (name != null) {
                functions.add(name.toLowerCase());
            }
        }
        
        return functions;
    }

    @Override
    public EntityManagerFactory registerFunctions(EntityManagerFactory entityManagerFactory, Map<String, JpqlFunctionGroup> dbmsFunctions) {
        DatabasePlatform platform = JpaHelper.getDatabaseSession(entityManagerFactory).getPlatform();
        @SuppressWarnings("unchecked")
        Map<Integer, ExpressionOperator> platformOperators = platform.getPlatformOperators();
        String dbms;
        
        if (platform.isMySQL()) {
            dbms = "mysql";
        } else if (platform.isOracle()) {
            dbms = "oracle";
        } else if (platform.isSQLServer()) {
            dbms = "microsoft";
        } else if (platform.isSybase()) {
            dbms = "sybase";
        } else {
            dbms = null;
        }
        
        for (Map.Entry<String, JpqlFunctionGroup> functionEntry : dbmsFunctions.entrySet()) {
            String functionName = functionEntry.getKey();
            JpqlFunctionGroup dbmsFunctionMap = functionEntry.getValue();
            JpqlFunction function = dbmsFunctionMap.get(dbms);
            
            if (function == null) {
                function = dbmsFunctionMap.get(null);
            }
            if (function == null) {
                LOG.warning("Could not register the function '" + functionName + "' because there is neither an implementation for the dbms '" + dbms + "' nor a default implementation!");
            } else {
                addFunction(platformOperators, functionName, function);
            }
        }
        
        return entityManagerFactory;
    }
    
    private void addFunction(Map<Integer, ExpressionOperator> platformOperators, String name, JpqlFunction function) {
        ExpressionOperator operator = createOperator(name, function);
        ExpressionOperator.registerOperator(operator.getSelector(), name);
        ExpressionOperator.addOperator(operator);
        platformOperators.put(Integer.valueOf(operator.getSelector()), operator);
    }
    
    private ExpressionOperator createOperator(String name, JpqlFunction function) {
        ExpressionOperator operator = new JpqlFunctionExpressionOperator(function);
        operator.setType(ExpressionOperator.FunctionOperator);
        operator.setSelector(functionSelectorCounter++);
        operator.setName(name.toUpperCase());
//        Vector v = new Vector();
//        v.add("TRIM(LEADING ");
//        v.add(" FROM ");
//        v.add(")");
//        operator.printsAs(v);
//        operator.bePrefix();
//        int[] argumentIndices = new int[2];
//        argumentIndices[0] = 1;
//        argumentIndices[1] = 0;
//        operator.setArgumentIndices(argumentIndices);
        operator.setNodeClass(ClassConstants.FunctionExpression_Class);
        operator.setIsBindingSupported(false);
        return operator;
    }
}
