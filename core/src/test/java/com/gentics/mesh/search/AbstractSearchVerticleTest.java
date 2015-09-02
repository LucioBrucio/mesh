package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.failingLatch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
public abstract class AbstractSearchVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchVerticleTest.class);

	@Autowired
	protected SearchVerticle searchVerticle;

	@Autowired
	protected SearchProvider searchProvider;

	@After
	public void resetElasticSearch() {
		searchProvider.reset();
	}

	@BeforeClass
	@AfterClass
	public static void clean() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	protected String getSimpleQuery(String text) throws JSONException {
		QueryBuilder qb = QueryBuilders.queryStringQuery(text);
		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(qb.toString()));
		return request.toString();
	}

	protected String getSimpleTermQuery(String key, String value) {
		QueryBuilder qb = QueryBuilders.termQuery(key, value);
		String json = "{";
		json += "	 \"query\":" + qb.toString();
		json += "	}";
		return json;
	}

	protected void fullIndex() throws InterruptedException {
		try (Trx tx = db.trx()) {
			boot.meshRoot().getSearchQueue().addFullIndex();
			CountDownLatch latch = new CountDownLatch(1);
			boot.meshRoot().getSearchQueue().processAll(rh -> {
				latch.countDown();
			});
			failingLatch(latch, 30);
		}
	}

}
