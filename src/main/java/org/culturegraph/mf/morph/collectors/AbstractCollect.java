/*
 *  Copyright 2013, 2014 Deutsche Nationalbibliothek
 *
 *  Licensed under the Apache License, Version 2.0 the "License";
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.culturegraph.mf.morph.collectors;

import org.culturegraph.mf.morph.AbstractNamedValuePipe;
import org.culturegraph.mf.morph.Metamorph;
import org.culturegraph.mf.morph.NamedValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Common basis for {@link Entity}, {@link Combine} etc.
 *
 * @author Markus Michael Geipel
 * @author Christoph Böhme
 *
 */
public abstract class AbstractCollect extends AbstractNamedValuePipe
		implements Collect {

	private int oldRecord;
	private int oldEntity;
	private boolean resetAfterEmit;
	private boolean sameEntity;
	private String name;
	private String value;
	private final Metamorph metamorph;
	private boolean waitForFlush;
	private boolean conditionMet;
	private boolean				includeSubEntities;
	private int					currentHierarchicalEntity	= 0;
	private int					oldHierarchicalEntity		= 0;
	private Map<String, List<String>> hierarchicalEntityEmitBuffer;

	private NamedValueSource conditionSource;

	public AbstractCollect(final Metamorph metamorph) {
		super();
		this.metamorph = metamorph;
	}

	protected final Metamorph getMetamorph() {
		return metamorph;
	}

	public final void setIncludeSubEntities(final boolean includeSubEntitiesArg) {

		includeSubEntities = includeSubEntitiesArg;

		if (includeSubEntities) {

			hierarchicalEntityEmitBuffer = new LinkedHashMap<String, List<String>>();
		}
	}

	protected final boolean getIncludeSubEntities() {

		return includeSubEntities;
	}

	protected final Map<String, List<String>> getHierarchicalEntityEmitBuffer() {

		return hierarchicalEntityEmitBuffer;
	}

	protected final int getRecordCount() {
		return oldRecord;
	}

	protected final int getEntityCount() {
		return oldEntity;
	}

	protected final boolean isConditionMet() {
		return conditionMet;
	}

	protected final void setConditionMet(final boolean conditionMet) {
		this.conditionMet = conditionMet;
	}

	protected final void resetCondition() {
		setConditionMet(conditionSource == null);
	}

	@Override
	public final void setWaitForFlush(final boolean waitForFlush) {
		this.waitForFlush = waitForFlush;
		// metamorph.addEntityEndListener(this, flushEntity);
	}

	@Override
	public final void setSameEntity(final boolean sameEntity) {
		this.sameEntity = sameEntity;
	}

	public final boolean getReset() {
		return resetAfterEmit;
	}

	@Override
	public final void setReset(final boolean reset) {
		this.resetAfterEmit = reset;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final void setName(final String name) {
		this.name = name;
	}

	@Override
	public final void setConditionSource(final NamedValueSource source) {
		conditionSource = source;
		conditionSource.setNamedValueReceiver(this);
		resetCondition();
	}

	public final String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public final void setValue(final String value) {
		this.value = value;
	}

	protected final void updateCounts(final int currentRecord,
			final int currentEntity) {
		if (!isSameRecord(currentRecord)) {
			resetCondition();
			clear();
			oldRecord = currentRecord;
		}
		if (resetNeedFor(currentEntity)) {
			resetCondition();
			clear();
		}
		oldEntity = currentEntity;
	}

	protected void updateHierarchicalEntity(final int entityCount) {

		oldHierarchicalEntity = currentHierarchicalEntity;
		currentHierarchicalEntity = entityCount;
	}

	private boolean resetNeedFor(final int currentEntity) {

		if (getIncludeSubEntities()) {

			if (sameEntity) {

				return false;
			}
		}

		return sameEntity && oldEntity != currentEntity;
	}

	protected final boolean isSameRecord(final int currentRecord) {
		return currentRecord == oldRecord;
	}

	@Override
	public final void receive(final String name, final String value,
			final NamedValueSource source, final int recordCount,
			final int entityCount) {

		updateCounts(recordCount, entityCount);

		if (source == conditionSource) {
			conditionMet = true;
		} else {
			receive(name, value, source);
		}

		if(getIncludeSubEntities()) {

			if(isConditionMet() && isComplete()) {

				emit();
			}

			return;
		}

		if (!waitForFlush && isConditionMet() && isComplete()) {
			emit();
			if (resetAfterEmit) {
				resetCondition();
				clear();
			}
		}
	}

	protected final boolean sameEntityConstraintSatisfied(final int entityCount) {

		if (getIncludeSubEntities()) {

			return !sameEntity || oldHierarchicalEntity <= entityCount;
		}

		return !sameEntity || oldEntity == entityCount;
	}

	protected abstract void receive(final String name, final String value,
			final NamedValueSource source);

	protected abstract boolean isComplete();

	protected abstract void clear();

	protected abstract void emit();

}