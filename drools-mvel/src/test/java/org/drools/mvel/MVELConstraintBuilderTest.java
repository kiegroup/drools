/*
 * Copyright (c) 2024. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel;

import org.drools.compiler.lang.descr.LiteralRestrictionDescr;
import org.drools.core.base.ValueType;
import org.drools.core.base.field.DoubleFieldImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELConstraintBuilderTest {

    @Test
    public void test() {
        String expression = MVELConstraintBuilder.normalizeMVELLiteralExpression(ValueType.FLOAT_TYPE,
                new DoubleFieldImpl(0.0),
                "this.method(\"UV4014.01\") != 0",
                "this.method(\"UV4014.01\")",
                "!=",
                "0",
        false,
                new LiteralRestrictionDescr("!=", "0"));

        assertThat(expression).isEqualTo("this.method(\"UV4014.01\") != 0.0");
    }

}
