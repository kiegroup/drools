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
package org.drools.reliability.test.smoke.proto;

import org.drools.reliability.test.BeforeAllMethodExtension;
import org.drools.reliability.test.smoke.BaseSmokeTest;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.condition.OS.WINDOWS;

@DisabledOnOs(WINDOWS)
@EnabledIf("isRemoteProtoInfinispan")
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(BeforeAllMethodExtension.class)
class ProtoSmokeTest extends BaseSmokeTest {

}
