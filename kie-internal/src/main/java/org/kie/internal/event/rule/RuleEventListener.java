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
package org.kie.internal.event.rule;

import java.util.EventListener;

import org.kie.api.runtime.rule.Match;

public interface RuleEventListener extends EventListener {

    default void onBeforeMatchFire(Match match) {}
    default void onAfterMatchFire(Match match) {}

    default void onDeleteMatch(Match match) {}
    default void onUpdateMatch(Match match) {}

//    to add later
//    void onAllFiring(Rule rule);
//    void onAllFired(Rule rule);
}
