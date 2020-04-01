/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.report;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local store for collected test results.
 */
class TestResults {
    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(TestResults.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TestSummary summary = new TestSummary();

    private final List<TestResult> tests = new ArrayList<>();

    public List<TestResult> getTests() {
        return tests;
    }

    public TestSummary getSummary() {
        return summary;
    }

    @JsonIgnore
    public void addTestResult(TestResult result) {
        this.tests.add(result);
    }

    @JsonIgnore
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to create test result Json report", e);
        }

        return "";
    }
}
