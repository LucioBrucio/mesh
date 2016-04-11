package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;

import rx.Observable;

/**
 * The DateField Domain Model interface.
 * 
 * A date graph field is a basic node field which can be used to store date values.
 */
public interface DateGraphField extends ListableGraphField, BasicGraphField<DateField> {

	FieldTransformator DATE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateGraphField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return Observable.just(new DateFieldImpl());
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	/**
	 * Set the date within the field.
	 * 
	 * @param date
	 */
	void setDate(Long date);

	/**
	 * Return the date which is stored in the field.
	 * 
	 * @return
	 */
	Long getDate();

}
