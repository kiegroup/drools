/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.testcoverage.regression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestConstants;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;

public class FromGenericCollectionTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseConfigurations().stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testGenerics(KieBaseTestConfiguration kieBaseTestConfiguration) {

        final String drl = "package " + TestConstants.PACKAGE_REGRESSION + "\n"
                + " import java.util.Map.Entry\n"
                + " import java.util.List\n"
                + " import " + GenericHolder.class.getCanonicalName() + "\n"
                + " rule checkCrazyMap\n"
                + " when\n"
                + "        GenericHolder( $map : crazyMap )\n"
                + "        $entry : Entry( $list : value ) from $map.entrySet\n"
                + "        $string : String ( ) from $list\n"
                + " then\n"
                + "        insert(new Boolean(true));\n"
                + " end\n";

        final KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl(TestConstants.PACKAGE_REGRESSION,
                                                                           kieBaseTestConfiguration, drl);
        final KieSession ksession = kieBase.newKieSession();
        try {
            final Map<String, List<String>> crazyMap = new HashMap<String, List<String>>();
            crazyMap.put("foo", List.of("bar"));
            final GenericHolder gh = new GenericHolder();
            gh.setCrazyMap(crazyMap);

            ksession.insert(gh);
            ksession.fireAllRules();

            assertThat(ksession.getObjects(new ClassObjectFilter(Boolean.class)).size()).isEqualTo(1);
        } finally {
            ksession.dispose();
        }
    }

    public static class GenericHolder {
        private Map<String, List<String>> crazyMap;

        public Map<String, List<String>> getCrazyMap() {
            return crazyMap;
        }

        public void setCrazyMap(final Map<String, List<String>> crazyMap) {
            this.crazyMap = crazyMap;
        }
    }

}
