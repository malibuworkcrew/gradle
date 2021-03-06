/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.platform.base.internal.registry
import org.gradle.api.Task
import org.gradle.api.initialization.Settings
import org.gradle.model.InvalidModelRuleDeclarationException
import org.gradle.model.collection.CollectionBuilder
import org.gradle.model.internal.inspect.DefaultMethodRuleDefinition
import org.gradle.model.internal.inspect.MethodRuleDefinition
import org.gradle.model.internal.inspect.RuleSourceDependencies
import org.gradle.platform.base.BinarySpec
import org.gradle.platform.base.BinaryTask
import org.gradle.platform.base.InvalidComponentModelException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Unroll

import java.lang.annotation.Annotation
import java.lang.reflect.Method

class BinaryTaskRuleDefinitionHandlerTest extends AbstractAnnotationRuleDefinitionHandlerTest {

    def ruleDependencies = Mock(RuleSourceDependencies)

    BinaryTaskRuleDefinitionHandler ruleHandler = new BinaryTaskRuleDefinitionHandler()

    @Override
    Class<? extends Annotation> getAnnotation() {
        return BinaryTask
    }

    def ruleDefinitionForMethod(String methodName) {
        for (Method candidate : Rules.class.getDeclaredMethods()) {
            if (candidate.getName().equals(methodName)) {
                return DefaultMethodRuleDefinition.create(Rules.class, candidate)
            }
        }
        throw new IllegalArgumentException("Not a test method name")
    }

    @Unroll
    def "decent error message for #descr"() {
        def ruleMethod = ruleDefinitionForMethod(methodName)
        def ruleDescription = getStringDescription(ruleMethod)

        when:
        ruleHandler.register(ruleMethod, modelRegistry, ruleDependencies)

        then:
        def ex = thrown(InvalidModelRuleDeclarationException)
        ex.message == "${ruleDescription} is not a valid BinaryTask model rule method."
        ex.cause instanceof InvalidComponentModelException
        ex.cause.message == expectedMessage

        where:
        methodName                    | expectedMessage                                                                                                      | descr
        "returnValue"                 | "BinaryTask method must not have a return value."                                                                    | "non void method"
        "noParams"                    | "BinaryTask method must have a parameter of type '${CollectionBuilder.name}'."                                       | "no CollectionBuilder subject"
        "wrongSubject"                | "BinaryTask method first parameter must be of type '${CollectionBuilder.name}'."                                       | "wrong rule subject type"
//        "multipileComponentSpecs"   | "BinaryTask method must have one parameter extending ComponentSpec. Found multiple parameter extending ComponentSpec." | "additional component spec parameter"
//        "noComponentSpec"           | "BinaryTask method must have one parameter extending ComponentSpec. Found no parameter extending ComponentSpec."       | "no component spec parameter"
//        "missmatchingComponentSpec" | "BinaryTask method parameter of type SomeOtherLibrary does not support binaries of type SomeBinarySpec."               | "non matching CompnentSpec type"
//        "rawCollectionBuilder"      | "Parameter of type 'Collection' must declare a type parameter extending 'BinarySpec'."                                                                                  | "non typed CollectionBuilder parameter"
    }

    def getStringDescription(MethodRuleDefinition ruleDefinition) {
        def builder = new StringBuilder()
        ruleDefinition.descriptor.describeTo(builder)
        builder.toString()
    }

    def aProjectPlugin() {
        ruleDependencies = ProjectBuilder.builder().build()
        _ * pluginApplication.target >> ruleDependencies
    }

    def aSettingsPlugin(def plugin) {
        Settings settings = Mock(Settings)
        _ * pluginApplication.target >> settings
        _ * pluginApplication.plugin >> plugin
        ruleHandler = new ComponentTypeRuleDefinitionHandler(instantiator)
    }

    interface SomeBinary extends BinarySpec {}

    static class Rules {

        @BinaryTask
        static void rawCollectionBuilder(CollectionBuilder tasks, SomeBinary binary) {
        }

        @BinaryTask
        static void noParams() {
        }

        @BinaryTask
        static void wrongSubject(binary) {
        }

        @BinaryTask
        static void noBinaryParameter(CollectionBuilder<Task> builder) {
        }

        @BinaryTask
        static String returnValue(CollectionBuilder<Task> builder, SomeBinary binary) {
        }

        @BinaryTask
        static void validTypeRule(CollectionBuilder<Task> tasks, SomeBinary binary) {
            tasks.create("create${binary.getName()}")
        }

    }
}