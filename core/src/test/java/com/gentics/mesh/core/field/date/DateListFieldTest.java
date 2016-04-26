package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.core.field.date.DateListFieldHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.date.DateListFieldHelper.FETCH;
import static com.gentics.mesh.core.field.date.DateListFieldHelper.FILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class DateListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String DATE_LIST = "dateList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("date");
		schema.setName(DATE_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {

		Node node = folder("2015");
		prepareNode(node, "dateList", "date");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		DateGraphFieldList dateList = container.createDateList("dateList");
		dateList.createDate(1L);
		dateList.createDate(2L);

		NodeResponse response = transform(node);
		assertList(2, "dateList", "date", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList list = container.createDateList("dummyList");
		assertNotNull(list);
		DateGraphField dateField = list.createDate(1L);
		assertNotNull(dateField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());

	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList testField = container.createDateList("testField");
		testField.createDate(47L);
		testField.createDate(11L);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getDateList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

	@Test
	@Override
	public void testEquals() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList fieldA = container.createDateList("fieldA");
		DateGraphFieldList fieldB = container.createDateList("fieldB");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
		fieldA.addItem(fieldA.createDate(42L));
		assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

		assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
		assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
		fieldB.addItem(fieldB.createDate(42L));
		assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
	}

	@Test
	@Override
	public void testEqualsNull() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList fieldA = container.createDateList("fieldA");
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		Long dummyValue = 42L;

		// rest null - graph null
		DateGraphFieldList fieldA = container.createDateList(DATE_LIST);

		DateFieldListImpl restField = new DateFieldListImpl();
		assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

		// rest set - graph set - different values
		fieldA.addItem(fieldA.createDate(dummyValue));
		restField.add(dummyValue + 1L);
		assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

		// rest set - graph set - same value
		restField.getItems().clear();
		restField.add(dummyValue);
		assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

		NumberFieldListImpl otherTypeRestField = new NumberFieldListImpl();
		otherTypeRestField.add(dummyValue);
		// rest set - graph set - same value different type
		assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		invokeUpdateFromRestTestcase(DATE_LIST, FETCH, CREATE_EMPTY);

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		invokeUpdateFromRestNullOnCreateRequiredTestcase(DATE_LIST, FETCH);

	}

	@Test
	@Override
	public void testRemoveFieldViaNullValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeRemoveFieldViaNullValueTestcase(DATE_LIST, FETCH, CREATE_EMPTY, (node) -> {
			DateFieldListImpl field = null;
			updateContainer(ac, node, DATE_LIST, field);
		});
	}

	@Test
	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeDeleteRequiredFieldViaNullValueTestcase(DATE_LIST, FETCH, FILL, (container) -> {
			DateFieldListImpl field = null;
			updateContainer(ac, container, DATE_LIST, field);
		});
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeUpdateFromRestValidSimpleValueTestcase(DATE_LIST, FILL, (container) -> {
			DateFieldListImpl field = new DateFieldListImpl();
			field.getItems().add(42L);
			field.getItems().add(43L);
			updateContainer(ac, container, DATE_LIST, field);
		} , (container) -> {
			DateGraphFieldList field = container.getDateList(DATE_LIST);
			assertNotNull("The graph field {" + DATE_LIST + "} could not be found.", field);
			assertEquals("The list of the field was not updated.", 2, field.getList().size());
			assertEquals("The list item of the field was not updated.", 42L, field.getList().get(0).getDate().longValue());
			assertEquals("The list item of the field was not updated.", 43L, field.getList().get(1).getDate().longValue());
		});
	}

}