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

package org.citrusframework.yaks.camelk.actions;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.consol.citrus.AbstractTestActionBuilder;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.Integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test action creates new Camel-K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateIntegrationTestAction extends AbstractTestAction {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(CreateIntegrationTestAction.class);

    private final KubernetesClient client;
    private final String integrationName;
    private final String source;
    private final String dependencies;
    private final String traits;
    private final ObjectMapper mapper;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateIntegrationTestAction(Builder builder) {
        super("create-integration", builder);
        this.client = builder.client;
        this.integrationName = builder.integrationName;
        this.source = builder.source;
        this.dependencies = builder.dependencies;
        this.traits = builder.traits;
        this.mapper = builder.mapper;
    }

    @Override
    public void doExecute(TestContext context) {
        createIntegration(context);
    }

    private void createIntegration(TestContext context) {
        final Integration.Builder integrationBuilder = new Integration.Builder()
                .name(context.replaceDynamicContentInString(integrationName))
                .source(context.replaceDynamicContentInString(source));

        if (dependencies != null && !dependencies.isEmpty()) {
            integrationBuilder.dependencies(Arrays.asList(context.replaceDynamicContentInString(dependencies).split(",")));
        }

        if (traits != null && !traits.isEmpty()) {
            final Map<String, Integration.TraitConfig> traitConfigMap = new HashMap<>();
            for(String t : context.replaceDynamicContentInString(traits).split(",")){
                //traitName.key=value
                if(!validateTraitFormat(t)) {
                    throw new IllegalArgumentException("Trait" + t + "does not match format traitName.key=value");
                }
                final String[] trait = t.split("\\.",2);
                final String[] traitConfig = trait[1].split("=", 2);
                if(traitConfigMap.containsKey(trait[0])) {
                    traitConfigMap.get(trait[0]).add(traitConfig[0], traitConfig[1]);
                } else {
                    traitConfigMap.put(trait[0],  new Integration.TraitConfig(traitConfig[0], traitConfig[1]));
                }
            }
            integrationBuilder.traits(traitConfigMap);
        }

        final Integration i = integrationBuilder.build();

        final CustomResourceDefinitionContext crdContext = getIntegrationCRD();

        try {
            Map<String, Object> result = client.customResource(crdContext).createOrReplace(CamelKSettings.getNamespace(), mapper.writeValueAsString(i));
            if (result.get("message") != null) {
                throw new CitrusRuntimeException(result.get("message").toString());
            }
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to create Camel-K integration via JSON object", e);
        }

        LOG.info(String.format("Successfully created Camel-K integration '%s'", i.getMetadata().getName()));
    }

    private CustomResourceDefinitionContext getIntegrationCRD() {
        return new CustomResourceDefinitionContext.Builder()
                .withName(Integration.CRD_INTEGRATION_NAME)
                .withGroup(Integration.CRD_GROUP)
                .withVersion(Integration.CRD_VERSION)
                .withPlural("integrations")
                .withScope("Namespaced")
                .build();
    }

    private boolean validateTraitFormat(String trait) {
        String patternString = "[A-Za-z-0-9]+\\.[A-Za-z-0-9]+=[A-Za-z-0-9]+";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(trait);
        return matcher.matches();
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractTestActionBuilder<CreateIntegrationTestAction, Builder> {

        private KubernetesClient client = new DefaultKubernetesClient();
        private String integrationName;
        private String source;
        private String dependencies;
        private String traits;
        private ObjectMapper mapper = new ObjectMapper();

        /**
         * Fluent API action building entry method used in Java DSL.
         * @return
         */
        public static Builder createIntegration() {
            return new Builder();
        }

        public Builder client(KubernetesClient client) {
            this.client = client;
            return this;
        }

        public Builder integrationName(String integrationName) {
            this.integrationName = integrationName;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder dependencies(String dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder traits(String traits) {
            this.traits = traits;
            return this;
        }

        public Builder mapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        @Override
        public CreateIntegrationTestAction build() {
            return new CreateIntegrationTestAction(this);
        }
    }
}
