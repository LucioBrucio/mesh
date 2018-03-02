package com.gentics.mesh.graphql.filter;

import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphqlfilter.filter.FilterField;
import com.gentics.mesh.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.graphqlfilter.filter.StringFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NodeFilter extends StartMainFilter<NodeContent> {

    private static final String NAME = "NodeFilter";

    public static NodeFilter filter(GraphQLContext context) {
        return context.getOrStore(NAME, () -> new NodeFilter(context));
    }

    private final GraphQLContext context;

    private NodeFilter(GraphQLContext context) {
        super(NAME, "Filters Nodes");
        this.context = context;
    }

    @Override
    protected List<FilterField<NodeContent, ?>> getFilters() {
        List<FilterField<NodeContent, ?>> filters = new ArrayList<>();
        filters.add(new MappedFilter<>("uuid", "Filters by uuid", StringFilter.filter(), content -> content.getNode().getUuid()));
        filters.add(new MappedFilter<>("schema", "Filters by schema", SchemaFilter.filter(context), content -> content.getNode().getSchemaContainer()));
        filters.addAll(createAllFieldFilters());

        return filters;
    }

    private List<FilterField<NodeContent, ?>> createAllFieldFilters() {
        return StreamSupport.stream(context.getProject().getSchemaContainerRoot().findAllIt().spliterator(), false)
            .map(this::createFieldFilter)
            .collect(Collectors.toList());
    }

    private FilterField<NodeContent, ?> createFieldFilter(SchemaContainer schema) {
        return new MappedFilter<>("fields_" + schema.getName(), "Filters by fields of the " + schema.getName() + " schema",
                FieldFilter.filter(context, schema.getLatestVersion().getSchema()),
                NodeContent::getContainer
            );
    }
}
