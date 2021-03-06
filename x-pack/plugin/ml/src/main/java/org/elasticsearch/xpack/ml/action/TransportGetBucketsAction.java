/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.ml.action.GetBucketsAction;
import org.elasticsearch.xpack.ml.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.persistence.JobResultsProvider;

public class TransportGetBucketsAction extends HandledTransportAction<GetBucketsAction.Request, GetBucketsAction.Response> {

    private final JobResultsProvider jobResultsProvider;
    private final JobManager jobManager;
    private final Client client;

    @Inject
    public TransportGetBucketsAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                     ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                     JobResultsProvider jobResultsProvider, JobManager jobManager, Client client) {
        super(settings, GetBucketsAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                GetBucketsAction.Request::new);
        this.jobResultsProvider = jobResultsProvider;
        this.jobManager = jobManager;
        this.client = client;
    }

    @Override
    protected void doExecute(GetBucketsAction.Request request, ActionListener<GetBucketsAction.Response> listener) {
        jobManager.getJobOrThrowIfUnknown(request.getJobId());

        BucketsQueryBuilder query =
                new BucketsQueryBuilder().expand(request.isExpand())
                        .includeInterim(request.isExcludeInterim() == false)
                        .start(request.getStart())
                        .end(request.getEnd())
                        .anomalyScoreThreshold(request.getAnomalyScore())
                        .sortField(request.getSort())
                        .sortDescending(request.isDescending());

        if (request.getPageParams() != null) {
            query.from(request.getPageParams().getFrom())
                    .size(request.getPageParams().getSize());
        }
        if (request.getTimestamp() != null) {
            query.timestamp(request.getTimestamp());
        } else {
            query.start(request.getStart());
            query.end(request.getEnd());
        }
        jobResultsProvider.buckets(request.getJobId(), query, q ->
                listener.onResponse(new GetBucketsAction.Response(q)), listener::onFailure, client);
    }
}
