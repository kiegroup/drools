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
package org.kie.dmn.feel.marshaller;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.feel.lang.types.BuiltInType;
import org.kie.dmn.feel.lang.types.impl.ComparablePeriod;

import static org.assertj.core.api.Assertions.assertThat;

public class FEELStringMarshallerUnmarshallTest {

    private static Collection<Object[]> data() {
        final Object[][] cases = new Object[][] {
                // numbers
                { BuiltInType.NUMBER, "2", BigDecimal.valueOf( 2 ) },
                { BuiltInType.NUMBER, "2.0", BigDecimal.valueOf( 2.0 ) },
                { BuiltInType.NUMBER, "0.2", BigDecimal.valueOf( .2 ) },
                { BuiltInType.NUMBER, "0.2", BigDecimal.valueOf( 0.2 ) },
                { BuiltInType.NUMBER, "-0.2", BigDecimal.valueOf( -0.2 ) },
                // strings
                { BuiltInType.STRING, "foo", "foo" },
                { BuiltInType.STRING, "", "" },
                // booleans
                { BuiltInType.BOOLEAN, "true", true },
                { BuiltInType.BOOLEAN, "false", false },
                // dates
                { BuiltInType.DATE, "2017-07-01", LocalDate.of( 2017, 07, 01 ) },
                // time
                { BuiltInType.TIME, "14:32:55", LocalTime.of( 14, 32, 55 ) },
                { BuiltInType.TIME, "14:32:55.125-05:00", OffsetTime.of( 14, 32, 55, 125000000, ZoneOffset.ofHours( -5 ) ) },
                { BuiltInType.TIME, "14:32:55.125Z", OffsetTime.of( 14, 32, 55, 125000000, ZoneOffset.UTC ) },
                // date and time
                { BuiltInType.DATE_TIME, "2017-06-30T10:49:11", LocalDateTime.of( 2017, 06, 30, 10, 49, 11 ) },
                { BuiltInType.DATE_TIME, "2017-06-30T10:49:11.650", LocalDateTime.of( 2017, 06, 30, 10, 49, 11, 650000000 ) },
                { BuiltInType.DATE_TIME, "2017-06-30T10:49:11.650+03:00", ZonedDateTime.of( 2017, 06, 30, 10, 49, 11, 650000000, ZoneOffset.ofHours( 3 ) ) },
                // days and time duration
                { BuiltInType.DURATION, "P5DT4H23M55S", Duration.ofDays( 5 ).plusHours( 4 ).plusMinutes( 23 ).plusSeconds( 55 ) },
                { BuiltInType.DURATION, "-P5DT4H23M55S", Duration.ofDays( -5 ).minusHours( 4 ).minusMinutes( 23 ).minusSeconds( 55 ) },
                { BuiltInType.DURATION, "P23D", Duration.ofDays( 23 ) },
                { BuiltInType.DURATION, "-P23D", Duration.ofDays( -23 ) },
                { BuiltInType.DURATION, "PT23H", Duration.ofHours( 23 ) },
                { BuiltInType.DURATION, "-PT23H", Duration.ofHours( -23 ) },
                { BuiltInType.DURATION, "PT23M", Duration.ofMinutes( 23 ) },
                { BuiltInType.DURATION, "-PT23M", Duration.ofMinutes( -23 ) },
                { BuiltInType.DURATION, "PT23S", Duration.ofSeconds( 23 ) },
                { BuiltInType.DURATION, "-PT23S", Duration.ofSeconds( -23 ) },
                { BuiltInType.DURATION, "PT0S", Duration.ofDays( 0 ) },
                { BuiltInType.DURATION, "P5DT4H", Duration.ofHours( 124 )},
                { BuiltInType.DURATION, "P737DT20H8M3S", Duration.ofSeconds( 63749283 )},
                // months and years duration
                { BuiltInType.DURATION, "P4Y5M", ComparablePeriod.of( 4, 5, 0 ) },
                { BuiltInType.DURATION, "P6Y1M", ComparablePeriod.of( 6, 1, 0 ) },
                { BuiltInType.DURATION, "-P6Y1M", ComparablePeriod.of( -6, -1, 0 ) },
                { BuiltInType.DURATION, "P0M", ComparablePeriod.of( 0, 0, 0 ) },
                // null
                { BuiltInType.UNKNOWN, "null", null },
                // Any based best efforts
                { BuiltInType.UNKNOWN, "John", "John" },
                { BuiltInType.UNKNOWN, "123", "123" },
        };
        return Arrays.asList( cases );
    }
    public Type feelType;
    public String value;
    public Object result;

    @MethodSource("data")
    @ParameterizedTest(name = "{index}: {0} ({1}) = {2}")
    public void expression(Type feelType, String value, Object result) {
        initFEELStringMarshallerUnmarshallTest(feelType, value, result);
        assertResult( feelType, value, result );
    }

    protected void assertResult(Type feelType, String value, Object result ) {
        if( result == null ) {
        	assertThat(FEELStringMarshaller.INSTANCE.unmarshall( feelType, value )).as("Unmarshalling: '" + value + "'").isNull();
        } else {
        	assertThat(FEELStringMarshaller.INSTANCE.unmarshall( feelType, value )).as("Unmarshalling: '" + value + "'").isEqualTo(result);
        }
    }

    public void initFEELStringMarshallerUnmarshallTest(Type feelType, String value, Object result) {
        this.feelType = feelType;
        this.value = value;
        this.result = result;
    }
}
