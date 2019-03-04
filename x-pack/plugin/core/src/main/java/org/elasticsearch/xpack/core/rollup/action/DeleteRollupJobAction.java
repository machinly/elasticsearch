/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.rollup.action;


import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.TaskOperationFailure;
import org.elasticsearch.action.support.tasks.BaseTasksRequest;
import org.elasticsearch.action.support.tasks.BaseTasksResponse;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContentFragment;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.core.rollup.RollupField;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DeleteRollupJobAction extends Action<DeleteRollupJobAction.Request, DeleteRollupJobAction.Response,
    DeleteRollupJobAction.RequestBuilder> {

    public static final DeleteRollupJobAction INSTANCE = new DeleteRollupJobAction();
    public static final String NAME = "cluster:admin/xpack/rollup/delete";

    private DeleteRollupJobAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, INSTANCE);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends BaseTasksRequest<Request> implements ToXContentFragment {
        private String id;

        public Request(String id) {
            this.id = ExceptionsHelper.requireNonNull(id, RollupField.ID.getPreferredName());
        }

        public Request() {}

        @Override
        public boolean match(Task task) {
            return task.getDescription().equals(RollupField.NAME + "_" + id);
        }

        public String getId() {
            return id;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            id = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(id);
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field(RollupField.ID.getPreferredName(), id);
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(id, other.id);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<DeleteRollupJobAction.Request,
        DeleteRollupJobAction.Response, RequestBuilder> {
        protected RequestBuilder(ElasticsearchClient client, DeleteRollupJobAction action) {
            super(client, action, new DeleteRollupJobAction.Request());
        }
    }

    public static class Response extends BaseTasksResponse implements Writeable, ToXContentObject {

        private boolean acknowledged;

        public Response(StreamInput in) throws IOException {
            super(Collections.emptyList(), Collections.emptyList());
            readFrom(in);
        }

        public Response(boolean acknowledged, List<TaskOperationFailure> taskFailures, List<FailedNodeException> nodeFailures) {
            super(taskFailures, nodeFailures);
            this.acknowledged = acknowledged;
        }

        public Response(boolean acknowledged) {
            super(Collections.emptyList(), Collections.emptyList());
            this.acknowledged = acknowledged;
        }

        public Response() {
            super(Collections.emptyList(), Collections.emptyList());
            this.acknowledged = false;
        }

        public boolean isDeleted() {
            return acknowledged;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            acknowledged = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBoolean(acknowledged);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            {
                toXContentCommon(builder, params);
                builder.field("acknowledged", acknowledged);
            }
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DeleteRollupJobAction.Response response = (DeleteRollupJobAction.Response) o;
            return super.equals(o) && acknowledged == response.acknowledged;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), acknowledged);
        }
    }
}
