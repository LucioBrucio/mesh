package com.gentics.mesh.core.verticle.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.handler.TypeConverter;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

@SuppressWarnings("restriction")
public abstract class AbstractMigrationHandler extends AbstractHandler implements MigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationHandler.class);

	/**
	 * Script engine factory.
	 */
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

	protected Database db;

	protected SearchQueue searchQueue;

	protected BinaryFieldHandler binaryFieldHandler;

	public AbstractMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler binaryFieldHandler) {
		this.db = db;
		this.searchQueue = searchQueue;
		this.binaryFieldHandler = binaryFieldHandler;
	}

	/**
	 * Collect the migration scripts and set of touched fields when migrating the given container into the next version
	 *
	 * @param fromVersion
	 *            Container which contains the expected migration changes
	 * @param migrationScripts
	 *            List of migration scripts (will be modified)
	 * @param touchedFields
	 *            Set of touched fields (will be modified)
	 * @throws IOException
	 */
	protected void prepareMigration(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Set<String> touchedFields) throws IOException {
		SchemaChange<?> change = fromVersion.getNextChange();
		while (change != null) {
			String migrationScript = change.getMigrationScript();
			if (migrationScript != null) {
				migrationScript = migrationScript + "\nnode = JSON.stringify(migrate(JSON.parse(node), fieldname, convert));";
				migrationScripts.add(Tuple.tuple(migrationScript, change.getMigrationScriptContext()));
			}

			// if either the type changes or the field is removed, the field is
			// "touched"
			if (change instanceof FieldTypeChangeImpl) {
				touchedFields.add(((FieldTypeChangeImpl) change).getFieldName());
			} else if (change instanceof RemoveFieldChange) {
				touchedFields.add(((RemoveFieldChange) change).getFieldName());
			}

			change = change.getNextChange();
		}
	}

	/**
	 * Migrate the given container. This will also set the new version to the container.
	 * 
	 * @param ac
	 *            context
	 * @param container
	 *            container to migrate
	 * @param restModel
	 *            rest model of the container
	 * @param newVersion
	 *            new schema version
	 * @param touchedFields
	 *            set of touched fields
	 * @param migrationScripts
	 *            list of migration scripts
	 * @param clazz
	 * @throws Exception
	 */
	protected <T extends FieldContainer> void migrate(NodeMigrationActionContextImpl ac, GraphFieldContainer container, RestModel restModel,
			GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> newVersion, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Class<T> clazz) throws Exception {
		// Collect the files for all binary fields (keys are the sha512sums, values are filepaths to the binary files)
		Map<String, String> filePaths = container.getFields().stream().filter(f -> f instanceof BinaryGraphField).map(f -> (BinaryGraphField) f)
				.collect(Collectors.toMap(BinaryGraphField::getSHA512Sum, BinaryGraphField::getFilePath, (existingPath, newPath) -> existingPath));

		// Remove all touched fields (if necessary, they will be readded later)
		container.getFields().stream().filter(f -> touchedFields.contains(f.getFieldKey())).forEach(f -> f.removeField(container));

		String nodeJson = JsonUtil.toJson(restModel);

		for (Tuple<String, List<Tuple<String, Object>>> scriptEntry : migrationScripts) {
			String script = scriptEntry.v1();
			List<Tuple<String, Object>> context = scriptEntry.v2();
			ScriptEngine engine = factory.getScriptEngine(new Sandbox());

			engine.put("node", nodeJson);
			engine.put("convert", new TypeConverter());
			if (context != null) {
				for (Tuple<String, Object> ctxEntry : context) {
					engine.put(ctxEntry.v1(), ctxEntry.v2());
				}
			}
			engine.eval(script);

			Object transformedNodeModel = engine.get("node");

			if (transformedNodeModel == null) {
				throw new Exception("Transformed node model not found after handling migration scripts");
			}

			nodeJson = transformedNodeModel.toString();
		}

		// Transform the result back to the Rest Model
		T transformedRestModel = JsonUtil.readValue(nodeJson, clazz);

		container.setSchemaContainerVersion(newVersion);
		container.updateFieldsFromRest(ac, transformedRestModel.getFields());

		// Create a map containing fieldnames (as keys) and sha512sums of the supposedly stored binary contents of all binary fields
		Map<String, String> existingBinaryFields = newVersion.getSchema().getFields().stream().filter(f -> "binary".equals(f.getType()))
				.map(f -> Tuple.tuple(f.getName(), transformedRestModel.getFields().getBinaryField(f.getName()))).filter(t -> t.v2() != null)
				.filter(t -> t.v2().getSha512sum() != null).collect(Collectors.toMap(t -> t.v1(), t -> t.v2().getSha512sum()));

		// Check for every binary field in the migrated node, whether the binary file is present, if not, copy it from the old data
		existingBinaryFields.entrySet().stream().forEach(entry -> {
			String fieldName = entry.getKey();
			String sha512Sum = entry.getValue();

			BinaryGraphField binaryField = container.getBinary(fieldName);
			if (binaryField != null && !binaryField.getFile().exists() && filePaths.containsKey(sha512Sum)) {
				String pathToOldBinaryFile = filePaths.get(sha512Sum);
				File file = new File(pathToOldBinaryFile);
				if (file.exists()) {
					Buffer buffer = Mesh.vertx().fileSystem().readFileBlocking(pathToOldBinaryFile);
					binaryFieldHandler.hashAndStoreBinaryFile(buffer, binaryField.getUuid(), binaryField.getSegmentedPath());
					binaryField.setSHA512Sum(sha512Sum);
				} else {
					log.error("Could not find binary file for field {" + fieldName + "} in container {" + container.getUuid() + "}");
					throw new RuntimeException("Could not find binary file for field {" + fieldName + "} in container {" + container.getUuid()
							+ "} within path " + file.getAbsolutePath() + "} Orignal filename was {" + binaryField.getFileName() + "}");
				}
			}
		});
	}

	/**
	 * Sandbox classfilter that filters all classes
	 */
	protected static class Sandbox implements ClassFilter {
		@Override
		public boolean exposeToScripts(String className) {
			return false;
		}
	}

}
