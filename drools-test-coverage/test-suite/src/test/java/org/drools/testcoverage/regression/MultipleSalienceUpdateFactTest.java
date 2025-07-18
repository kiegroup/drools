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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.drools.testcoverage.common.KieSessionTest;
import org.drools.testcoverage.common.model.ListHolder;
import org.drools.testcoverage.common.model.Person;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieSessionTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.command.Command;
import org.kie.api.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.testcoverage.common.util.KieUtil.getCommands;

/**
 * Test to verify that BRMS-580 is fixed. NPE when trying to update fact with
 * rules with different saliences.
 */
public class MultipleSalienceUpdateFactTest extends KieSessionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleSalienceUpdateFactTest.class);

    private static final String DRL_FILE = "BRMS-580.drl";

    public static Stream<Arguments> parameters() {
        return TestParametersUtil2.getKieBaseAndStatefulKieSessionConfigurations().stream();
    }
    
    @ParameterizedTest(name = "{1}" + " (from " + "{0}" + ")")
	@MethodSource("parameters")
    public void test(KieBaseTestConfiguration kieBaseTestConfiguration,
            KieSessionTestConfiguration kieSessionTestConfiguration) {
    	createKieSession(kieBaseTestConfiguration, kieSessionTestConfiguration);
        session.setGlobal("LOGGER", LOGGER);
        List<Command<?>> commands = new ArrayList<Command<?>>();

        Person person = new Person("PAUL");

        ListHolder listHolder = new ListHolder();
        List<String> list = Arrays.asList("eins", "zwei", "drei");
        listHolder.setList(list);

        commands.add(getCommands().newInsert(person));
        commands.add(getCommands().newInsert(listHolder));
        commands.add(getCommands().newFireAllRules());

        session.execute(getCommands().newBatchExecution(commands, null));

        assertThat(firedRules.isRuleFired("PERSON_PAUL")).isTrue();
        assertThat(firedRules.isRuleFired("PERSON_PETER")).isTrue();
    }

    @Override
    protected Resource[] createResources() {
        return KieUtil.createResources(DRL_FILE, MultipleSalienceUpdateFactTest.class);
    }
}
