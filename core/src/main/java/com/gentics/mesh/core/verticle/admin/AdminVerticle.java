package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The admin verticle provides core administration rest endpoints.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminVerticle.class);

	@Autowired
	private AdminHandler handler;

	public AdminVerticle() {
		super("admin");
	}
	
	@Override
	public String getDescription() {
		//TODO what is a admin permission?
		return "Collection of administrative endpoints which usually require admin permission";
	}

	@Override
	public void registerEndPoints() throws Exception {
		addStatusHandler();
		addMigrationStatusHandler();

		// TODO secure handlers below
//		addBackupHandler();
//		addRestoreHandler();
//		addImportHandler();
//		addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();

	}

	private void addMigrationStatusHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/migrationStatus");
		endpoint.method(GET);
		endpoint.description("Return the current schema or node migration status.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			handler.handleMigrationStatus(InternalActionContext.create(rc));
		});
	}

	private void addExportHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/export");
		endpoint.method(GET);
		endpoint.description("Invoke a graph database export");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			handler.handleExport(rc);
		});
	}

	private void addImportHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/import");
		endpoint.method(GET);
		endpoint.description("Invoke a graph database import");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			handler.handleImport(rc);
		});
	}

	private void addRestoreHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/restore");
		endpoint.description("Invoke a graph database restore");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.method(GET);
		endpoint.handler(rc -> {
			handler.handleRestore(rc);
		});
	}

	private void addBackupHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/backup");
		endpoint.method(GET);
		endpoint.description("Invoke an incremental graph database backup.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			handler.handleBackup(rc);
		});
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addStatusHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.description("Return the mesh system status.");
		endpoint.path("/status");
		endpoint.method(GET);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			handler.handleStatus(ac);
		});

	}

	// private void addServiceHandler() {
	// route("/deployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Deploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// route("/undeployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// }
	//
	// private void addVerticleHandler() {
	// route("/deployVerticle/:clazz").method(GET).handler(rc -> {
	// String clazz = rc.request().params().get("clazz");
	// try {
	// // TODO create merged jsonconfig (see mesh init)
	// JsonObject config = new JsonObject();
	// String id = deployAndWait(vertx, config, clazz);
	// rc.response().end("Deployed " + clazz + " id: " + id);
	// } catch (Exception e) {
	// rc.fail(e);
	// }
	// });
	//
	// route("/undeployVerticle/:clazz").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("clazz"));
	// });
	// }

}
