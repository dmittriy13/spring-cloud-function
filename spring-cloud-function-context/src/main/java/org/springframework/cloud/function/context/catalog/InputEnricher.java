/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.function.context.catalog;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 *
 * @author Oleg Zhurakousky
 *
 * @since 3.1.3
 *
 */
public class InputEnricher implements Function<Object, Object> {

	private final Map<String, Map<String, String>> headerExpressions;

	private final SpelExpressionParser spelParser = new SpelExpressionParser();

	private final StandardEvaluationContext evalContext = new StandardEvaluationContext();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public InputEnricher(Map headerExpressions, @Nullable BeanResolver beanResolver) {
		Assert.notEmpty(headerExpressions, "'headerExpressions' must not be null or empty");
		this.headerExpressions = headerExpressions;
		this.evalContext.addPropertyAccessor(new MapAccessor());
		if (beanResolver != null) {
			this.evalContext.setBeanResolver(beanResolver);
		}
	}

	@Override
	public Object apply(Object input) {
		if (input instanceof Message<?>) {
			MessageBuilder<?> messageBuilder = MessageBuilder.fromMessage((Message<?>) input);
			Map<String, String> mappings = this.headerExpressions.get("0");
			for (Entry<String, String> keyValueExpressionEntry : mappings.entrySet()) {
				Expression expression = this.spelParser.parseExpression(keyValueExpressionEntry.getValue());
				Object value = expression.getValue(this.evalContext, input, Object.class);
				messageBuilder.setHeader(keyValueExpressionEntry.getKey(), value);
			}
			input = messageBuilder.build();
		}

		return input;
	}

}
